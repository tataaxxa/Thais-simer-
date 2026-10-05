package com.example.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.Violet400

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BridgeConsoleSheet(
    viewModel: ArushiViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var bridgeConsoleLog by remember { mutableStateOf("Android JavaScript Bridge initialized.\nwindow.AndroidBridge is available.") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Slate900,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Bridge Code",
                        tint = Cyan400
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Android Action Bridge Inspector",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("bridge_sheet_close_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                }
            }

            Text(
                text = "Spec Section 3: JavaScript-to-native Android action bridge for Web/Capacitor apps. Exposes openApp, makeCall, callContact, openWhatsApp, and openUrl via window.AndroidBridge.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Test Action Buttons
            Text(
                text = "Execute Bridge Commands",
                style = MaterialTheme.typography.labelMedium,
                color = Cyan400,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        val result = viewModel.actionBridge.openWhatsApp()
                        bridgeConsoleLog += "\n> window.AndroidBridge.openWhatsApp()\n$result"
                    },
                    modifier = Modifier.weight(1f).testTag("bridge_btn_whatsapp"),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Slate800, contentColor = Cyan400)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("openWhatsApp()", fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = {
                        val result = viewModel.actionBridge.callContact("Mummy")
                        bridgeConsoleLog += "\n> window.AndroidBridge.callContact('Mummy')\n$result"
                    },
                    modifier = Modifier.weight(1f).testTag("bridge_btn_call_mummy"),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Slate800, contentColor = Violet400)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("call('Mummy')", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        val result = viewModel.actionBridge.callContact("Rahul")
                        bridgeConsoleLog += "\n> window.AndroidBridge.callContact('Rahul')\n$result"
                    },
                    modifier = Modifier.weight(1f).testTag("bridge_btn_call_rahul"),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Slate800, contentColor = Violet400)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("call('Rahul')", fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = {
                        val result = viewModel.actionBridge.makeCall("9876543210")
                        bridgeConsoleLog += "\n> window.AndroidBridge.makeCall('9876543210')\n$result"
                    },
                    modifier = Modifier.weight(1f).testTag("bridge_btn_make_call"),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Slate800, contentColor = Cyan400)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("call('9876543210')", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        val result = viewModel.actionBridge.openApp("YouTube")
                        bridgeConsoleLog += "\n> window.AndroidBridge.openApp('YouTube')\n$result"
                    },
                    modifier = Modifier.weight(1f).testTag("bridge_btn_youtube"),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Slate800, contentColor = Color.White)
                ) {
                    Text("openApp('YouTube')", fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = {
                        val result = viewModel.actionBridge.openApp("Settings")
                        bridgeConsoleLog += "\n> window.AndroidBridge.openApp('Settings')\n$result"
                    },
                    modifier = Modifier.weight(1f).testTag("bridge_btn_settings"),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Slate800, contentColor = Color.White)
                ) {
                    Text("openApp('Settings')", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Console output display
            Text(
                text = "Live Bridge Output & WebView Client",
                style = MaterialTheme.typography.labelMedium,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate950),
                color = Slate950
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Text(
                        text = bridgeConsoleLog,
                        color = Color(0xFF34D399),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    // Embedded real Android WebView loading HTML using window.AndroidBridge
                    Box(modifier = Modifier.height(1.dp).fillMaxWidth()) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    addJavascriptInterface(viewModel.actionBridge, "AndroidBridge")
                                    webViewClient = WebViewClient()
                                    val html = """
                                        <!DOCTYPE html>
                                        <html>
                                        <body>
                                          <script>
                                            if (window.AndroidBridge && window.AndroidBridge.isBridgeAvailable()) {
                                              console.log("Native Bridge connected successfully.");
                                            }
                                          </script>
                                        </body>
                                        </html>
                                    """.trimIndent()
                                    loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
