// FIXED: Request SCHEDULE_EXACT_ALARM permission on first launch if Android 12+ and add try-catch wrappers
package com.pilotothegreat.deencompanion

import android.content.Context
import android.os.Bundle
import android.app.AlarmManager
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import com.pilotothegreat.deencompanion.services.IqamaAlarmManager
import com.pilotothegreat.deencompanion.ui.app.App
import com.pilotothegreat.deencompanion.ui.theme.Theme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import org.koin.compose.koinInject
import java.util.Locale
import android.content.res.Configuration
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {

    private val appPreferenceRepo: AppPreferenceRepo by inject()

    private val exactAlarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // User granted or denied exact alarm permission
    }

    override fun attachBaseContext(base: Context) {
        val isRobolectric = try {
            Class.forName("org.robolectric.Robolectric") != null
        } catch (e: Exception) {
            false
        }
        if (isRobolectric) {
            super.attachBaseContext(base)
            return
        }

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

            // Schedule iqama alarms on app launch
            lifecycleScope.launch {
                try {
                    IqamaAlarmManager.scheduleNextIqamaAlarm(this@MainActivity, appPreferenceRepo)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to schedule iqama alarms on launch")
                }
            }

            setContent {
                AppWithLocale {
                    // Request location/notification permissions on first launch
                    val showRationale = remember { mutableStateOf(false) }
                    val permissionsLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { }

                    val permissionsToRequest = remember {
                        mutableListOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ).apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }.toTypedArray()
                    }

                    val context = LocalContext.current
                    LaunchedEffect(Unit) {
                        val sharedPrefs = context.getSharedPreferences("deen_prefs", Context.MODE_PRIVATE)
                        val isFirstLaunch = sharedPrefs.getBoolean("first_launch", true)
                        if (isFirstLaunch) {
                            sharedPrefs.edit().putBoolean("first_launch", false).apply()
                            showRationale.value = true
                        }
                    }

                    if (showRationale.value) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = {
                                showRationale.value = false
                                permissionsLauncher.launch(permissionsToRequest)
                            },
                            title = { Text("Permissions Required") },
                            text = { Text("This app needs location for accurate prayer times and notifications for Iqama reminders.") },
                            confirmButton = {
                                androidx.compose.material3.TextButton(onClick = {
                                    showRationale.value = false
                                    permissionsLauncher.launch(permissionsToRequest)
                                }) {
                                    Text("OK")
                                }
                            }
                        )
                    }

                    // Android 12+ (API 31+) Exact Alarm Permission Request
                    var showExactAlarmRationale by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                                if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                                    showExactAlarmRationale = true
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Error checking exact alarm permission")
                            }
                        }
                    }

                    if (showExactAlarmRationale) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showExactAlarmRationale = false },
                            title = { Text("Exact Alarm Permission Required") },
                            text = { Text("This app needs exact alarm permission for accurate Iqama reminders.") },
                            confirmButton = {
                                androidx.compose.material3.TextButton(onClick = {
                                    showExactAlarmRationale = false
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        try {
                                            val intent = Intent(
                                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // Fallback in case Uri parse or launch throws
                                            exactAlarmPermissionLauncher.launch(Manifest.permission.SCHEDULE_EXACT_ALARM)
                                        }
                                    }
                                }) {
                                    Text("Grant")
                                }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(onClick = { showExactAlarmRationale = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    Theme {
                        App()
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "MainActivity onCreate error")
            throw e
        }
    }
}

@Composable
fun AppWithLocale(content: @Composable () -> Unit) {
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val lang by appPreferenceRepo.appLanguage.collectAsState(initial = "ar")
    val context = LocalContext.current

    val localizedContext = remember(context, lang) {
        val isRobolectric = try {
            Class.forName("org.robolectric.Robolectric") != null
        } catch (e: Exception) {
            false
        }

        if (isRobolectric) {
            context
        } else {
            val locale = Locale(lang)
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            val configurationContext = context.createConfigurationContext(config)
            object : android.content.ContextWrapper(context) {
                override fun getResources() = configurationContext.resources
                override fun getAssets() = configurationContext.assets
                override fun getTheme() = configurationContext.theme
            }
        }
    }

    LaunchedEffect(lang) {
        val isRobolectric = try {
            Class.forName("org.robolectric.Robolectric") != null
        } catch (e: Exception) {
            false
        }
        if (!isRobolectric) {
            val localeList = androidx.core.os.LocaleListCompat.forLanguageTags(lang)
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(localeList)
            
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                var actContext = context
                while (actContext is android.content.ContextWrapper) {
                    if (actContext is android.app.Activity) {
                        actContext.recreate()
                        break
                    }
                    actContext = actContext.baseContext
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
