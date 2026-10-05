package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayer
import com.example.audio.SpeechInputManager
import com.example.data.AssistantResponse
import com.example.data.ChatMessage
import com.example.data.GeminiRepository
import com.example.data.MessageSender
import com.example.device.AndroidActionBridge
import com.example.device.DeviceActionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AssistantVisualState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    EXECUTING_ACTION
}

data class ContactClarificationState(
    val isOpen: Boolean = false,
    val queryName: String = "",
    val question: String = "",
    val contacts: List<String> = emptyList()
)

data class ActionBannerState(
    val isVisible: Boolean = false,
    val actionName: String = "",
    val details: String = "",
    val success: Boolean = true
)

data class ArushiUiState(
    val visualState: AssistantVisualState = AssistantVisualState.IDLE,
    val detectedLanguage: String = "Auto (Hindi / English / Hinglish)",
    val messages: List<ChatMessage> = emptyList(),
    val partialSpeech: String = "",
    val currentAudioAmplitude: Float = 0f,
    val isMicrophoneActive: Boolean = false,
    val isAudioPlaying: Boolean = false,
    val clarificationState: ContactClarificationState = ContactClarificationState(),
    val actionBanner: ActionBannerState = ActionBannerState(),
    val showBridgeConsole: Boolean = false,
    val hasApiKey: Boolean = false,
    val lastRecognizedText: String = ""
)

class ArushiViewModel(application: Application) : AndroidViewModel(application) {

    val actionManager = DeviceActionManager(application.applicationContext)
    val audioPlayer = AudioPlayer(application.applicationContext)
    val speechInputManager = SpeechInputManager(application.applicationContext)
    val geminiRepository = GeminiRepository(application.applicationContext, actionManager)

    val actionBridge = AndroidActionBridge(actionManager) { action, msg, success ->
        showActionBanner(action, msg, success)
        addMessage(
            ChatMessage(
                sender = MessageSender.SYSTEM_ACTION,
                text = "Bridge Executed: $action -> $msg",
                actionName = action,
                actionDetails = msg,
                actionSuccess = success
            )
        )
    }

    private val _uiState = MutableStateFlow(
        ArushiUiState(
            hasApiKey = geminiRepository.hasApiKey,
            messages = listOf(
                ChatMessage(
                    sender = MessageSender.ARUSHI,
                    text = "Namaste! I am Arushi, your AI voice assistant. Speak to me in English, Hindi, Hinglish, or your preferred language. You can ask me to open WhatsApp, call contacts, open apps, or just talk!"
                )
            )
        )
    )
    val uiState: StateFlow<ArushiUiState> = _uiState.asStateFlow()

    init {
        // Collect audio playback status & amplitude for orb animation
        viewModelScope.launch {
            audioPlayer.isPlaying.collect { playing ->
                _uiState.update { current ->
                    current.copy(
                        isAudioPlaying = playing,
                        visualState = when {
                            playing -> AssistantVisualState.SPEAKING
                            current.isMicrophoneActive -> AssistantVisualState.LISTENING
                            current.visualState == AssistantVisualState.THINKING -> AssistantVisualState.THINKING
                            else -> AssistantVisualState.IDLE
                        }
                    )
                }
            }
        }

        viewModelScope.launch {
            audioPlayer.currentAmplitude.collect { amp ->
                _uiState.update { it.copy(currentAudioAmplitude = amp) }
            }
        }

        // Collect mic input status
        viewModelScope.launch {
            speechInputManager.isListening.collect { listening ->
                _uiState.update { current ->
                    current.copy(
                        isMicrophoneActive = listening,
                        visualState = if (listening) AssistantVisualState.LISTENING else {
                            if (current.isAudioPlaying) AssistantVisualState.SPEAKING else current.visualState
                        }
                    )
                }
            }
        }

        viewModelScope.launch {
            speechInputManager.rmsDb.collect { rms ->
                if (_uiState.value.isMicrophoneActive) {
                    _uiState.update { it.copy(currentAudioAmplitude = rms) }
                }
            }
        }

        viewModelScope.launch {
            speechInputManager.partialText.collect { partial ->
                _uiState.update { it.copy(partialSpeech = partial) }
            }
        }

        // Listen for speech recognition results
        speechInputManager.onSpeechResult = { text ->
            onUserSpoke(text)
        }

        speechInputManager.onSpeechError = { errorMsg ->
            showActionBanner("Microphone", errorMsg, false)
        }
    }

    /**
     * Test Case 10: Interruption handling.
     * When user speaks or taps interrupt, immediately cancel audio output.
     */
    fun interruptArushi() {
        audioPlayer.stop()
        _uiState.update {
            it.copy(
                isAudioPlaying = false,
                visualState = if (it.isMicrophoneActive) AssistantVisualState.LISTENING else AssistantVisualState.IDLE
            )
        }
    }

    fun toggleMicrophone(hasRecordAudioPermission: Boolean) {
        if (!hasRecordAudioPermission) return

        if (_uiState.value.isMicrophoneActive) {
            speechInputManager.stopListening()
        } else {
            // Stop any ongoing assistant speech first (interruption)
            interruptArushi()
            _uiState.update { it.copy(visualState = AssistantVisualState.LISTENING, partialSpeech = "") }
            speechInputManager.startListening()
        }
    }

    fun onUserSpoke(userText: String) {
        if (userText.isBlank()) return

        // Stop active speech playback if any
        interruptArushi()

        // Detect language switch
        updateLanguageIndicator(userText)

        // Add user message to conversation
        addMessage(
            ChatMessage(
                sender = MessageSender.USER,
                text = userText
            )
        )

        _uiState.update {
            it.copy(
                visualState = AssistantVisualState.THINKING,
                lastRecognizedText = userText,
                partialSpeech = ""
            )
        }

        // Process with Gemini Live
        viewModelScope.launch {
            val response = geminiRepository.processUserTurn(userText)
            handleAssistantResponse(response)
        }
    }

    private fun handleAssistantResponse(response: AssistantResponse) {
        when (response) {
            is AssistantResponse.VoiceText -> {
                addMessage(
                    ChatMessage(
                        sender = MessageSender.ARUSHI,
                        text = response.text,
                        audioBase64 = response.audioBase64,
                        audioMimeType = response.audioMimeType
                    )
                )

                _uiState.update {
                    it.copy(
                        visualState = AssistantVisualState.SPEAKING,
                        detectedLanguage = if (response.detectedLanguage != "auto") response.detectedLanguage else it.detectedLanguage
                    )
                }

                playResponseVoice(response.text, response.audioBase64, response.audioMimeType)
            }

            is AssistantResponse.ActionExecuted -> {
                showActionBanner(response.actionName, response.details, response.success)

                addMessage(
                    ChatMessage(
                        sender = MessageSender.SYSTEM_ACTION,
                        text = "Executed ${response.actionName}: ${response.details}",
                        actionName = response.actionName,
                        actionDetails = response.details,
                        actionSuccess = response.success
                    )
                )

                val followUp = response.followUpResponse
                if (followUp != null) {
                    addMessage(
                        ChatMessage(
                            sender = MessageSender.ARUSHI,
                            text = followUp.text,
                            audioBase64 = followUp.audioBase64,
                            audioMimeType = followUp.audioMimeType
                        )
                    )
                    _uiState.update { it.copy(visualState = AssistantVisualState.SPEAKING) }
                    playResponseVoice(followUp.text, followUp.audioBase64, followUp.audioMimeType)
                } else {
                    _uiState.update { it.copy(visualState = AssistantVisualState.IDLE) }
                }
            }

            is AssistantResponse.MultipleContactsPrompt -> {
                _uiState.update {
                    it.copy(
                        visualState = AssistantVisualState.SPEAKING,
                        clarificationState = ContactClarificationState(
                            isOpen = true,
                            queryName = response.nameQuery,
                            question = response.questionText,
                            contacts = response.contacts
                        )
                    )
                }

                addMessage(
                    ChatMessage(
                        sender = MessageSender.ARUSHI,
                        text = response.questionText
                    )
                )

                playResponseVoice(response.questionText, null, null)
            }

            is AssistantResponse.Error -> {
                _uiState.update { it.copy(visualState = AssistantVisualState.IDLE) }
                addMessage(
                    ChatMessage(
                        sender = MessageSender.ARUSHI,
                        text = "Maaf kijiye, main process nahi kar payi: ${response.message}"
                    )
                )
            }
        }
    }

    private fun playResponseVoice(text: String, audioBase64: String?, audioMime: String?) {
        if (!audioBase64.isNullOrBlank()) {
            audioPlayer.playGeminiAudio(audioBase64, audioMime ?: "audio/pcm;rate=24000") {
                _uiState.update { it.copy(visualState = AssistantVisualState.IDLE) }
            }
        } else {
            // Audible voice output via Native TTS fallback
            val langCode = when (_uiState.value.detectedLanguage.lowercase()) {
                "hindi" -> "hi-IN"
                "marathi" -> "mr-IN"
                "gujarati" -> "gu-IN"
                "bengali" -> "bn-IN"
                "tamil" -> "ta-IN"
                "telugu" -> "te-IN"
                "kannada" -> "kn-IN"
                "malayalam" -> "ml-IN"
                "punjabi" -> "pa-IN"
                "urdu" -> "ur-IN"
                else -> "en-IN"
            }
            audioPlayer.speakText(text, langCode) {
                _uiState.update { it.copy(visualState = AssistantVisualState.IDLE) }
            }
        }
    }

    fun selectClarificationContact(contactString: String) {
        _uiState.update { it.copy(clarificationState = it.clarificationState.copy(isOpen = false)) }
        val number = contactString.substringAfter("(").substringBefore(")").trim()
        val name = contactString.substringBefore("(").trim()
        onUserSpoke("Call $name at $number")
    }

    fun dismissClarification() {
        _uiState.update { it.copy(clarificationState = it.clarificationState.copy(isOpen = false)) }
    }

    private fun updateLanguageIndicator(text: String) {
        val lower = text.lowercase()
        val detected = when {
            lower.contains("hindi") || lower.contains("हिंदी") -> "Hindi"
            lower.contains("hinglish") -> "Hinglish"
            lower.contains("english") -> "English"
            lower.contains("marathi") -> "Marathi"
            lower.contains("gujarati") -> "Gujarati"
            lower.contains("bengali") || lower.contains("bangla") -> "Bengali"
            lower.contains("tamil") -> "Tamil"
            lower.contains("telugu") -> "Telugu"
            lower.contains("kannada") -> "Kannada"
            lower.contains("malayalam") -> "Malayalam"
            lower.contains("punjabi") -> "Punjabi"
            lower.contains("urdu") -> "Urdu"
            lower.contains("kholo") || lower.contains("karo") || lower.contains("lagao") || lower.contains("namaste") -> "Hinglish"
            else -> _uiState.value.detectedLanguage
        }
        _uiState.update { it.copy(detectedLanguage = detected) }
    }

    private fun addMessage(message: ChatMessage) {
        _uiState.update { it.copy(messages = it.messages + message) }
    }

    fun showActionBanner(action: String, details: String, success: Boolean) {
        _uiState.update {
            it.copy(
                actionBanner = ActionBannerState(
                    isVisible = true,
                    actionName = action,
                    details = details,
                    success = success
                )
            )
        }
    }

    fun dismissActionBanner() {
        _uiState.update { it.copy(actionBanner = it.actionBanner.copy(isVisible = false)) }
    }

    fun setBridgeConsoleVisible(visible: Boolean) {
        _uiState.update { it.copy(showBridgeConsole = visible) }
    }

    fun clearHistory() {
        geminiRepository.clearHistory()
        _uiState.update {
            it.copy(
                messages = listOf(
                    ChatMessage(
                        sender = MessageSender.ARUSHI,
                        text = "History cleared! How can I help you next?"
                    )
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        speechInputManager.stopListening()
    }
}
