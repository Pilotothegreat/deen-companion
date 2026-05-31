package com.pilotothegreat.deencompanion.services

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.SessionToken
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.pilotothegreat.deencompanion.ui.quran.QuranHelper
import com.pilotothegreat.deencompanion.ui.quran.Reciter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.TimeUnit

class QuranPlaybackManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSurahId = MutableStateFlow(1)
    val currentSurahId: StateFlow<Int> = _currentSurahId.asStateFlow()

    private val _currentAyahId = MutableStateFlow(1)
    val currentAyahId: StateFlow<Int> = _currentAyahId.asStateFlow()

    private val _selectedReciter = MutableStateFlow(Reciter.MISHARY)
    val selectedReciter: StateFlow<Reciter> = _selectedReciter.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _sleepTimerRemaining = MutableStateFlow(0L) // Remaining seconds
    val sleepTimerRemaining: StateFlow<Long> = _sleepTimerRemaining.asStateFlow()

    private val _endOfSurahEnabled = MutableStateFlow(false)
    val endOfSurahEnabled: StateFlow<Boolean> = _endOfSurahEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(false)
    val repeatMode: StateFlow<Boolean> = _repeatMode.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private var sleepTimerJob: Job? = null

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, QuranPlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                controller = controllerFuture?.get()
                setupControllerListener()
                Timber.i("MediaController bound successfully to QuranPlaybackService")
            } catch (e: Exception) {
                Timber.e(e, "Error binding MediaController")
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupControllerListener() {
        val player = controller ?: return
        
        // Sync initial player state
        _isPlaying.value = player.isPlaying
        player.currentMediaItem?.let { updateStateFromMediaItem(it) }

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = playbackState == Player.STATE_BUFFERING
                if (_endOfSurahEnabled.value && playbackState == Player.STATE_ENDED) {
                    player.pause()
                    _endOfSurahEnabled.value = false
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.let { updateStateFromMediaItem(it) }
                val p = controller
                if (p != null && _endOfSurahEnabled.value && p.currentMediaItemIndex == p.mediaItemCount - 1 && p.playbackState == Player.STATE_ENDED) {
                    p.pause()
                    _endOfSurahEnabled.value = false
                }
            }
        })
    }

    private fun updateStateFromMediaItem(mediaItem: MediaItem) {
        val mediaId = mediaItem.mediaId
        if (mediaId.contains("_")) {
            val parts = mediaId.split("_")
            val surah = parts.getOrNull(0)?.toIntOrNull() ?: 1
            val ayah = parts.getOrNull(1)?.toIntOrNull() ?: 1
            _currentSurahId.value = surah
            _currentAyahId.value = ayah
        }
    }

    fun playSurah(surah: QuranHelper.Surah, startAyah: Int = 1) {
        val player = controller ?: return
        val reciter = _selectedReciter.value

        scope.launch {
            player.stop()
            player.clearMediaItems()

            val mediaItems = mutableListOf<MediaItem>()
            for (ayahNum in 1..surah.totalVerses) {
                val surahStr = surah.id.toString().padStart(3, '0')
                val ayahStr = ayahNum.toString().padStart(3, '0')
                val url = "https://everyayah.com/data/${reciter.folder}/$surahStr$ayahStr.mp3"
                mediaItems.add(
                    MediaItem.Builder()
                        .setUri(url)
                        .setMediaId("${surah.id}_$ayahNum")
                        .build()
                )
            }

            player.setMediaItems(mediaItems)
            player.prepare()
            if (startAyah in 1..surah.totalVerses) {
                player.seekTo(startAyah - 1, 0L)
            }
            player.play()
            _isPlaying.value = true
        }
    }

    fun jumpToAyah(ayahId: Int) {
        val player = controller ?: return
        val currentSurah = _currentSurahId.value
        
        // If we are currently playing the correct surah, seek directly in the playlist
        if (player.mediaItemCount > 0 && player.currentMediaItem?.mediaId?.startsWith("${currentSurah}_") == true) {
            val index = ayahId - 1
            if (index in 0 until player.mediaItemCount) {
                player.seekTo(index, 0L)
                player.play()
            }
        }
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.mediaItemCount > 0) {
                player.play()
            }
        }
    }

    fun setReciter(reciter: Reciter, surah: QuranHelper.Surah) {
        _selectedReciter.value = reciter
        // Restart current Surah from the current Ayah to apply new reciter voice url
        val currentAyah = _currentAyahId.value
        playSurah(surah, currentAyah)
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        controller?.setPlaybackSpeed(speed)
    }

    fun setEndOfSurahEnabled(enabled: Boolean) {
        _endOfSurahEnabled.value = enabled
        if (enabled) {
            sleepTimerJob?.cancel()
            _sleepTimerRemaining.value = 0L
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _endOfSurahEnabled.value = false
        if (minutes <= 0) {
            _sleepTimerRemaining.value = 0L
            return
        }

        val totalSeconds = minutes * 60L
        _sleepTimerRemaining.value = totalSeconds

        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _sleepTimerRemaining.value = remaining
            }
            // Timer expired, stop playback
            controller?.pause()
            _sleepTimerRemaining.value = 0L
        }
    }

    fun toggleRepeat() {
        val player = controller ?: return
        val currentMode = _repeatMode.value
        _repeatMode.value = !currentMode
        player.repeatMode = if (!currentMode) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
    }

    fun stopAndClear() {
        controller?.stop()
        controller?.clearMediaItems()
        _currentSurahId.value = -1
        _isPlaying.value = false
    }

    fun release() {
        sleepTimerJob?.cancel()
        scope.cancel()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
