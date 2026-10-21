package com.audiobox.player.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import com.audiobox.player.data.AppThemeChoice
import androidx.core.view.WindowCompat

@Composable
fun AudioboxTheme(
    choice: AppThemeChoice = AppThemeChoice.DARK,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val isLight = when (choice) {
        AppThemeChoice.LIGHT -> true
        AppThemeChoice.DARK -> false
        AppThemeChoice.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme().not()
    }
    val scheme = if (isLight) {
        lightColorScheme(
            primary = AudioboxAccent, secondary = AudioboxSecondary,
            background = Color(0xFFF7F7F8), surface = Color.White,
            onPrimary = Color.White, onSecondary = Color.White,
            onBackground = Color(0xFF17171B), onSurface = Color(0xFF17171B)
        )
    } else {
        darkColorScheme(
            primary = AudioboxAccent, secondary = AudioboxSecondary,
            background = Color(0xFF000000), surface = Color(0xFF121212),
            onPrimary = Color.White, onSecondary = Color.White,
            onBackground = Color.White, onSurface = Color.White
        )
    }
    if (!view.isInEditMode) {
        SideEffect {
            setAudioboxPalette(isLight)
            val window = (view.context as Activity).window
            val bars = scheme.background.toArgb()
            window.statusBarColor = bars
            window.navigationBarColor = bars
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLight
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content
    )
}
