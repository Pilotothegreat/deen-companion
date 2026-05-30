package com.pilotothegreat.deencompanion.services

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import timber.log.Timber

class QuranPlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .build()

            player = ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true)
                .build()

            mediaSession = MediaSession.Builder(this, player!!)
                .build()
            Timber.i("QuranPlaybackService created successfully")
        } catch (e: Exception) {
            Timber.e(e, "Error creating QuranPlaybackService")
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null && !player.playWhenReady) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        try {
            player?.release()
            mediaSession?.release()
            mediaSession = null
            player = null
            Timber.i("QuranPlaybackService destroyed successfully")
        } catch (e: Exception) {
            Timber.e(e, "Error destroying QuranPlaybackService")
        }
        super.onDestroy()
    }
}
