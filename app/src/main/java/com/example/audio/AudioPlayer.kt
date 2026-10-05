package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class AudioPlayer(private val context: Context) : TextToSpeech.OnInitListener {

    private val tag = "AudioPlayer"

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize TTS fallback", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isPlaying.value = true
                }
                override fun onDone(utteranceId: String?) {
                    _isPlaying.value = false
                    _currentAmplitude.value = 0f
                }
                override fun onError(utteranceId: String?) {
                    _isPlaying.value = false
                    _currentAmplitude.value = 0f
                }
            })
        }
    }

    /**
     * Immediately stops any currently playing audio (interruption handling).
     */
    fun stop() {
        playbackJob?.cancel()
        playbackJob = null

        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.w(tag, "Error stopping AudioTrack", e)
        }
        audioTrack = null

        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.w(tag, "Error stopping MediaPlayer", e)
        }
        mediaPlayer = null

        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.w(tag, "Error stopping TTS", e)
        }

        _isPlaying.value = false
        _currentAmplitude.value = 0f
    }

    /**
     * Plays Gemini Live audio output (PCM or encoded WAV/MP3).
     */
    fun playGeminiAudio(base64Audio: String, mimeType: String, onFinished: (() -> Unit)? = null) {
        stop()

        playbackJob = scope.launch {
            try {
                val rawBytes = Base64.decode(base64Audio, Base64.DEFAULT)
                if (rawBytes == null || rawBytes.isEmpty()) {
                    onFinished?.invoke()
                    return@launch
                }

                _isPlaying.value = true

                if (mimeType.contains("pcm") || mimeType.contains("raw")) {
                    playRawPcm(rawBytes, sampleRate = 24000, onFinished)
                } else {
                    playEncodedAudioBytes(rawBytes, mimeType, onFinished)
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to play Gemini audio", e)
                _isPlaying.value = false
                _currentAmplitude.value = 0f
                onFinished?.invoke()
            }
        }
    }

    private fun playRawPcm(pcmBytes: ByteArray, sampleRate: Int = 24000, onFinished: (() -> Unit)?) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val bufferSize = maxOf(minBufferSize, pcmBytes.size)

        val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
        }

        audioTrack = track
        track.play()

        val chunkSize = 2048
        var offset = 0
        while (offset < pcmBytes.size && _isPlaying.value) {
            val length = minOf(chunkSize, pcmBytes.size - offset)
            track.write(pcmBytes, offset, length)

            // Approximate amplitude for visualizer
            var sum = 0L
            for (i in offset until (offset + length - 1) step 2) {
                val sample = ((pcmBytes[i + 1].toInt() shl 8) or (pcmBytes[i].toInt() and 0xFF)).toShort()
                sum += kotlin.math.abs(sample.toLong())
            }
            val avg = sum / (length / 2).coerceAtLeast(1)
            _currentAmplitude.value = (avg / 32768f).coerceIn(0.1f, 1f)

            offset += length
        }

        track.stop()
        track.release()
        audioTrack = null
        _isPlaying.value = false
        _currentAmplitude.value = 0f
        onFinished?.invoke()
    }

    private fun playEncodedAudioBytes(bytes: ByteArray, mimeType: String, onFinished: (() -> Unit)?) {
        try {
            val ext = if (mimeType.contains("wav")) ".wav" else ".mp3"
            val tempFile = File.createTempFile("gemini_voice_", ext, context.cacheDir)
            FileOutputStream(tempFile).use { it.write(bytes) }

            val player = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentAmplitude.value = 0f
                    tempFile.delete()
                    onFinished?.invoke()
                }
                setOnErrorListener { _, _, _ ->
                    _isPlaying.value = false
                    _currentAmplitude.value = 0f
                    tempFile.delete()
                    onFinished?.invoke()
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(tag, "Failed to play encoded audio file", e)
            _isPlaying.value = false
            _currentAmplitude.value = 0f
            onFinished?.invoke()
        }
    }

    /**
     * Speaks text using native TTS (for demo mode / when Gemini audio is not returned).
     */
    fun speakText(text: String, languageCode: String = "hi-IN", onFinished: (() -> Unit)? = null) {
        stop()
        if (!ttsReady || tts == null) {
            onFinished?.invoke()
            return
        }

        try {
            val locale = when (languageCode.lowercase()) {
                "hi", "hin", "hindi" -> Locale.forLanguageTag("hi-IN")
                "mr", "marathi" -> Locale.forLanguageTag("mr-IN")
                "gu", "gujarati" -> Locale.forLanguageTag("gu-IN")
                "bn", "bengali" -> Locale.forLanguageTag("bn-IN")
                "ta", "tamil" -> Locale.forLanguageTag("ta-IN")
                "te", "telugu" -> Locale.forLanguageTag("te-IN")
                "kn", "kannada" -> Locale.forLanguageTag("kn-IN")
                "ml", "malayalam" -> Locale.forLanguageTag("ml-IN")
                "pa", "punjabi" -> Locale.forLanguageTag("pa-IN")
                "ur", "urdu" -> Locale.forLanguageTag("ur-IN")
                else -> Locale.ENGLISH
            }
            tts?.language = locale
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "arushi_utterance_${System.currentTimeMillis()}")
            _isPlaying.value = true
        } catch (e: Exception) {
            Log.e(tag, "TTS speak error", e)
            _isPlaying.value = false
            onFinished?.invoke()
        }
    }

    fun release() {
        stop()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
