package com.audiobox.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiobox.player.model.AudioDeviceCategory
import com.audiobox.player.model.AudioOutputState
import com.audiobox.player.model.AudioStreamInfo
import com.audiobox.player.model.BitPerfectStatus
import com.audiobox.player.ui.theme.AudioboxAccent
import com.audiobox.player.ui.theme.AudioboxSecondary
import com.audiobox.player.ui.theme.DarkBackground
import com.audiobox.player.ui.theme.DarkCardBorder
import com.audiobox.player.ui.theme.DarkSurfaceElevated
import com.audiobox.player.ui.theme.TextMuted
import com.audiobox.player.ui.theme.TextPrimary
import com.audiobox.player.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioOutputSheet(
    outputState: AudioOutputState,
    streamInfo: AudioStreamInfo,
    onToggleBitPerfect: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                            .background(AudioboxAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = AudioboxAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Audio Output & Bit-Perfect",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Kartu Format Sumber Audio (Source Stream Info)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SOURCE AUDIO STREAM",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AudioboxAccent
                        )

                        if (streamInfo.isHiRes) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AudioboxSecondary.copy(alpha = 0.25f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "HI-RES AUDIO",
                                    color = AudioboxSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Codec", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(text = streamInfo.codecName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        }
                        Column {
                            Text(text = "Sample Rate", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = if (streamInfo.sampleRate > 0) "${streamInfo.sampleRate} Hz" else "44.1 kHz",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        Column {
                            Text(text = "Bit Depth", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(text = streamInfo.bitDepth, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        }
                        Column {
                            Text(text = "Channels", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = if (streamInfo.channelCount == 1) "Mono" else "Stereo",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Kartu Perangkat Output Saat Ini
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val deviceIcon = when (outputState.deviceCategory) {
                        AudioDeviceCategory.USB_DAC -> Icons.Rounded.Usb
                        AudioDeviceCategory.WIRED_HEADSET -> Icons.Rounded.Headphones
                        AudioDeviceCategory.BLUETOOTH -> Icons.Rounded.Bluetooth
                        AudioDeviceCategory.BUILTIN_SPEAKER -> Icons.Rounded.Speaker
                        else -> Icons.Rounded.GraphicEq
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (outputState.isUsbDac) AudioboxSecondary.copy(alpha = 0.2f)
                                else AudioboxAccent.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = deviceIcon,
                            contentDescription = null,
                            tint = if (outputState.isUsbDac) AudioboxSecondary else AudioboxAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ACTIVE OUTPUT DEVICE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = outputState.activeDeviceName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = outputState.deviceCategory.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Kartu Mode Bit-Perfect & Pengaturan USB DAC
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "USB DAC Bit-Perfect Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Direct bypass of Android AudioFlinger resampling",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }

                        Switch(
                            checked = outputState.isBitPerfectEnabledByUser,
                            onCheckedChange = { onToggleBitPerfect(it) },
                            enabled = outputState.isUsbDac && (
                                outputState.bitPerfectStatus == BitPerfectStatus.SUPPORTED ||
                                outputState.bitPerfectStatus == BitPerfectStatus.REQUESTED
                            ),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AudioboxAccent,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status Badge
                    val (statusColor, statusBg) = when (outputState.bitPerfectStatus) {
                        BitPerfectStatus.REQUESTED -> Pair(Color(0xFF00E676), Color(0xFF00E676).copy(alpha = 0.15f))
                        BitPerfectStatus.SUPPORTED -> Pair(AudioboxAccent, AudioboxAccent.copy(alpha = 0.15f))
                        BitPerfectStatus.FAILED -> Pair(Color(0xFFFF5252), Color(0xFFFF5252).copy(alpha = 0.15f))
                        BitPerfectStatus.STANDARD_OUTPUT -> Pair(Color(0xFF64B5F6), Color(0xFF64B5F6).copy(alpha = 0.15f))
                        BitPerfectStatus.UNAVAILABLE -> Pair(Color(0xFFFFB74D), Color(0xFFFFB74D).copy(alpha = 0.15f))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusBg)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "STATUS: ${outputState.bitPerfectStatus.label.uppercase()}",
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = outputState.statusDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Catatan Keterbukaan Teknis (Honest Disclosure Box)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Bit-perfect audio on Android requires compatible USB DAC hardware and Android 14+ (API 34) AudioMixerAttributes support. Built-in 3.5 mm jack and phone speakers always pass through the standard Android OS audio mixer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
