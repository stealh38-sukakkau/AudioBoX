package com.audiobox.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import java.util.Locale
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.audiobox.player.data.MusicRepositoryImpl
import com.audiobox.player.data.UserPreferences
import com.audiobox.player.ui.theme.AudioboxTheme
import com.audiobox.player.ui.viewmodel.NowPlayingViewModel

class MainActivity : ComponentActivity() {
    private var playbackViewModel: NowPlayingViewModel? = null

    companion object {
        const val EXTRA_OPEN_NOW_PLAYING = "com.audiobox.player.extra.OPEN_NOW_PLAYING"
    }

    override fun attachBaseContext(newBase: Context) {
        val language = com.audiobox.player.data.UserPreferences(newBase).language.value
        val tag = when (language) {
            com.audiobox.player.data.AppLanguageChoice.INDONESIAN -> "id"
            com.audiobox.player.data.AppLanguageChoice.ENGLISH -> "en"
            com.audiobox.player.data.AppLanguageChoice.SYSTEM -> ""
        }
        if (tag.isBlank()) {
            super.attachBaseContext(newBase)
        } else {
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(Locale.forLanguageTag(tag))
            super.attachBaseContext(newBase.createConfigurationContext(config))
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferences = UserPreferences(applicationContext)
        val repository = MusicRepositoryImpl(applicationContext, preferences)
        val viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NowPlayingViewModel(application, repository, preferences) as T
                }
            }
        )[NowPlayingViewModel::class.java]
        playbackViewModel = viewModel
        consumeNotificationIntent(intent)

        setContent {
            val themeChoice by viewModel.preferences.theme.collectAsStateWithLifecycle()
            AudioboxTheme(themeChoice) {
                AudioboxApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeNotificationIntent(intent)
    }

    private fun consumeNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_NOW_PLAYING, false) == true) {
            intent.removeExtra(EXTRA_OPEN_NOW_PLAYING)
            playbackViewModel?.setNowPlayingExpanded(true)
        }
    }
}
