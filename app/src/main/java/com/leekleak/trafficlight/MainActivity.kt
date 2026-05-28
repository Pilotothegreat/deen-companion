package com.leekleak.trafficlight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.services.IqamaAlarmManager
import com.leekleak.trafficlight.ui.app.App
import com.leekleak.trafficlight.ui.theme.Theme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val appPreferenceRepo: AppPreferenceRepo by inject()

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
            Theme {
                App()
            }
        }
    }
}