package com.pilotothegreat.deencompanion

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.work.Configuration
import com.pilotothegreat.deencompanion.alarms.KhatmaReminderWorker
import com.pilotothegreat.deencompanion.alarms.Notifications
import com.pilotothegreat.deencompanion.alarms.PrayerAlarmScheduler
import com.pilotothegreat.deencompanion.alarms.RescheduleWorker
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.prayer.PrayerConfig
import com.pilotothegreat.deencompanion.data.athkar.AthkarRepository
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import com.pilotothegreat.deencompanion.data.settings.SoundSettings
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
    private val athkar: AthkarRepository by inject()
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
        KhatmaReminderWorker.enqueue(this)
        syncLanguage()
        refreshWidgetsOnUnlock()
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

    /**
     * Everything an alarm's time or existence depends on. The pre-reminder, the silence window and
     * travel all move alarms, so a change to any of them has to rebuild the set — otherwise the
     * setting appears to take effect and quietly does not until the next prayer.
     */
    private data class AlarmInputs(
        val config: PrayerConfig,
        val enabled: Boolean,
        val muted: Set<Prayer>,
        val athkar: Boolean,
        val sounds: SoundSettings,
        val travelling: Boolean,
    )

    @OptIn(FlowPreview::class)
    private fun keepAlarmsInSync() = appScope.launch {
        settings.settings
            .map { AlarmInputs(it.prayerConfig, it.notificationsEnabled, it.mutedPrayers, it.athkarReminders, it.sounds, it.smart.isTravelling) }
            .distinctUntilChanged()
            .debounce(300)
            .collectLatest { scheduler.reschedule() }
    }

    /**
     * Redraws the widgets when the phone is unlocked, so a countdown is never stale at the moment
     * someone actually looks at it.
     *
     * Registered here rather than in the manifest because ACTION_USER_PRESENT is an implicit
     * broadcast, and Android has not delivered those to manifest receivers since Oreo. That means it
     * only fires while this process happens to be alive — which is exactly when someone has been
     * using the app and is most likely to glance at its widgets. The half-hourly update and the
     * alarm at each prayer cover the rest.
     */
    private fun refreshWidgetsOnUnlock() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                appScope.launch { WidgetUpdater.updateAll(context) }
            }
        }
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        // Registered against the platform rather than ContextCompat: only the system can send this
        // broadcast, and the compat helper asks for a permission of its own to say so.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(receiver, filter)
        }
    }

    private fun keepChannelsLocalized() = appScope.launch {
        settings.settings.map { it.appLanguage }.distinctUntilChanged().collect {
            Notifications.createChannels(this@DeenApplication, it)
        }
    }

    @OptIn(FlowPreview::class)
    private fun keepWidgetsInSync() {
        appScope.launch {
            combine(settings.settings, tasbih.state, athkar.progress) { s, t, p -> Triple(s, t, p) }
                .distinctUntilChanged()
                .debounce(500)
                .collectLatest { WidgetUpdater.updateAll(this@DeenApplication) }
        }
        // The widget picker's previews are in the app's language and colours, so they follow both.
        appScope.launch {
            settings.settings.map { it.appLanguage to it.dynamicColor }
                .distinctUntilChanged()
                .collectLatest { WidgetUpdater.publishPreviews(this@DeenApplication) }
        }
    }
}
