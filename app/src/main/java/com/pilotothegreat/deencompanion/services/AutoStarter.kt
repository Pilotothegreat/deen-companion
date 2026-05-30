package com.pilotothegreat.deencompanion.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class AutoStarter : BroadcastReceiver(), KoinComponent {
    private val appPreferenceRepo: AppPreferenceRepo by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    IqamaAlarmManager.scheduleNextIqamaAlarm(context, appPreferenceRepo)
                } catch (e: Exception) {
                    Timber.e(e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
