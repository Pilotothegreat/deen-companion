package com.pilotothegreat.deencompanion

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.pilotothegreat.deencompanion.alarms.Notifications
import com.pilotothegreat.deencompanion.alarms.PrayerAlarmScheduler
import com.pilotothegreat.deencompanion.alarms.RescheduleWorker
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.prayer.PrayerConfig
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import com.pilotothegreat.deencompanion.data.tasbih.TasbihRepository
import com.pilotothegreat.deencompanion.di.appModule
import com.pilotothegreat.deencompanion.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class DeenApplication : Application(), Configuration.Provider {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val settings: SettingsRepository by inject()
    private val tasbih: TasbihRepository by inject()
    private val scheduler: PrayerAlarmScheduler by inject()

    // WorkManager initializes on first use instead of through its startup provider.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        startKoin {
            androidContext(this@DeenApplication)
            modules(appModule)
        }
        RescheduleWorker.enqueue(this)
        syncLanguage()
        keepAlarmsInSync()
        keepChannelsLocalized()
        keepWidgetsInSync()
    }

    private fun syncLanguage() = appScope.launch {
        val migrated = settings.migrateLegacyLanguage()
        if (migrated != null) {
            withContext(Dispatchers.Main) { AppLanguage.apply(migrated) }
        } else {
            // The language may have been changed from system settings on Android 13+.
            val fromSystem = AppLanguage.fromSystemSettings(this@DeenApplication)
            if (fromSystem != null && fromSystem != settings.current().appLanguage) settings.setAppLanguage(fromSystem)
        }
    }

    private data class AlarmInputs(val config: PrayerConfig, val enabled: Boolean, val muted: Set<Prayer>, val athkar: Boolean)

    @OptIn(FlowPreview::class)
    private fun keepAlarmsInSync() = appScope.launch {
        settings.settings
            .map { AlarmInputs(it.prayerConfig, it.notificationsEnabled, it.mutedPrayers, it.athkarReminders) }
            .distinctUntilChanged()
            .debounce(300)
            .collectLatest { scheduler.reschedule() }
    }

    private fun keepChannelsLocalized() = appScope.launch {
        settings.settings.map { it.appLanguage }.distinctUntilChanged().collect {
            Notifications.createChannels(this@DeenApplication, it)
        }
    }

    @OptIn(FlowPreview::class)
    private fun keepWidgetsInSync() = appScope.launch {
        combine(settings.settings, tasbih.state) { s, t -> s to t }
            .distinctUntilChanged()
            .debounce(500)
            .collectLatest { WidgetUpdater.updateAll(this@DeenApplication) }
    }
}
