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
    SILENCE_END;

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
            NotificationManagerCompat.from(context).notify(kind.ordinal * 10, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post.
        }
    }

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
     * A prayer alert, with the two answers people actually have: "prayed" and "in a moment".
     *
     * The adhan itself is not a channel sound. A channel's sound is fixed when the channel is
     * created, several manufacturers cut long ones short, and Do Not Disturb silences them; the
     * adhan is played by AdhanService with alarm attributes instead, and this notification is what
     * stops it.
     */
    fun showPrayer(context: Context, languageTag: String, kind: AlarmKind, prayer: Prayer) {
        if (!canPost(context)) return
        val res = AppLanguage.localizedContext(context, languageTag)
        val name = res.getString(prayer.nameRes)
        val (channel, title, body) = when (kind) {
            AlarmKind.PRE_PRAYER -> Triple(
                CHANNEL_IQAMA,
                res.getString(R.string.pre_prayer_title, name),
                res.getString(R.string.pre_prayer_body, name),
            )
            AlarmKind.ADHAN -> Triple(
                CHANNEL_ADHAN,
                res.getString(R.string.adhan_notification_title, name),
                res.getString(R.string.adhan_notification_body, name),
            )
            AlarmKind.IQAMA -> Triple(
                CHANNEL_IQAMA,
                res.getString(R.string.iqama_notification_title, name),
                res.getString(R.string.iqama_notification_body, name),
            )
            AlarmKind.ATHKAR_MORNING, AlarmKind.ATHKAR_EVENING,
            AlarmKind.SILENCE_START, AlarmKind.SILENCE_END,
            -> return
        }
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(if (kind == AlarmKind.ADHAN) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        if (kind != AlarmKind.PRE_PRAYER) {
            builder.addAction(0, res.getString(R.string.action_prayed), action(context, PrayerActionReceiver.ACTION_PRAYED, prayer))
        }
        if (kind == AlarmKind.ADHAN) {
            builder.addAction(0, res.getString(R.string.action_snooze), action(context, PrayerActionReceiver.ACTION_SNOOZE, prayer))
            builder.setDeleteIntent(action(context, PrayerActionReceiver.ACTION_STOP, prayer))
        }
        try {
            NotificationManagerCompat.from(context).notify(notificationId(kind, prayer), builder.build())
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post.
        }
    }

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
