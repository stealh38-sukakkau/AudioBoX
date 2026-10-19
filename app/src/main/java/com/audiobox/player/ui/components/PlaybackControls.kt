package com.audiobox.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.audiobox.player.model.RepeatMode
import com.audiobox.player.ui.theme.AudioboxAccent

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playIconScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.96f,
        animationSpec = tween(durationMillis = 200),
        label = "playIconScale"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle Button
        IconButton(
            onClick = onToggleShuffle,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = "Toggle Shuffle",
                tint = if (isShuffle) AudioboxAccent else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }

        // Previous Track
        IconButton(
            onClick = onSkipPrevious,
            modifier = Modifier.size(54.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous Song",
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }

        // Apple Music Style Prominent Play / Pause
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(playIconScale)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.3f)),
                    onClick = onPlayPause
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(52.dp)
            )
        }

        // Next Track
        IconButton(
            onClick = onSkipNext,
            modifier = Modifier.size(54.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next Song",
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }

        // Repeat Button
        IconButton(
            onClick = onToggleRepeat,
            modifier = Modifier.size(44.dp)
        ) {
            val icon = if (repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
            val tint = if (repeatMode != RepeatMode.OFF) AudioboxAccent else Color.White.copy(alpha = 0.5f)
            Icon(
                imageVector = icon,
                contentDescription = "Toggle Repeat",
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
