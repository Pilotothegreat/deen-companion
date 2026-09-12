package com.pilotothegreat.deencompanion.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.pilotothegreat.deencompanion.BuildConfig
import com.pilotothegreat.deencompanion.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Hosts the recitation player so audio continues in the background with media controls. */
@OptIn(UnstableApi::class)
class QuranPlaybackService : MediaSessionService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var session: MediaSession? = null
    private var sleepTimer: Job? = null

    override fun onCreate() {
        super.onCreate()
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent("Bilal/${BuildConfig.VERSION_NAME} (Android; Media3)")
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
            .setAllowCrossProtocolRedirects(true)
        // Ayahs never change, so anything already on disk is played from there and never fetched again.
        val dataSource = CacheDataSource.Factory()
            .setCache(AudioCache.get(this, AudioCache.budgetBytes))
            .setUpstreamDataSourceFactory(http)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(dataSource))
            .build()
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        session = MediaSession.Builder(this, player).setSessionActivity(openApp).setCallback(callback).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /**
     * The sleep timer belongs to the session, not to the app. It used to run on an app-scoped
     * coroutine, so it could be torn down while this service kept reciting, and the recitation
     * would play on all night.
     */
    private fun setSleepTimer(millis: Long) {
        sleepTimer?.cancel()
        sleepTimer = if (millis <= 0) {
            null
        } else {
            scope.launch {
                delay(millis)
                session?.player?.pause()
            }
        }
    }

    private val callback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(COMMAND_SLEEP_TIMER, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.accept(
                commands,
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS,
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction != COMMAND_SLEEP_TIMER) {
                return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
            }
            setSleepTimer(args.getLong(EXTRA_SLEEP_MILLIS))
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        sleepTimer?.cancel()
        scope.cancel()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    companion object {
        const val COMMAND_SLEEP_TIMER = "com.pilotothegreat.deencompanion.SLEEP_TIMER"
        const val EXTRA_SLEEP_MILLIS = "sleep_millis"
    }
}
