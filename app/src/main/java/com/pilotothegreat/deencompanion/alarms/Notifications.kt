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
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.ui.common.nameRes

enum class AlarmKind { ADHAN, IQAMA }

object Notifications {
    const val CHANNEL_ADHAN = "adhan"
    const val CHANNEL_IQAMA = "iqama"
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
            ),
        )
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun showPrayer(context: Context, languageTag: String, kind: AlarmKind, prayer: Prayer) {
        if (!canPost(context)) return
        val res = AppLanguage.localizedContext(context, languageTag)
        val name = res.getString(prayer.nameRes)
        val (channel, title, body) = when (kind) {
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
        }
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(kind.ordinal * 10 + prayer.ordinal, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post.
        }
    }
}
