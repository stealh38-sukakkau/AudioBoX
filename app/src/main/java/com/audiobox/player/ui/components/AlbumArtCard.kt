package com.audiobox.player.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.audiobox.player.ui.theme.AudioboxAccent
import com.audiobox.player.ui.theme.AudioboxSecondary
import com.audiobox.player.ui.theme.DarkCardBorder
import com.audiobox.player.ui.theme.DarkSurfaceElevated

@Composable
fun AlbumArtCard(
    artworkUrl: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp
) {
    // Keep artwork visually steady while giving playback state a subtle response.
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.97f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "albumArtScale"
    )

    val shadowElevation by animateDpAsState(
        targetValue = if (isPlaying) 22.dp else 12.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "albumArtShadow"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .aspectRatio(1f)
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(cornerRadius),
                spotColor = Color.Black.copy(alpha = 0.75f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(cornerRadius))
            .background(DarkSurfaceElevated)
    ) {
        if (artworkUrl.isNotBlank()) {
            AsyncImage(
                model = jellyfinArtworkRequest(artworkUrl),
                contentDescription = "Album Artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Subtle top-to-bottom ambient sheen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.25f)
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                AudioboxAccent.copy(alpha = 0.35f),
                                AudioboxSecondary.copy(alpha = 0.35f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = "Audio track",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxSize(0.35f)
                )
            }
        }
    }
}
