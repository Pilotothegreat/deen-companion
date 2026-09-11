package com.pilotothegreat.deencompanion

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import com.pilotothegreat.deencompanion.data.settings.ThemeMode
import com.pilotothegreat.deencompanion.ui.DeenApp
import com.pilotothegreat.deencompanion.ui.navigation.DeepLinks
import com.pilotothegreat.deencompanion.ui.theme.DeenTheme
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private val settings: SettingsRepository by inject()

    /** Screen requested by a notification or widget, handed to the navigator once. */
    private var destination by mutableStateOf<NavKey?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) destination = DeepLinks.parse(intent)
        setContent {
            val appSettings by settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val current = appSettings ?: return@setContent
            val darkTheme = when (current.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(darkTheme) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            DeenTheme(darkTheme = darkTheme, dynamicColor = current.dynamicColor, pureBlack = current.pureBlack) {
                DeenApp(settings = current, destination = destination, onDestinationOpened = { destination = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        DeepLinks.parse(intent)?.let { destination = it }
    }
}
