// v1.4.5: Consolidated locale management, replaced double-dialog with OnboardingScreen,
// removed SharedPreferences first-launch flag (now DataStore IS_ONBOARDING_COMPLETE),
// removed Robolectric checks from production code
package com.pilotothegreat.deencompanion

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import com.pilotothegreat.deencompanion.services.AdhanNotificationWorker
import com.pilotothegreat.deencompanion.services.IqamaAlarmManager
import com.pilotothegreat.deencompanion.ui.app.App
import com.pilotothegreat.deencompanion.ui.onboarding.OnboardingScreen
import com.pilotothegreat.deencompanion.ui.theme.Theme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val appPreferenceRepo: AppPreferenceRepo by inject()

    override fun attachBaseContext(base: Context) {
        // attachBaseContext fires before Koin/Compose/DataStore is ready.
        // The SharedPreferences "settings" store is kept in sync with DataStore
        // by AppPreferenceRepo.init(), so this read is always consistent.
        val sharedPrefs = base.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val lang = sharedPrefs.getString("app_language", "ar") ?: "ar"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        super.attachBaseContext(base.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()

            lifecycleScope.launch {
                try {
                    IqamaAlarmManager.scheduleNextIqamaAlarm(this@MainActivity, appPreferenceRepo)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to schedule iqama alarms on launch")
                }
                try {
                    val count = appPreferenceRepo.appLaunchCount.first()
                    appPreferenceRepo.setAppLaunchCount(count + 1)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to increment launch count")
                }
                try {
                    val workRequest = PeriodicWorkRequestBuilder<AdhanNotificationWorker>(
                        24, TimeUnit.HOURS
                    ).build()
                    WorkManager.getInstance(this@MainActivity).enqueueUniquePeriodicWork(
                        "adhan_scheduler",
                        ExistingPeriodicWorkPolicy.KEEP,
                        workRequest
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Failed to enqueue AdhanNotificationWorker on launch")
                }
            }

            setContent {
                AppWithLocale {
                    val repo: AppPreferenceRepo = koinInject()
                    // null = DataStore not yet read; false = show onboarding; true = show app
                    val isOnboardingComplete by repo.isOnboardingComplete.collectAsState(initial = null)

                    Theme {
                        when (isOnboardingComplete) {
                            null -> { /* Await DataStore first emission — brief blank splash */ }
                            false -> OnboardingScreen(onComplete = { /* state change triggers recompose */ })
                            true -> App()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "MainActivity onCreate error")
            throw e
        }
    }
}

/**
 * Applies the user's chosen language to the Composition context.
 * Single source of truth for locale in the Compose tree.
 *
 * Note: attachBaseContext handles the *initial* locale at Activity creation time
 * via SharedPreferences (the only hook available that early). This composable
 * handles *live* locale changes while the app is running.
 */
@Composable
fun AppWithLocale(content: @Composable () -> Unit) {
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val lang by appPreferenceRepo.appLanguage.collectAsState(initial = "ar")
    val context = LocalContext.current

    val localizedContext = remember(context, lang) {
        val locale = Locale(lang)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        val configCtx = context.createConfigurationContext(config)
        object : ContextWrapper(context) {
            override fun getResources() = configCtx.resources
            override fun getAssets() = configCtx.assets
            override fun getTheme() = configCtx.theme
        }
    }

    // Keep Android's per-app locale API in sync (covers Android 13+ system settings)
    LaunchedEffect(lang) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))

        // Pre-Android 13: recreate the Activity so resources fully reload
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val currentLanguage = context.resources.configuration.locales[0].language
            if (currentLanguage != lang) {
                var ctx = context
                while (ctx is ContextWrapper) {
                    if (ctx is Activity) { ctx.recreate(); break }
                    ctx = ctx.baseContext
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        androidx.compose.ui.platform.LocalConfiguration provides localizedContext.resources.configuration,
        LocalLayoutDirection provides if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        content()
    }
}
