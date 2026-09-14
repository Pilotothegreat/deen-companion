package com.pilotothegreat.deencompanion.alarms

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pilotothegreat.deencompanion.MainActivity
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.athkar.AthkarIds
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.ui.common.nameRes
import com.pilotothegreat.deencompanion.ui.navigation.DeepLinks

enum class AlarmKind {
    /** A few minutes before the adhan, so wudu and the walk to the mosque are not a scramble. */
    PRE_PRAYER,
    ADHAN,
    IQAMA,
    ATHKAR_MORNING,
    ATHKAR_EVENING,

    /** Optional: quieten the phone from iqama, and give it back afterwards. */
    SILENCE_START,
    SILENCE_END,

    /**
     * A nudge to redraw the progress bar between the adhan and the iqama. The countdown beside it
     * is a Chronometer and needs nothing; only the bar has to be re-posted.
     */
    IQAMA_PROGRESS_1,
    IQAMA_PROGRESS_2,
    IQAMA_PROGRESS_3;

    /** The checkpoint kinds, in order, so the scheduler can zip them with their times. */
    companion object {
        val progressCheckpoints: List<AlarmKind>
            get() = listOf(IQAMA_PROGRESS_1, IQAMA_PROGRESS_2, IQAMA_PROGRESS_3)
    }

    val isAthkar: Boolean get() = this == ATHKAR_MORNING || this == ATHKAR_EVENING

    /** Kinds that must land on the minute; the rest use inexact alarms and spare the battery. */
    val needsExactTime: Boolean get() = this == ADHAN || this == IQAMA || this == PRE_PRAYER || this == SILENCE_START
}

object Notifications {
    const val CHANNEL_ADHAN = "adhan"
    const val CHANNEL_IQAMA = "iqama"
    const val CHANNEL_ATHKAR = "athkar"
    const val CHANNEL_KHATMA = "khatma"
    private const val LEGACY_CHANNEL = "prayer_times"

    /** Creates or renames the channels in [languageTag]; call on start and when the language changes. */
    fun createChannels(context: Context, languageTag: String) {
        val res = AppLanguage.localizedContext(context, languageTag)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.deleteNotificationChannel(LEGACY_CHANNEL)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_ADHAN, res.getString(R.string.channel_adhan), NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = res.getString(R.string.channel_adhan_desc) },
                NotificationChannel(CHANNEL_IQAMA, res.getString(R.string.channel_iqama), NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = res.getString(R.string.channel_iqama_desc) },
                NotificationChannel(CHANNEL_ATHKAR, res.getString(R.string.channel_athkar), NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = res.getString(R.string.channel_athkar_desc) },
                NotificationChannel(CHANNEL_KHATMA, res.getString(R.string.channel_khatma), NotificationManager.IMPORTANCE_LOW)
                    .apply { description = res.getString(R.string.channel_khatma_desc) },
            ),
        )
    }

    /** Morning or evening reminder; tapping it opens that athkar session. */
    fun showAthkar(context: Context, languageTag: String, kind: AlarmKind) {
        if (!canPost(context)) return
        val res = AppLanguage.localizedContext(context, languageTag)
        val morning = kind == AlarmKind.ATHKAR_MORNING
        val open = PendingIntent.getActivity(
            context,
            1 + kind.ordinal,
            DeepLinks.athkar(context, if (morning) AthkarIds.MORNING else AthkarIds.EVENING),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ATHKAR)
            .setSmallIcon(R.drawable.notification)
            .setContentTitle(res.getString(if (morning) R.string.athkar_morning_reminder else R.string.athkar_evening_reminder))
            .setContentText(res.getString(R.string.athkar_reminder_body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            // One id for both, so the evening reminder replaces a morning one still sitting in the
            // shade rather than joining it.
            NotificationManagerCompat.from(context).notify(ATHKAR_NOTIFICATION, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post.
        }
    }

    private const val ATHKAR_NOTIFICATION = 910

    /**
     * The khatma nudge. It is low importance and only ever sent on a day the plan is actually
     * behind, so it never congratulates anyone for reading and never fires twice in a day.
     */
    fun showKhatma(context: Context, languageTag: String, pagesDue: Int, page: Int) {
        if (!canPost(context)) return
        val res = AppLanguage.localizedContext(context, languageTag)
        val open = PendingIntent.getActivity(
            context,
            KHATMA_REQUEST,
            DeepLinks.reader(context, page.coerceAtLeast(1)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_KHATMA)
            .setSmallIcon(R.drawable.notification)
            .setContentTitle(res.getString(R.string.khatma))
            .setContentText(res.resources.getQuantityString(R.plurals.khatma_pages_due, pagesDue, pagesDue.toString()))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(KHATMA_NOTIFICATION, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post.
        }
    }

    private const val KHATMA_REQUEST = 900
    private const val KHATMA_NOTIFICATION = 901

    /** The reader can switch a channel off in system settings, and the app cannot switch it back. */
    fun isAdhanChannelBlocked(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        val channel = manager.getNotificationChannel(CHANNEL_ADHAN) ?: return false
        return channel.importance == NotificationManager.IMPORTANCE_NONE
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /**
     * The one notification a prayer gets, from the adhan through to the iqama.
     *
     * It is built rather than posted three times: the adhan used to raise one notification from the
     * receiver and a second from the foreground service playing the audio, and the iqama added a
     * third, all saying the same thing. This returns a builder so the service can hand the same
     * notification to startForeground and later update it in place.
     *
     * The waiting state counts down with a Chronometer rather than a re-posted string. A chronometer
     * ticks in the shade with no process alive and is exact to the second; re-posting for it would
     * mean waking the app every minute for a clock Android can draw itself.
     */
    fun prayerWindow(
        context: Context,
        languageTag: String,
        prayer: Prayer,
        stage: PrayerStage,
        iqamaAt: Long?,
    ): NotificationCompat.Builder? {
        val res = AppLanguage.localizedContext(context, languageTag)
        val name = res.getString(prayer.nameRes)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ADHAN)
            .setSmallIcon(R.drawable.notification)
            .setContentTitle(res.getString(R.string.adhan_notification_title, name))
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDeleteIntent(action(context, PrayerActionReceiver.ACTION_STOP, prayer))
            .addAction(0, res.getString(R.string.action_prayed), action(context, PrayerActionReceiver.ACTION_PRAYED, prayer))

        when (stage) {
            PrayerStage.Adhan -> builder
                .setContentText(res.getString(R.string.adhan_notification_body, name))
                .setOngoing(true)
                .addAction(0, res.getString(R.string.stop_playback), action(context, PrayerActionReceiver.ACTION_STOP, prayer))

            is PrayerStage.Waiting -> {
                builder
                    .setContentText(res.getString(R.string.iqama_waiting_body, name))
                    .setOngoing(false)
                    .setOnlyAlertOnce(true)
                    .setProgress(PROGRESS_MAX, (stage.fraction * PROGRESS_MAX).toInt(), false)
                    .addAction(0, res.getString(R.string.action_snooze), action(context, PrayerActionReceiver.ACTION_SNOOZE, prayer))
                if (iqamaAt != null) {
                    // Android draws the remaining time itself, and keeps drawing it after the app is gone.
                    builder.setUsesChronometer(true).setChronometerCountDown(true).setWhen(iqamaAt)
                    builder.setShowWhen(true)
                }
            }

            PrayerStage.Iqama -> builder
                .setContentTitle(res.getString(R.string.iqama_notification_title, name))
                .setContentText(res.getString(R.string.iqama_notification_body, name))
                .setOngoing(false)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)

            PrayerStage.Done -> return null
        }
        return builder
    }

    /** Posts or updates the prayer's one notification; a null stage removes it. */
    fun showPrayerWindow(
        context: Context,
        languageTag: String,
        prayer: Prayer,
        stage: PrayerStage,
        iqamaAt: Long?,
    ) {
        val manager = NotificationManagerCompat.from(context)
        val builder = if (canPost(context)) prayerWindow(context, languageTag, prayer, stage, iqamaAt) else null
        try {
            // The "ten minutes to Dhuhr" notice has done its job the moment the adhan sounds, and
            // two Bilal notifications for one prayer is exactly what this release set out to end.
            manager.cancel(notificationId(AlarmKind.PRE_PRAYER, prayer))
            if (builder == null) manager.cancel(prayerWindowId(prayer)) else manager.notify(prayerWindowId(prayer), builder.build())
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post.
        }
    }

    /** The quieter notice before the adhan, which stays its own thing. */
    fun showPreReminder(context: Context, languageTag: String, prayer: Prayer) {
        if (!canPost(context)) return
        val res = AppLanguage.localizedContext(context, languageTag)
        val name = res.getString(prayer.nameRes)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_IQAMA)
            .setSmallIcon(R.drawable.notification)
            .setContentTitle(res.getString(R.string.pre_prayer_title, name))
            .setContentText(res.getString(R.string.pre_prayer_body, name))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId(AlarmKind.PRE_PRAYER, prayer), notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post.
        }
    }

    const val PROGRESS_MAX = 1000

    /** One id per prayer for the whole adhan-to-iqama window, so nothing can stack. */
    fun prayerWindowId(prayer: Prayer): Int = PRAYER_WINDOW_BASE + prayer.ordinal

    private const val PRAYER_WINDOW_BASE = 700

    /** Stable per kind and prayer, so an action can cancel exactly the notification it belongs to. */
    fun notificationId(kind: AlarmKind, prayer: Prayer): Int = kind.ordinal * 100 + prayer.ordinal

    private fun action(context: Context, action: String, prayer: Prayer): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            action.hashCode() + prayer.ordinal,
            PrayerActionReceiver.intent(context, action, prayer),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
