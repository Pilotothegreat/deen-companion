package com.pilotothegreat.deencompanion

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
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

    /** Set once settings have been read, which is when there is a first frame worth showing. */
    private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) destination = DeepLinks.parse(intent)
        holdSplashUntilReady()
        setContent {
            val appSettings by settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val current = appSettings ?: return@setContent
            ready = true
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
            DeenTheme(
                darkTheme = darkTheme,
                dynamicColor = current.dynamicColor,
                pureBlack = current.pureBlack,
                accessibility = current.accessibility,
            ) {
                DeenApp(settings = current, destination = destination, onDestinationOpened = { destination = null })
            }
        }
    }

    /**
     * The system's splash stays up until the first frame is drawn, and the first frame used to be an
     * empty window while settings loaded: splash, blank, then the app. Holding the draw until they
     * are in keeps the splash on screen instead of the blank. A second is the most it will wait.
     */
    private fun holdSplashUntilReady() {
        val content = findViewById<View>(android.R.id.content)
        val started = SystemClock.uptimeMillis()
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (!ready && SystemClock.uptimeMillis() - started < SPLASH_LIMIT_MS) return false
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            },
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        DeepLinks.parse(intent)?.let { destination = it }
    }
}

private const val SPLASH_LIMIT_MS = 1_000L
