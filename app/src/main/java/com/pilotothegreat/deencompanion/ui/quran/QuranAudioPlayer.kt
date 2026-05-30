// FIXED: Auto-play next Surah when current Surah ends
package com.pilotothegreat.deencompanion.ui.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import org.koin.compose.koinInject
import com.pilotothegreat.deencompanion.ui.navigation.Navigator

@Composable
fun QuranAudioPlayer(
    surah: QuranHelper.Surah,
    currentAyah: Int,
    onAyahChanged: (Int) -> Unit,
    autoPlay: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navigator: Navigator = koinInject()
    val allSurahs = remember(context) { QuranHelper.getSurahs(context) }
    var isPlaying by remember { mutableStateOf(autoPlay) }
    var lastPlayedAyah by remember { mutableStateOf(currentAyah) }
    val totalAyahs = surah.totalVerses

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    fun playAyah(ayahNum: Int) {
        lastPlayedAyah = ayahNum
        onAyahChanged(ayahNum)
        val surahStr = surah.id.toString().padStart(3, '0')
        val ayahStr = ayahNum.toString().padStart(3, '0')
        val url = "https://everyayah.com/data/Alafasy_128kbps/$surahStr$ayahStr.mp3"
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
        exoPlayer.play()
        isPlaying = true
    }

    // Auto-play on launch if specified
    LaunchedEffect(Unit) {
        if (autoPlay) {
            playAyah(1)
        }
    }

    // Play ayah if parent changes selection (e.g. user clicked on a verse)
    LaunchedEffect(currentAyah) {
        if (currentAyah != lastPlayedAyah) {
            playAyah(currentAyah)
        }
    }

    // Auto-advance to next ayah using a separate state trigger to prevent unsafe Compose state mutation inside listener
    val playbackEnded = remember { mutableStateOf(false) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    playbackEnded.value = true
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(playbackEnded) {
        snapshotFlow { playbackEnded.value }.collect { ended ->
            if (ended) {
                playbackEnded.value = false
                if (currentAyah < totalAyahs) {
                    playAyah(currentAyah + 1)
                } else {
                    val nextSurahId = if (surah.id == 114) 1 else surah.id + 1
                    val nextSurah = allSurahs.firstOrNull { it.id == nextSurahId }
                    if (nextSurah != null) {
                        if (navigator.backStack.isNotEmpty()) {
                            navigator.backStack[navigator.backStack.lastIndex] =
                                com.pilotothegreat.deencompanion.ui.navigation.QuranReaderKey(nextSurahId, nextSurah.transliteration, autoPlay = true)
                        } else {
                            navigator.goTo(com.pilotothegreat.deencompanion.ui.navigation.QuranReaderKey(nextSurahId, nextSurah.transliteration, autoPlay = true))
                        }
                    } else {
                        isPlaying = false
                    }
                }
            }
        }
    }

    // M3 Bottom Player Card
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = surah.transliteration,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Ayah $currentAyah / $totalAyahs • Alafasy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                // Previous
                FilledTonalIconButton(onClick = {
                    if (currentAyah > 1) {
                        val prev = currentAyah - 1
                        if (isPlaying) {
                            playAyah(prev)
                        } else {
                            lastPlayedAyah = prev
                            onAyahChanged(prev)
                        }
                    }
                }) { Icon(Icons.Default.SkipPrevious, contentDescription = "Prev") }
                // Play/Pause
                FilledIconButton(onClick = {
                    if (isPlaying) {
                        exoPlayer.pause()
                        isPlaying = false
                    } else {
                        playAyah(currentAyah)
                    }
                }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play"
                    )
                }
                // Next
                FilledTonalIconButton(onClick = {
                    if (currentAyah < totalAyahs) {
                        val next = currentAyah + 1
                        if (isPlaying) {
                            playAyah(next)
                        } else {
                            lastPlayedAyah = next
                            onAyahChanged(next)
                        }
                    }
                }) { Icon(Icons.Default.SkipNext, contentDescription = "Next") }
            }
        }
    }
}

