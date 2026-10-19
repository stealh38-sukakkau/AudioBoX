package com.audiobox.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiobox.player.data.jellyfin.JellyfinConnectionState
import com.audiobox.player.data.jellyfin.JellyfinSession
import com.audiobox.player.ui.theme.AudioboxAccent
import com.audiobox.player.ui.theme.DarkBackground
import com.audiobox.player.ui.theme.DarkCardBorder
import com.audiobox.player.ui.theme.DarkSurfaceElevated
import com.audiobox.player.ui.theme.TextMuted
import com.audiobox.player.ui.theme.TextPrimary
import com.audiobox.player.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JellyfinSettingsSheet(
    connectionState: JellyfinConnectionState,
    currentSession: JellyfinSession?,
    savedServerUrl: String,
    savedUsername: String,
    isLoading: Boolean,
    errorMessage: String?,
    onConnect: (serverUrl: String, username: String, password: String) -> Unit,
    onRefresh: () -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var serverUrlInput by remember(currentSession?.serverUrl, savedServerUrl) { mutableStateOf(currentSession?.serverUrl ?: savedServerUrl) }
    var usernameInput by remember(currentSession?.username, savedUsername) { mutableStateOf(currentSession?.username ?: savedUsername) }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    LaunchedEffect(connectionState) {
        if (connectionState == JellyfinConnectionState.CONNECTED) passwordInput = ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00A4DC).copy(alpha = 0.2f)), // Jellyfin cyan
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cloud,
                            contentDescription = null,
                            tint = Color(0xFF00A4DC),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Jellyfin Server",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Remote Audio Streaming",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Jika Sudah Terhubung (Connected State)
            if (connectionState != JellyfinConnectionState.DISCONNECTED && currentSession != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00A4DC).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (connectionState) {
                                    JellyfinConnectionState.CONNECTED -> "CONNECTED"
                                    JellyfinConnectionState.CONNECTING -> "REFRESHING LIBRARY"
                                    JellyfinConnectionState.ERROR -> "SERVER UNAVAILABLE"
                                    JellyfinConnectionState.DISCONNECTED -> "DISCONNECTED"
                                },
                                color = if (connectionState == JellyfinConnectionState.CONNECTED) Color(0xFF00E676) else Color(0xFFFFB74D),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = currentSession.serverName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentSession.serverUrl,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Logged in as ${currentSession.username} • Jellyfin ${currentSession.serverVersion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )

                        if (!errorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(errorMessage, color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        if (connectionState != JellyfinConnectionState.CONNECTED) {
                            OutlinedButton(
                                onClick = onRefresh,
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isLoading) "Retrying…" else "Retry library connection")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        OutlinedButton(
                            onClick = onDisconnect,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Disconnect & Remove Session", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                // 2. Form Login / Sambungkan Server (Disconnected / Connecting / Error State)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "CONNECT TO SERVER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AudioboxAccent,
                            letterSpacing = 0.8.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Input Server URL
                        OutlinedTextField(
                            value = serverUrlInput,
                            onValueChange = { serverUrlInput = it },
                            label = { Text("Server URL", color = TextSecondary) },
                            placeholder = { Text("https://jellyfin.example.com", color = TextMuted) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Dns, contentDescription = null, tint = TextMuted)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = Color(0xFF00A4DC),
                                unfocusedBorderColor = DarkCardBorder,
                                cursorColor = Color(0xFF00A4DC)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Input Username
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text("Username", color = TextSecondary) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = TextMuted)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = Color(0xFF00A4DC),
                                unfocusedBorderColor = DarkCardBorder,
                                cursorColor = Color(0xFF00A4DC)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Input Password
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password", color = TextSecondary) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Lock, contentDescription = null, tint = TextMuted)
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = "Toggle password visibility",
                                        tint = TextMuted
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = Color(0xFF00A4DC),
                                unfocusedBorderColor = DarkCardBorder,
                                cursorColor = Color(0xFF00A4DC)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Error Banner jika ada
                        if (!errorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFF5252).copy(alpha = 0.15f))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFF5252),
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tombol Hubungkan
                        Button(
                            onClick = {
                                onConnect(serverUrlInput.trim(), usernameInput.trim(), passwordInput)
                            },
                            enabled = serverUrlInput.isNotBlank() && usernameInput.isNotBlank() && passwordInput.isNotBlank() && !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00A4DC),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Connecting...")
                            } else {
                                Text("Connect to Jellyfin", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Catatan Keamanan
            Text(
                text = "AudioBoX connects directly to your Jellyfin instance using standard token-based authentication. Passwords are never saved in plain text, and TLS certificate validation remains enforced.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                lineHeight = 16.sp
            )
        }
    }
}
