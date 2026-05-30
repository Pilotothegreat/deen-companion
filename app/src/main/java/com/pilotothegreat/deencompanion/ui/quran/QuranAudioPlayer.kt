package com.pilotothegreat.deencompanion.ui.quran

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import org.koin.compose.koinInject
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.ui.navigation.Navigator
import kotlin.math.roundToInt

enum class Reciter(val labelRes: Int, val folder: String) {
    MISHARY(R.string.reciter_mishary, "Alafasy_128kbps"),
    HUSARY(R.string.reciter_husary, "Husary_128kbps"),
    ABDUL_BASIT(R.string.reciter_abdul_basit, "Abdul_Basit_Mujawwad_128kbps")
}

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

    var showReciterMenu by remember { mutableStateOf(false) }
    var selectedReciter by remember { mutableStateOf(Reciter.MISHARY) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    fun playAyah(ayahNum: Int) {
        lastPlayedAyah = ayahNum
        onAyahChanged(ayahNum)
        val surahStr = surah.id.toString().padStart(3, '0')
        val ayahStr = ayahNum.toString().padStart(3, '0')
        val url = "https://everyayah.com/data/${selectedReciter.folder}/$surahStr$ayahStr.mp3"
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

    // Auto-advance to next ayah using a separate state trigger
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Surah Title, Reciter Dropdown, Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = surah.transliteration,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    
                    // Reciter selector dropdown button
                    Box {
                        Text(
                            text = stringResource(selectedReciter.labelRes),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { showReciterMenu = true }
                                .padding(vertical = 2.dp)
                        )
                        DropdownMenu(
                            expanded = showReciterMenu,
                            onDismissRequest = { showReciterMenu = false }
                        ) {
                            Reciter.values().forEach { reciter ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(reciter.labelRes)) },
                                    onClick = {
                                        selectedReciter = reciter
                                        showReciterMenu = false
                                        // Replay current ayah with the new reciter if it was already playing
                                        if (isPlaying) {
                                            playAyah(currentAyah)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Previous
                    FilledTonalIconButton(
                        onClick = {
                            if (currentAyah > 1) {
                                val prev = currentAyah - 1
                                if (isPlaying) {
                                    playAyah(prev)
                                } else {
                                    lastPlayedAyah = prev
                                    onAyahChanged(prev)
                                }
                            }
                        }
                    ) { Icon(Icons.Default.SkipPrevious, contentDescription = "Prev Ayah") }
                    
                    // Play/Pause
                    FilledIconButton(
                        onClick = {
                            if (isPlaying) {
                                exoPlayer.pause()
                                isPlaying = false
                            } else {
                                playAyah(currentAyah)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause"
                        )
                    }
                    
                    // Next
                    FilledTonalIconButton(
                        onClick = {
                            if (currentAyah < totalAyahs) {
                                val next = currentAyah + 1
                                if (isPlaying) {
                                    playAyah(next)
                                } else {
                                    lastPlayedAyah = next
                                    onAyahChanged(next)
                                }
                            }
                        }
                    ) { Icon(Icons.Default.SkipNext, contentDescription = "Next Ayah") }
                }
            }
            
            // Row 2: Progress Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentAyah.toFloat(),
                    onValueChange = { targetVal ->
                        val target = targetVal.roundToInt()
                        if (target in 1..totalAyahs) {
                            if (isPlaying) {
                                playAyah(target)
                            } else {
                                lastPlayedAyah = target
                                onAyahChanged(target)
                            }
                        }
                    },
                    valueRange = 1f..totalAyahs.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ayah $currentAyah",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Ayah $totalAyahs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
