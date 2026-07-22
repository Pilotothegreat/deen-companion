// FIXED: Add tap contentIntent + AudioFocus request before MediaPlayer playback
package com.pilotothegreat.deencompanion.services

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pilotothegreat.deencompanion.MainActivity
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.core.app.NotificationManagerCompat
import com.pilotothegreat.deencompanion.R
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class IqamaAlarmReceiver : BroadcastReceiver(), KoinComponent {

    private val repo: AppPreferenceRepo by inject()

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Prayer"

            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.Default)
            var launched = false
            try {
                scope.launch {
                    try {
                        val lang = repo.appLanguage.first()
                        val config = android.content.res.Configuration(context.resources.configuration).apply {
                            setLocale(java.util.Locale(lang))
                        }
                        val localizedContext = context.createConfigurationContext(config)
                        val localizedPrayerName = when (prayerName) {
                            "Fajr" -> localizedContext.getString(R.string.fajr)
                            "Dhuhr" -> localizedContext.getString(R.string.dhuhr)
                            "Asr" -> localizedContext.getString(R.string.asr)
                            "Maghrib" -> localizedContext.getString(R.string.maghrib)
                            "Isha" -> localizedContext.getString(R.string.isha)
                            else -> prayerName
                        }
                        val mutedPrayers = repo.mutedPrayers.first()
                        if (mutedPrayers.contains(prayerName)) {
                            return@launch
                        }

                        showPrayerNotification(context, localizedContext, localizedPrayerName, isIqama = true)

                        val volume = repo.notificationVolume.first()
                        if (volume > 0) {
                            try {
                                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                                var focusGranted = false
                                var focusRequest: AudioFocusRequest? = null

                                if (audioManager != null) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        val playbackAttrs = AudioAttributes.Builder()
                                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                            .build()
                                        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                                            .setAudioAttributes(playbackAttrs)
                                            .setAcceptsDelayedFocusGain(false)
                                            .build()
                                        val result = audioManager.requestAudioFocus(focusRequest)
                                        focusGranted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                                    } else {
                                        @Suppress("DEPRECATION")
                                        val result = audioManager.requestAudioFocus(
                                            null,
                                            AudioManager.STREAM_NOTIFICATION,
                                            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                                        )
                                        focusGranted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                                    }
                                }

                                val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                                val mediaPlayer = android.media.MediaPlayer().apply {
                                    setDataSource(context, ringtoneUri)
                                    setAudioAttributes(
                                        AudioAttributes.Builder()
                                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                            .build()
                                    )
                                    val vol = volume / 100.0f
                                    setVolume(vol, vol)
                                    setOnCompletionListener { mp ->
                                        mp.release()
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
                                        } else {
                                            @Suppress("DEPRECATION")
                                            audioManager?.abandonAudioFocus(null)
                                        }
                                    }
                                    setOnErrorListener { mp, _, _ ->
                                        mp.release()
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
                                        } else {
                                            @Suppress("DEPRECATION")
                                            audioManager?.abandonAudioFocus(null)
                                        }
                                        true
                                    }
                                    prepare()
                                    start()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        IqamaAlarmManager.scheduleAllIqamaAlarms(context, repo)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try {
                            pendingResult.finish()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                launched = true
            } finally {
                if (!launched) {
                    try {
                        pendingResult.finish()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showPrayerNotification(context: Context, localizedContext: Context, prayerName: String, isIqama: Boolean) {
        val title = if (isIqama) {
            localizedContext.getString(R.string.iqama_notification_title, prayerName)
        } else {
            localizedContext.getString(R.string.adhan_notification_title, prayerName)
        }

        val body = if (isIqama) {
            localizedContext.getString(R.string.iqama_notification_body, prayerName)
        } else {
            localizedContext.getString(R.string.adhan_notification_body, prayerName)
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            prayerName.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "prayer_times")
            .setSmallIcon(R.drawable.notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(tapPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(null)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(prayerName.hashCode(), notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
