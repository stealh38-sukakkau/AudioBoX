package com.audiobox.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.audiobox.player.model.AlbumColorPalette
import com.audiobox.player.ui.theme.DarkBackground

@Composable
fun DynamicBackground(
    palette: AlbumColorPalette?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val primaryColor by animateColorAsState(
        targetValue = palette?.primary ?: DarkBackground,
        animationSpec = tween(durationMillis = 800),
        label = "primaryAura"
    )
    val secondaryColor by animateColorAsState(
        targetValue = palette?.secondary ?: DarkBackground,
        animationSpec = tween(durationMillis = 800),
        label = "secondaryAura"
    )
    val baseBackground by animateColorAsState(
        targetValue = palette?.background ?: DarkBackground,
        animationSpec = tween(durationMillis = 800),
        label = "baseBackground"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBackground)
    ) {
        // Gradient Mesh / Aura Layer 1 (Top Left Primary Glow)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.50f),
                            Color.Transparent
                        ),
                        center = Offset(250f, 350f),
                        radius = 950f
                    )
                )
        )

        // Gradient Mesh / Aura Layer 2 (Bottom Right Secondary Glow)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = 0.40f),
                            Color.Transparent
                        ),
                        center = Offset(850f, 1500f),
                        radius = 1100f
                    )
                )
        )

        // Apple Music Dark Frost & Vignette Overlay (Ensures text & controls remain 100% readable)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.40f),
                            Color.Black.copy(alpha = 0.20f),
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        content()
    }
}
