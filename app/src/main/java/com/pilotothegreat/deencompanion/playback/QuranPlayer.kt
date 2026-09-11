package com.pilotothegreat.deencompanion.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.pilotothegreat.deencompanion.data.quran.Reciter
import com.pilotothegreat.deencompanion.data.quran.Surah
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import timber.log.Timber

data class PlaybackState(
    val surah: Int = 0,
    val ayah: Int = 0,
    val verseCount: Int = 0,
    val reciter: Reciter = Reciter.MISHARY,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    /** Epoch millis when the sleep timer pauses playback, or null. */
    val sleepTimerEndsAt: Long? = null,
) {
    val isActive: Boolean get() = surah > 0
}

/**
 * App-wide handle on the recitation session. Commands issued before the MediaController has
 * connected are queued rather than dropped.
 */
class QuranPlayer(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controller = scope.async(start = CoroutineStart.LAZY) {
        val token = SessionToken(context, ComponentName(context, QuranPlaybackService::class.java))
        MediaController.Builder(context, token).buildAsync().await().also { it.addListener(listener) }
    }

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _errors = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emits when an ayah fails to load (usually no connection); playback stops. */
    val errors: SharedFlow<Unit> = _errors.asSharedFlow()

    private var current: Surah? = null
    private var sleepTimer: Job? = null

    fun play(surah: Surah, fromAyah: Int, reciter: Reciter) = withController { player ->
        current = surah
        val items = surah.verses.map { verse ->
            MediaItem.Builder()
                .setMediaId("${surah.number}:${verse.number}")
                .setUri(reciter.audioUrl(surah.number, verse.number))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("${surah.nameEnglish} ${surah.number}:${verse.number}")
                        .setArtist(context.getString(reciter.label))
                        .setAlbumTitle(surah.nameArabic)
                        .build(),
                )
                .build()
        }
        player.setMediaItems(items, (fromAyah - 1).coerceIn(0, items.lastIndex), 0L)
        player.prepare()
        player.play()
        _state.update {
            it.copy(surah = surah.number, ayah = fromAyah, verseCount = items.size, reciter = reciter, isPlaying = true)
        }
    }

    /** Restarts the current ayah with another reciter. */
    fun changeReciter(reciter: Reciter) {
        val surah = current ?: return _state.update { it.copy(reciter = reciter) }
        play(surah, _state.value.ayah.coerceAtLeast(1), reciter)
    }

    fun togglePlayPause() = withController { player ->
        when {
            player.isPlaying -> player.pause()
            player.playbackState == Player.STATE_IDLE -> {
                player.prepare()
                player.play()
            }
            player.playbackState == Player.STATE_ENDED -> {
                player.seekTo(0, 0L)
                player.play()
            }
            else -> player.play()
        }
    }

    fun next() = withController { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }

    fun previous() = withController { if (it.hasPreviousMediaItem()) it.seekToPreviousMediaItem() }

    fun stop() = withController { player ->
        player.stop()
        player.clearMediaItems()
        sleepTimer?.cancel()
        current = null
        _state.update { PlaybackState(reciter = it.reciter) }
    }

    /** Pauses after [minutes]; 0 cancels the timer. */
    fun setSleepTimer(minutes: Int) {
        sleepTimer?.cancel()
        if (minutes <= 0) {
            _state.update { it.copy(sleepTimerEndsAt = null) }
            return
        }
        val durationMillis = minutes * 60_000L
        _state.update { it.copy(sleepTimerEndsAt = System.currentTimeMillis() + durationMillis) }
        sleepTimer = scope.launch {
            delay(durationMillis)
            controller.await().pause()
            _state.update { it.copy(sleepTimerEndsAt = null) }
        }
    }

    private fun withController(block: (MediaController) -> Unit) {
        scope.launch {
            try {
                block(controller.await())
            } catch (e: Exception) {
                Timber.e(e, "Media controller unavailable")
            }
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            val ayah = player.currentMediaItem?.mediaId?.substringAfter(':')?.toIntOrNull()
            _state.update {
                it.copy(
                    ayah = ayah ?: it.ayah,
                    isPlaying = player.isPlaying,
                    isBuffering = player.playbackState == Player.STATE_BUFFERING,
                )
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Timber.w(error, "Recitation failed to load")
            _errors.tryEmit(Unit)
        }
    }
}
