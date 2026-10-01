package com.pilotothegreat.deencompanion.alarms

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.pilotothegreat.deencompanion.MainActivity
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.ui.common.nameRes
import timber.log.Timber

/**
 * Plays the adhan.
 *
 * Not as a notification channel sound, which is the obvious way and the wrong one: a channel's sound
 * is fixed the moment the channel is created and cannot be changed afterwards without recreating it
 * and losing the user's own settings, several manufacturers truncate long channel sounds to a few
 * seconds, and Do Not Disturb silences them outright. A short foreground service with alarm audio
 * attributes plays the whole adhan, survives the screen being off, and is heard through Do Not
 * Disturb as an alarm should be.
 *
 * There is no screen takeover. The notification carries a Stop action and that is the whole
 * interface; full-screen adhan stays available to anyone who wants it, off by default.
 */
class AdhanService : Service() {

    private var player: MediaPlayer? = null
    private var prayer: Prayer? = null
    private var languageTag: String = ""
    private var iqamaAt: Long? = null
    /** True once the countdown has been handed over, so tearing down must not cancel it. */
    private var handedOver = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val prayer = Prayer.fromKey(intent?.getStringExtra(EXTRA_PRAYER)) ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        this.prayer = prayer
        languageTag = intent?.getStringExtra(EXTRA_LANGUAGE).orEmpty()
        iqamaAt = intent?.getLongExtra(EXTRA_IQAMA_AT, 0L)?.takeIf { it > 0L }
        val sound = intent?.getStringExtra(EXTRA_SOUND)

        startForeground(prayer, languageTag)
        play(sound)
        return START_NOT_STICKY
    }

    private fun startForeground(prayer: Prayer, languageTag: String) {
        val notification: Notification = Notifications
            .prayerWindow(this, languageTag, prayer, PrayerStage.Adhan, iqamaAt)
            ?.build()
            ?: return stopSelf()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(this, notificationId(prayer), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            ServiceCompat.startForeground(this, notificationId(prayer), notification, 0)
        }
    }

    /**
     * The audio is over but the prayer is not. The same notification becomes the countdown to the
     * iqama and is detached from the service, so it survives this process going away — a chronometer
     * keeps ticking in the shade with nothing running behind it.
     */
    private fun handOverToCountdown() {
        val prayer = prayer ?: return
        handedOver = true
        val stage = PrayerWindow.stageAt(
            now = System.currentTimeMillis(),
            adhanAt = System.currentTimeMillis(),
            iqamaAt = iqamaAt,
            adhanPlaying = false,
        )
        Notifications.showPrayerWindow(this, languageTag, prayer, stage, iqamaAt)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun play(sound: String?) {
        val uri = sound?.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: return handOverToCountdown()
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        // ALARM, so the adhan is heard through Do Not Disturb and at alarm volume.
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(this@AdhanService, uri)
                setOnCompletionListener { this@AdhanService.handOverToCountdown() }
                setOnErrorListener { _, _, _ ->
                    this@AdhanService.handOverToCountdown()
                    true
                }
                prepare()
                start()
            }
        }.onFailure {
            Timber.w(it, "Could not play the adhan")
            handOverToCountdown()
        }.getOrNull()
    }

    override fun onDestroy() {
        player?.runCatching {
            if (isPlaying) stop()
            release()
        }
        player = null
        // Only tear the notification down if it was not handed on as the iqama countdown.
        if (!handedOver) prayer?.let { NotificationManagerCompat.from(this).cancel(notificationId(it)) }
        super.onDestroy()
    }

    private fun notificationId(prayer: Prayer) = Notifications.prayerWindowId(prayer)

    companion object {
        private const val ACTION_STOP = "com.pilotothegreat.deencompanion.action.STOP_ADHAN"
        private const val EXTRA_PRAYER = "prayer"
        private const val EXTRA_LANGUAGE = "language"
        private const val EXTRA_SOUND = "sound"
        private const val EXTRA_IQAMA_AT = "iqama_at"

        /** Starts the adhan for [prayer]; [sound] is a URI, or null for the phone's own alarm sound. */
        fun start(context: Context, prayer: Prayer, languageTag: String, sound: String?, iqamaAt: Long = 0L) {
            val resolved = sound ?: defaultAlarmUri(context)
            val intent = Intent(context, AdhanService::class.java)
                .putExtra(EXTRA_PRAYER, prayer.key)
                .putExtra(EXTRA_LANGUAGE, languageTag)
                .putExtra(EXTRA_SOUND, resolved)
                .putExtra(EXTRA_IQAMA_AT, iqamaAt)
            runCatching { context.startForegroundService(intent) }
                .onFailure { Timber.w(it, "Could not start the adhan service") }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, AdhanService::class.java)) }
        }

        private fun defaultAlarmUri(context: Context): String? =
            android.media.RingtoneManager
                .getActualDefaultRingtoneUri(context, android.media.RingtoneManager.TYPE_NOTIFICATION)
                ?.toString()

        @Suppress("unused")
        private val audioManagerHint = AudioManager.STREAM_ALARM
    }
}
