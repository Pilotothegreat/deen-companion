// FIXED: Add GPS/Notification permissions requests on launch and dynamic locale context providers
package com.leekleak.trafficlight

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.services.IqamaAlarmManager
import com.leekleak.trafficlight.ui.app.App
import com.leekleak.trafficlight.ui.theme.Theme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import org.koin.compose.koinInject
import java.util.Locale
import android.content.res.Configuration
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {

    private val appPreferenceRepo: AppPreferenceRepo by inject()

    override fun attachBaseContext(base: Context) {
        val sharedPrefs = base.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val lang = sharedPrefs.getString("app_language", "en") ?: "en"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        super.attachBaseContext(base.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Schedule iqama alarms on app launch
        lifecycleScope.launch {
            try {
                IqamaAlarmManager.scheduleNextIqamaAlarm(this@MainActivity, appPreferenceRepo)
            } catch (e: Exception) {
                e.printStackTrace()
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
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ).apply {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            add(android.Manifest.permission.POST_NOTIFICATIONS)
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

                Theme {
                    App()
                }
            }
        }
    }
}

@Composable
fun AppWithLocale(content: @Composable () -> Unit) {
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val lang by appPreferenceRepo.appLanguage.collectAsState(initial = "en")
    val context = LocalContext.current

    val localizedContext = remember(context, lang) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        context.createConfigurationContext(config)
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        androidx.compose.ui.platform.LocalConfiguration provides localizedContext.resources.configuration,
        LocalLayoutDirection provides if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        content()
    }
}