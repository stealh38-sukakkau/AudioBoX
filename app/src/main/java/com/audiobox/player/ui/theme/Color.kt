package com.audiobox.player.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf

// Apple Music Inspired Dark Palette
private val backgroundState = mutableStateOf(Color(0xFF000000))
private val surfaceState = mutableStateOf(Color(0xFF121212))
private val elevatedState = mutableStateOf(Color(0xFF1C1C1E))
private val highlightState = mutableStateOf(Color(0xFF2C2C2E))
private val borderState = mutableStateOf(Color(0x1FFFFFFF))
private val primaryTextState = mutableStateOf(Color.White)
private val secondaryTextState = mutableStateOf(Color(0xFF98989D))
private val mutedTextState = mutableStateOf(Color(0xFF636366))
private val tertiaryTextState = mutableStateOf(Color(0xFF48484A))
val DarkBackground get() = backgroundState.value
val DarkSurface get() = surfaceState.value
val DarkSurfaceElevated get() = elevatedState.value
val DarkSurfaceHighlight get() = highlightState.value
val DarkCardBorder get() = borderState.value

// Signature Apple Music Pink-Red Accent with Audiobox identity
val AudioboxAccent = Color(0xFFFA2D48)          // Apple Music vibrant coral-red
val AudioboxSecondary = Color(0xFFFF375F)       // Secondary rose pink
val AudioboxGold = Color(0xFFFFD60A)            // Hi-Res gold badge accent

// Crisp White & Apple Gray Typography
val TextPrimary get() = primaryTextState.value
val TextSecondary get() = secondaryTextState.value
val TextMuted get() = mutedTextState.value
val TextTertiary get() = tertiaryTextState.value

internal fun setAudioboxPalette(light: Boolean) {
    if (light) {
        backgroundState.value = Color(0xFFF7F7F8)
        surfaceState.value = Color.White
        elevatedState.value = Color(0xFFEFEFF1)
        highlightState.value = Color(0xFFE2E2E7)
        borderState.value = Color(0x1F000000)
        primaryTextState.value = Color(0xFF17171B)
        secondaryTextState.value = Color(0xFF57575F)
        mutedTextState.value = Color(0xFF74747C)
        tertiaryTextState.value = Color(0xFF96969D)
    } else {
        backgroundState.value = Color(0xFF000000)
        surfaceState.value = Color(0xFF121212)
        elevatedState.value = Color(0xFF1C1C1E)
        highlightState.value = Color(0xFF2C2C2E)
        borderState.value = Color(0x1FFFFFFF)
        primaryTextState.value = Color.White
        secondaryTextState.value = Color(0xFF98989D)
        mutedTextState.value = Color(0xFF636366)
        tertiaryTextState.value = Color(0xFF48484A)
    }
}
