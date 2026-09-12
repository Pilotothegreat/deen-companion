package com.pilotothegreat.deencompanion.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.pilotothegreat.deencompanion.core.quran.PlaybackStep
import com.pilotothegreat.deencompanion.core.quran.RepeatMode
import com.pilotothegreat.deencompanion.core.quran.RepeatPlan
import com.pilotothegreat.deencompanion.data.net.NetError
import com.pilotothegreat.deencompanion.data.quran.QuranRepository
import com.pilotothegreat.deencompanion.data.quran.Reciter
import com.pilotothegreat.deencompanion.data.quran.Surah
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val repeatCount: Int = 3,
    /** How many times the repeating unit has already come round. */
    val repeatsDone: Int = 0,
    val speed: Float = 1f,
) {
    val isActive: Boolean get() = surah > 0
}

/**
 * App-wide handle on the recitation session. Commands issued before the MediaController has
 * connected are queued rather than dropped.
 */
class QuranPlayer(
    private val context: Context,
    private val quran: QuranRepository,
    private val settings: SettingsRepository,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controller = scope.async(start = CoroutineStart.LAZY) {
        val token = SessionToken(context, ComponentName(context, QuranPlaybackService::class.java))
        MediaController.Builder(context, token).buildAsync().await().also { it.addListener(listener) }
    }

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _errors = MutableSharedFlow<NetError>(extraBufferCapacity = 1)
    /** Emits when an ayah fails to load, saying why, so the reader is told something useful. */
    val errors: SharedFlow<NetError> = _errors.asSharedFlow()

    private var current: Surah? = null
    private var repeatsDone = 0
    private var repeatRange: IntRange = IntRange.EMPTY
    private var prefs = PlaybackPrefs()

    private data class PlaybackPrefs(
        val mode: RepeatMode = RepeatMode.OFF,
        val count: Int = 3,
        val speed: Float = 1f,
        val continuous: Boolean = true,
        val cacheMb: Int = 256,
    )

    init {
        scope.launch {
            settings.settings
                .map { PlaybackPrefs(it.quran.repeatMode, it.quran.repeatCount, it.quran.playbackSpeed, it.quran.continuousPlayback, it.quran.audioCacheMb) }
                .distinctUntilChanged()
                .collect { new ->
                    val speedChanged = new.speed != prefs.speed
                    prefs = new
                    AudioCache.setBudgetMb(new.cacheMb)
                    _state.update { it.copy(repeatMode = new.mode, repeatCount = new.count, speed = new.speed) }
                    if (speedChanged && _state.value.isActive) withController { it.setPlaybackSpeed(new.speed) }
                }
        }
    }

    fun play(surah: Surah, fromAyah: Int, reciter: Reciter) = withController { player ->
        current = surah
        repeatsDone = 0
        // A range repeat with nothing chosen runs from where playback started to the end of the surah.
        repeatRange = (fromAyah - 1).coerceIn(0, surah.verses.lastIndex)..surah.verses.lastIndex
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
        player.setPlaybackSpeed(prefs.speed)
        player.prepare()
        player.play()
        _state.update {
            it.copy(
                surah = surah.number,
                ayah = fromAyah,
                verseCount = items.size,
                reciter = reciter,
                isPlaying = true,
                repeatsDone = 0,
            )
        }
    }

    /** Repeat this span of ayahs (inclusive, 1-based) when the mode is RANGE. */
    fun setRepeatRange(fromAyah: Int, toAyah: Int) {
        val verses = current?.verses ?: return
        val from = (fromAyah - 1).coerceIn(0, verses.lastIndex)
        val to = (toAyah - 1).coerceIn(from, verses.lastIndex)
        repeatRange = from..to
        repeatsDone = 0
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
        sendSleepTimer(0L)
        current = null
        repeatsDone = 0
        _state.update { PlaybackState(reciter = it.reciter, repeatMode = prefs.mode, repeatCount = prefs.count, speed = prefs.speed) }
    }

    /** Pauses after [minutes]; 0 cancels the timer. */
    fun setSleepTimer(minutes: Int) {
        val millis = minutes.coerceAtLeast(0) * 60_000L
        _state.update { it.copy(sleepTimerEndsAt = if (millis > 0) System.currentTimeMillis() + millis else null) }
        sendSleepTimer(millis)
    }

    private fun sendSleepTimer(millis: Long) = withController { player ->
        val args = Bundle().apply { putLong(QuranPlaybackService.EXTRA_SLEEP_MILLIS, millis) }
        player.sendCustomCommand(SessionCommand(QuranPlaybackService.COMMAND_SLEEP_TIMER, Bundle.EMPTY), args)
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

    /** Applies the repeat rules once an ayah has finished. */
    private fun onAyahFinished(finished: Int, player: MediaController) {
        val surah = current ?: return
        val step = RepeatPlan.onAyahFinished(
            finished = finished,
            lastIndex = surah.verses.lastIndex,
            mode = prefs.mode,
            repeatCount = prefs.count,
            repeatsDone = repeatsDone,
            range = repeatRange,
            continueToNextSurah = prefs.continuous,
        )
        if (RepeatPlan.unitRestarted(step)) repeatsDone++ else repeatsDone = 0
        _state.update { it.copy(repeatsDone = repeatsDone) }
        when (step) {
            is PlaybackStep.SeekTo -> {
                player.seekTo(step.index, 0L)
                player.play()
            }
            PlaybackStep.NextSurah -> playNextSurah(surah.number)
            PlaybackStep.Stop -> stop()
            PlaybackStep.Advance -> Unit
        }
    }

    private fun playNextSurah(after: Int) {
        if (after >= 114) return stop()
        scope.launch { play(quran.quran().surah(after + 1), 1, _state.value.reciter) }
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

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            // Only an automatic transition means an ayah played to its end; a seek is the reader's doing.
            if (reason != Player.DISCONTINUITY_REASON_AUTO_TRANSITION) return
            withController { onAyahFinished(oldPosition.mediaItemIndex, it) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_ENDED) return
            val lastIndex = current?.verses?.lastIndex ?: return
            withController { onAyahFinished(lastIndex, it) }
        }

        override fun onPlayerError(error: PlaybackException) {
            Timber.w(error, "Recitation failed to load")
            _errors.tryEmit(error.toNetError())
        }
    }
}

/** Turns a playback failure into something worth showing: no connection, missing ayah, or a fault. */
internal fun PlaybackException.toNetError(): NetError = when (errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
    -> NetError.OFFLINE
    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> NetError.NOT_FOUND
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> NetError.FAILED
    else -> NetError.FAILED
}
