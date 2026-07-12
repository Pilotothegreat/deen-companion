package com.pilotothegreat.deencompanion.services

import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import timber.log.Timber

class QuranPlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    companion object {
        private const val CMD_SKIP_PREV = "CMD_SKIP_PREV"
        private const val CMD_SKIP_NEXT = "CMD_SKIP_NEXT"
    }

    override fun onCreate() {
        super.onCreate()
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .build()

            player = ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
                .build()

            mediaSession = MediaSession.Builder(this, player!!)
                .setCallback(object : MediaSession.Callback {
                    override fun onConnect(
                        session: MediaSession,
                        controller: MediaSession.ControllerInfo
                    ): MediaSession.ConnectionResult {
                        // Allow system notification controls
                        val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                            .add(SessionCommand(CMD_SKIP_PREV, Bundle.EMPTY))
                            .add(SessionCommand(CMD_SKIP_NEXT, Bundle.EMPTY))
                            .build()
                        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                            .setAvailableSessionCommands(sessionCommands)
                            .build()
                    }

                    override fun onCustomCommand(
                        session: MediaSession,
                        controller: MediaSession.ControllerInfo,
                        customCommand: SessionCommand,
                        args: Bundle
                    ): ListenableFuture<SessionResult> {
                        val p = player ?: return Futures.immediateFuture(SessionResult(SessionError.ERROR_UNKNOWN))
                        when (customCommand.customAction) {
                            CMD_SKIP_PREV -> {
                                val idx = p.currentMediaItemIndex
                                if (idx > 0) p.seekTo(idx - 1, 0L)
                            }
                            CMD_SKIP_NEXT -> {
                                val idx = p.currentMediaItemIndex
                                if (idx < p.mediaItemCount - 1) p.seekTo(idx + 1, 0L)
                            }
                        }
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                })
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
