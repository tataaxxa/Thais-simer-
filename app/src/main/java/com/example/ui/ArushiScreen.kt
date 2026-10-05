package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.ChatMessage
import com.example.data.MessageSender
import com.example.ui.theme.Amber400
import com.example.ui.theme.Cyan300
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Cyan900
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Rose500
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.Violet400
import com.example.ui.theme.Violet500
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArushiScreen(
    viewModel: ArushiViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var typedText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Permissions handling
    var hasRecordAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasRecordAudioPermission = perms[Manifest.permission.RECORD_AUDIO] == true
        hasContactsPermission = perms[Manifest.permission.READ_CONTACTS] == true
    }

    LaunchedEffect(Unit) {
        if (!hasRecordAudioPermission || !hasContactsPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.CALL_PHONE
                )
            )
        }
    }

    // Auto-scroll conversation to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950),
        color = Slate950
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top App Bar
            TopBar(
                languageName = uiState.detectedLanguage,
                isLiveActive = uiState.visualState != AssistantVisualState.IDLE,
                onOpenBridge = { viewModel.setBridgeConsoleVisible(true) },
                onClearHistory = { viewModel.clearHistory() }
            )

            // Action Feedback Banner
            AnimatedVisibility(
                visible = uiState.actionBanner.isVisible,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                ActionBanner(
                    banner = uiState.actionBanner,
                    onDismiss = { viewModel.dismissActionBanner() }
                )
            }

            // Contact Clarification Dialog (Test Case 8)
            if (uiState.clarificationState.isOpen) {
                ContactClarificationDialog(
                    state = uiState.clarificationState,
                    onSelectContact = { viewModel.selectClarificationContact(it) },
                    onDismiss = { viewModel.dismissClarification() }
                )
            }

            // Central Glowing Orb Voice Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ArushiVoiceOrb(
                        visualState = uiState.visualState,
                        amplitude = uiState.currentAudioAmplitude,
                        onClick = {
                            if (uiState.isAudioPlaying) {
                                // Test Case 10: Interruption
                                viewModel.interruptArushi()
                            } else {
                                viewModel.toggleMicrophone(hasRecordAudioPermission)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status Indicator Text
                    Text(
                        text = when (uiState.visualState) {
                            AssistantVisualState.IDLE -> "Arushi is ready • Tap to speak"
                            AssistantVisualState.LISTENING -> "Listening... • Speak now"
                            AssistantVisualState.THINKING -> "Thinking & processing..."
                            AssistantVisualState.SPEAKING -> "Arushi is speaking • Tap orb to interrupt"
                            AssistantVisualState.EXECUTING_ACTION -> "Executing device action..."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (uiState.visualState) {
                            AssistantVisualState.LISTENING -> Amber400
                            AssistantVisualState.SPEAKING -> Cyan300
                            AssistantVisualState.THINKING -> Violet400
                            else -> Slate400
                        },
                        fontWeight = FontWeight.Medium
                    )

                    // Partial real-time speech text preview
                    if (uiState.partialSpeech.isNotBlank()) {
                        Text(
                            text = "\"${uiState.partialSpeech}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate200,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Test Cases Quick Actions Carousel (Supports 1-touch testing of all 10 prompt test cases)
            TestCasesCarousel(
                onExecuteTest = { command ->
                    viewModel.onUserSpoke(command)
                },
                onInterrupt = {
                    viewModel.interruptArushi()
                }
            )

            // Conversation Messages Feed
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(uiState.messages, key = { it.id }) { msg ->
                    MessageCard(
                        message = msg,
                        onReplayAudio = {
                            viewModel.audioPlayer.playGeminiAudio(
                                msg.audioBase64 ?: "",
                                msg.audioMimeType ?: "audio/pcm;rate=24000"
                            )
                        }
                    )
                }
            }

            // Bottom Input Controls Bar
            BottomInputBar(
                typedText = typedText,
                onTextChange = { typedText = it },
                onSend = {
                    if (typedText.isNotBlank()) {
                        viewModel.onUserSpoke(typedText)
                        typedText = ""
                    }
                },
                isListening = uiState.isMicrophoneActive,
                isPlaying = uiState.isAudioPlaying,
                onMicClick = {
                    if (!hasRecordAudioPermission) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.READ_CONTACTS,
                                Manifest.permission.CALL_PHONE
                            )
                        )
                    } else {
                        viewModel.toggleMicrophone(true)
                    }
                },
                onStopAudio = {
                    viewModel.interruptArushi()
                }
            )
        }

        // Bridge Console Sheet Modal
        if (uiState.showBridgeConsole) {
            BridgeConsoleSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.setBridgeConsoleVisible(false) }
            )
        }
    }
}

@Composable
private fun TopBar(
    languageName: String,
    isLiveActive: Boolean,
    onOpenBridge: () -> Unit,
    onClearHistory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isLiveActive) Cyan400 else Slate400)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Arushi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Cyan900.copy(alpha = 0.6f),
                modifier = Modifier.testTag("language_badge")
            ) {
                Text(
                    text = languageName,
                    color = Cyan300,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onOpenBridge,
                modifier = Modifier.testTag("bridge_inspector_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "Action Bridge Inspector",
                    tint = Cyan400
                )
            }

            IconButton(
                onClick = onClearHistory,
                modifier = Modifier.testTag("clear_history_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear History",
                    tint = Slate400
                )
            }
        }
    }
}

/**
 * Animated Radiant Voice Orb for Arushi:
 * Visualizes states (IDLE, LISTENING, THINKING, SPEAKING).
 */
@Composable
private fun ArushiVoiceOrb(
    visualState: AssistantVisualState,
    amplitude: Float,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val animatedAmp by animateFloatAsState(
        targetValue = amplitude.coerceIn(0f, 1f),
        animationSpec = tween(100),
        label = "amp"
    )

    Box(
        modifier = Modifier
            .size(150.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag("voice_orb"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width / 2, size.height / 2)
            val baseRadius = (size.minDimension / 2) * 0.7f

            when (visualState) {
                AssistantVisualState.IDLE -> {
                    // Gentle breathing dual-glow gradient
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Cyan500.copy(alpha = 0.6f), Violet500.copy(alpha = 0.3f), Color.Transparent),
                            center = centerOffset,
                            radius = baseRadius * pulseScale * 1.3f
                        ),
                        radius = baseRadius * pulseScale * 1.3f,
                        center = centerOffset
                    )

                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(Cyan400, Violet400)
                        ),
                        radius = baseRadius * 0.75f,
                        center = centerOffset
                    )
                }

                AssistantVisualState.LISTENING -> {
                    // Radiant sound wave rings expanding outwards with mic level
                    val ringRadius = baseRadius * (1f + animatedAmp * 0.8f)
                    drawCircle(
                        color = Amber400.copy(alpha = 0.25f),
                        radius = ringRadius * 1.3f,
                        center = centerOffset
                    )
                    drawCircle(
                        color = Amber400.copy(alpha = 0.4f),
                        radius = ringRadius,
                        center = centerOffset
                    )
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(Amber400, Cyan400)
                        ),
                        radius = baseRadius * 0.8f,
                        center = centerOffset
                    )
                }

                AssistantVisualState.THINKING -> {
                    // Rotating multi-color orbit sweep
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Violet500.copy(alpha = 0.5f), Color.Transparent),
                            radius = baseRadius * 1.4f
                        ),
                        radius = baseRadius * 1.4f,
                        center = centerOffset
                    )

                    val sweepAngle = rotationAngle
                    val sweepRad = Math.toRadians(sweepAngle.toDouble())
                    val orbitX = centerOffset.x + (baseRadius * 0.9f * cos(sweepRad)).toFloat()
                    val orbitY = centerOffset.y + (baseRadius * 0.9f * sin(sweepRad)).toFloat()

                    drawCircle(
                        color = Cyan300,
                        radius = 8.dp.toPx(),
                        center = Offset(orbitX, orbitY)
                    )

                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(Violet400, Cyan400)
                        ),
                        radius = baseRadius * 0.75f,
                        center = centerOffset
                    )
                }

                AssistantVisualState.SPEAKING -> {
                    // Pulsating voice bars reacting to real audio amplitude
                    val speakRadius = baseRadius * (0.85f + animatedAmp * 0.5f)

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Cyan400.copy(alpha = 0.7f), Violet400.copy(alpha = 0.3f), Color.Transparent),
                            radius = speakRadius * 1.5f
                        ),
                        radius = speakRadius * 1.5f,
                        center = centerOffset
                    )

                    // Draw radial audio wave rays
                    val numRays = 16
                    for (i in 0 until numRays) {
                        val angle = (i * 360f / numRays) + rotationAngle * 0.2f
                        val rad = Math.toRadians(angle.toDouble())
                        val rayLen = (12.dp.toPx() + (animatedAmp * 28.dp.toPx()) * ((i % 3) + 1) / 3f)
                        val startX = centerOffset.x + ((speakRadius * 0.85f) * cos(rad)).toFloat()
                        val startY = centerOffset.y + ((speakRadius * 0.85f) * sin(rad)).toFloat()
                        val endX = centerOffset.x + ((speakRadius * 0.85f + rayLen) * cos(rad)).toFloat()
                        val endY = centerOffset.y + ((speakRadius * 0.85f + rayLen) * sin(rad)).toFloat()

                        drawLine(
                            brush = Brush.linearGradient(listOf(Cyan300, Amber400)),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    drawCircle(
                        brush = Brush.linearGradient(listOf(Cyan400, Violet500)),
                        radius = baseRadius * 0.75f,
                        center = centerOffset
                    )
                }

                AssistantVisualState.EXECUTING_ACTION -> {
                    drawCircle(
                        color = Emerald500.copy(alpha = 0.4f),
                        radius = baseRadius * 1.2f,
                        center = centerOffset
                    )
                    drawCircle(
                        color = Emerald500,
                        radius = baseRadius * 0.8f,
                        center = centerOffset
                    )
                }
            }

            // Outer decorative ring
            drawCircle(
                color = Cyan400.copy(alpha = 0.3f),
                radius = baseRadius * 1.1f,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

/**
 * 1-Touch test cases chips for verifying all 10 specifications easily.
 */
@Composable
private fun TestCasesCarousel(
    onExecuteTest: (String) -> Unit,
    onInterrupt: () -> Unit
) {
    val scrollState = rememberScrollState()

    val testItems = listOf(
        "Hello Arushi",
        "Hindi mein baat karo",
        "Talk to me in English",
        "Hinglish mein baat karo",
        "WhatsApp kholo",
        "Open WhatsApp",
        "Mummy ko call karo",
        "Call Rahul",
        "Call 9876543210",
        "Open YouTube",
        "Open settings",
        "Open Instagram",
        "Open Chrome"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Voice Test Commands",
                style = MaterialTheme.typography.labelSmall,
                color = Slate400,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Tap to speak",
                style = MaterialTheme.typography.labelSmall,
                color = Cyan400
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Interruption action chip
            AssistChip(
                onClick = onInterrupt,
                label = { Text("Stop / Interrupt", color = Rose500, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = Rose500,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(containerColor = Slate900),
                border = androidx.compose.foundation.BorderStroke(1.dp, Rose500.copy(alpha = 0.5f)),
                modifier = Modifier.testTag("test_chip_interrupt")
            )

            testItems.forEach { testCommand ->
                AssistChip(
                    onClick = { onExecuteTest(testCommand) },
                    label = { Text(testCommand, color = Slate200, fontSize = 12.sp) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Slate900),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                    modifier = Modifier.testTag("test_chip_${testCommand.replace(" ", "_").lowercase()}")
                )
            }
        }
    }
}

@Composable
private fun ActionBanner(
    banner: ActionBannerState,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("action_feedback_banner"),
        colors = CardDefaults.cardColors(
            containerColor = if (banner.success) Slate800 else Slate900
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (banner.success) Emerald500.copy(alpha = 0.6f) else Rose500.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (banner.success) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (banner.success) Emerald500 else Rose500,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Action: ${banner.actionName}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = banner.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate200
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Slate400,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageCard(
    message: ChatMessage,
    onReplayAudio: () -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    val isSystem = message.sender == MessageSender.SYSTEM_ACTION

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = when {
            isUser -> Arrangement.End
            isSystem -> Arrangement.Center
            else -> Arrangement.Start
        }
    ) {
        if (isSystem) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Emerald500,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate200,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) Cyan900.copy(alpha = 0.8f) else Slate800
                ),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isUser) Cyan400.copy(alpha = 0.5f) else Slate700
                ),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isUser) "You" else "Arushi",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) Cyan300 else Amber400
                        )

                        if (!isUser && message.audioBase64 != null) {
                            IconButton(
                                onClick = onReplayAudio,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Replay Gemini Voice",
                                    tint = Cyan300,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Dialog for Test Case 8 ("multiple matches clarification"):
 * Example: "I found two Rahuls. Which one should I call?"
 */
@Composable
private fun ContactClarificationDialog(
    state: ContactClarificationState,
    onSelectContact: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Call, contentDescription = null, tint = Amber400)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Contact to Call", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = state.question,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                state.contacts.forEach { contactStr ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelectContact(contactStr) }
                            .testTag("contact_item_${contactStr.take(10)}"),
                        shape = RoundedCornerShape(8.dp),
                        color = Slate800
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = Cyan400)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = contactStr,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate400)
            }
        },
        containerColor = Slate900,
        textContentColor = Color.White,
        titleContentColor = Color.White
    )
}

@Composable
private fun BottomInputBar(
    typedText: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isListening: Boolean,
    isPlaying: Boolean,
    onMicClick: () -> Unit,
    onStopAudio: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Slate900,
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mic / Stop FAB
            FloatingActionButton(
                onClick = {
                    if (isPlaying) {
                        onStopAudio()
                    } else {
                        onMicClick()
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("mic_fab_button"),
                containerColor = when {
                    isPlaying -> Rose500
                    isListening -> Amber400
                    else -> Cyan500
                },
                contentColor = Slate950,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(
                    imageVector = when {
                        isPlaying -> Icons.Default.Stop
                        isListening -> Icons.Default.MicOff
                        else -> Icons.Default.Mic
                    },
                    contentDescription = if (isPlaying) "Stop Voice" else "Microphone"
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Text Input Field
            OutlinedTextField(
                value = typedText,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_text_input"),
                placeholder = {
                    Text(
                        text = if (isListening) "Listening..." else "Say or type: 'WhatsApp kholo'...",
                        color = Slate400,
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Cyan400,
                    unfocusedBorderColor = Slate700,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Slate800,
                    unfocusedContainerColor = Slate800
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() })
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                enabled = typedText.isNotBlank(),
                modifier = Modifier.testTag("send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (typedText.isNotBlank()) Cyan400 else Slate700
                )
            }
        }
    }
}
