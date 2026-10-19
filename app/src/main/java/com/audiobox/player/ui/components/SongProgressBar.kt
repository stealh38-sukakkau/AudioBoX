package com.audiobox.player.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiobox.player.ui.theme.TextMuted
import com.audiobox.player.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongProgressBar(
    currentPositionMs: Long,
    durationMs: Long,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isUserDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val actualProgress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val displayProgress = if (isUserDragging) dragProgress else actualProgress

    val displayedPositionMs = if (isUserDragging) {
        (dragProgress * durationMs).toLong()
    } else {
        currentPositionMs
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = displayProgress,
            onValueChange = {
                isUserDragging = true
                dragProgress = it
            },
            onValueChangeFinished = {
                onSeek(dragProgress)
                isUserDragging = false
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White.copy(alpha = 0.85f),
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(displayedPositionMs),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
            val remainingMs = (durationMs - displayedPositionMs).coerceAtLeast(0L)
            Text(
                text = if (durationMs > 0) "-${formatDuration(remainingMs)}" else "--:--",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
