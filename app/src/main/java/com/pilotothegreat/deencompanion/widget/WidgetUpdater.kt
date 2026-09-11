package com.pilotothegreat.deencompanion.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.pilotothegreat.deencompanion.MainActivity
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import com.pilotothegreat.deencompanion.data.tasbih.TasbihRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

internal object WidgetDeps : KoinComponent {
    val settings: SettingsRepository by inject()
    val tasbih: TasbihRepository by inject()
}

object WidgetUpdater {
    /** Re-renders every placed widget; widgets that aren't on the home screen are skipped. */
    suspend fun updateAll(context: Context) {
        runCatching { PrayerWidgetProvider.render(context) }.onFailure { Timber.w(it, "Prayer widget update failed") }
        runCatching { TasbihWidgetProvider.render(context) }.onFailure { Timber.w(it, "Tasbih widget update failed") }
        runCatching { InspirationWidgetProvider.render(context) }.onFailure { Timber.w(it, "Inspiration widget update failed") }
    }
}

internal fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
    context,
    0,
    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)
