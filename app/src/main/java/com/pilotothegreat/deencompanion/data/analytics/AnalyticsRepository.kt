package com.pilotothegreat.deencompanion.data.analytics

import android.content.Context
import android.os.Build
import com.pilotothegreat.deencompanion.BuildConfig
import com.pilotothegreat.deencompanion.core.analytics.UsageCount
import com.pilotothegreat.deencompanion.core.analytics.UsageEnvironment
import com.pilotothegreat.deencompanion.core.analytics.UsageEvent
import com.pilotothegreat.deencompanion.core.analytics.UsageReport
import com.pilotothegreat.deencompanion.data.db.UsageDao
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.util.Locale

/**
 * What the app records about its own use, and the only thing that records it.
 *
 * Three rules hold this together. It counts nothing until someone turns it on — [SettingsRepository]
 * says so, and the switch is off in a fresh install. It counts only the events named in [UsageEvent],
 * as a number per day, so there is nothing in the table that describes a person. And it sends nothing
 * anywhere unless a collector was configured at build time *and* the switch is on: with no endpoint
 * the whole thing is a local tally the reader can look at and export by hand.
 */
class AnalyticsRepository(
    private val context: Context,
    private val dao: UsageDao,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    val enabled: Flow<Boolean> = settings.settings.map { it.analyticsEnabled }.distinctUntilChanged()

    /** True when this build has somewhere to send a report; false makes the feature local-only. */
    val hasCollector: Boolean = BuildConfig.ANALYTICS_ENDPOINT.isNotBlank()

    /**
     * Counts [event] if counting is on. Deliberately not a suspending function: a screen that has to
     * wait on analytics is a screen that has been slowed down by analytics.
     */
    fun record(event: UsageEvent) {
        scope.launch {
            runCatching {
                if (!settings.current().analyticsEnabled) return@launch
                dao.increment(today(), event.id)
            }.onFailure { Timber.w(it, "Could not count %s", event.id) }
        }
    }

    /** Everything inside the retention window, ready to be shown, exported or sent. */
    suspend fun report(): UsageReport {
        val since = LocalDate.now().minusDays(RETENTION_DAYS).toString()
        val rows = dao.since(since).map { UsageCount(it.day, it.event, it.count) }
        return UsageReport(
            environment = environment(),
            firstSeen = dao.firstDay() ?: today(),
            daysActive = dao.daysActive(),
            sessions = dao.total(UsageEvent.APP_OPENED.id),
            counts = rows,
        )
    }

    /** Drops everything older than the window; called when the app starts. */
    suspend fun prune() {
        runCatching { dao.deleteBefore(LocalDate.now().minusDays(RETENTION_DAYS).toString()) }
            .onFailure { Timber.w(it, "Could not prune usage counters") }
    }

    /** Empties the table. Turning the switch off does this, rather than leaving the history behind. */
    suspend fun clear() {
        runCatching { dao.clear() }.onFailure { Timber.w(it, "Could not clear usage counters") }
    }

    private fun environment() = UsageEnvironment(
        appVersion = BuildConfig.VERSION_NAME,
        appVersionCode = BuildConfig.VERSION_CODE,
        installSource = installSource(),
        androidSdk = Build.VERSION.SDK_INT,
        deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
        language = Locale.getDefault().language,
        country = Locale.getDefault().country,
    )

    /** Which build this is, which is the one thing worth knowing that the version name cannot say. */
    private fun installSource(): String {
        val installer = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
        }.getOrNull()
        return when (installer) {
            "com.android.vending" -> "play"
            null, "" -> "github"
            else -> "other"
        }
    }

    private fun today(): String = LocalDate.now().toString()

    private companion object {
        /** Three months is enough to see a season; keeping more would not be kept for any reason. */
        const val RETENTION_DAYS = 90L
    }
}
