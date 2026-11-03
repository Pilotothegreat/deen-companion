package com.pilotothegreat.deencompanion.ui.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.pilotothegreat.deencompanion.MainActivity
import com.pilotothegreat.deencompanion.core.prayer.DaySchedule
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.nameRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.time.ZonedDateTime

/**
 * The next prayer and its time, in the quick settings shade.
 *
 * One glance without unlocking, which is what the question "how long do I have?" actually deserves.
 * The tile updates when the shade opens rather than on a schedule, so it costs nothing while it is
 * out of sight.
 */
class NextPrayerTileService : TileService() {

    private val settings: SettingsRepository by inject()
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope.launch {
            val label = withContext(Dispatchers.Default) { nextPrayerLabel() } ?: return@launch
            qsTile?.apply {
                state = Tile.STATE_ACTIVE
                this.label = label.first
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) subtitle = label.second
                contentDescription = "${label.first} ${label.second}"
                updateTile()
            }
        }
    }

    override fun onStopListening() {
        scope.cancel()
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }

    private suspend fun nextPrayerLabel(): Pair<String, String>? {
        val current = settings.current()
        val res = AppLanguage.localizedContext(this, current.appLanguage)
        val locale = AppLanguage.locale(current.appLanguage)
        val next = DaySchedule.next(ZonedDateTime.now(current.zone), current.prayerConfig)
        return res.getString(next.prayer.nameRes) to Formatters.time(this, next.adhan.toLocalTime(), locale)
    }
}
