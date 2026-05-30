package com.pilotothegreat.deencompanion.ui.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.services.QuranPlaybackManager
import org.koin.compose.koinInject

@Composable
fun QuranAudioPlayer(
    surah: QuranHelper.Surah,
    modifier: Modifier = Modifier
) {
    val playbackManager: QuranPlaybackManager = koinInject()
    val isPlaying by playbackManager.isPlaying.collectAsState()
    val currentSurahId by playbackManager.currentSurahId.collectAsState()
    val currentAyahId by playbackManager.currentAyahId.collectAsState()
    val selectedReciter by playbackManager.selectedReciter.collectAsState()
    val playbackSpeed by playbackManager.playbackSpeed.collectAsState()
    val sleepTimerRemaining by playbackManager.sleepTimerRemaining.collectAsState()
    val repeatMode by playbackManager.repeatMode.collectAsState()
    val isBuffering by playbackManager.isBuffering.collectAsState()

    var showReciterMenu by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showTimerMenu by remember { mutableStateOf(false) }

    val playScale by animateFloatAsState(if (isPlaying) 1.05f else 1f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Surah Title, Reciter Selector, Speed, Timer Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = surah.transliteration,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    
                    // Reciter Trigger
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
                            Reciter.entries.forEach { reciter ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(reciter.labelRes)) },
                                    onClick = {
                                        playbackManager.setReciter(reciter, surah)
                                        showReciterMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Controls: Speed, Sleep Timer, Repeat
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Playback Speed
                    Box {
                        IconButton(onClick = { showSpeedMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Playback Speed",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text("${speed}x") },
                                    onClick = {
                                        playbackManager.setSpeed(speed)
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Sleep Timer
                    Box {
                        IconButton(onClick = { showTimerMenu = true }) {
                            BadgedBox(
                                badge = {
                                    if (sleepTimerRemaining > 0) {
                                        Badge {
                                            Text("${sleepTimerRemaining / 60}m")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Snooze,
                                    contentDescription = "Sleep Timer",
                                    tint = if (sleepTimerRemaining > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showTimerMenu,
                            onDismissRequest = { showTimerMenu = false }
                        ) {
                            listOf(
                                0 to stringResource(R.string.timer_off),
                                10 to stringResource(R.string.timer_10m),
                                15 to stringResource(R.string.timer_15m),
                                30 to stringResource(R.string.timer_30m),
                                45 to stringResource(R.string.timer_45m),
                                60 to stringResource(R.string.timer_60m)
                            ).forEach { (minutes, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        playbackManager.setSleepTimer(minutes)
                                        showTimerMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Repeat Mode Toggle
                    IconButton(onClick = { playbackManager.toggleRepeat() }) {
                        Icon(
                            imageVector = if (repeatMode) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Toggle Repeat",
                            tint = if (repeatMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Row 2: Playback Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev Ayah
                IconButton(
                    onClick = {
                        if (currentAyahId > 1) {
                            playbackManager.jumpToAyah(currentAyahId - 1)
                        }
                    },
                    enabled = currentSurahId == surah.id
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Verse",
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Play / Pause
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(64.dp)
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                    } else {
                        FilledIconButton(
                            onClick = {
                                if (currentSurahId == surah.id) {
                                    playbackManager.togglePlayPause()
                                } else {
                                    playbackManager.playSurah(surah, 1)
                                }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .scale(playScale)
                        ) {
                            Icon(
                                imageVector = if (isPlaying && currentSurahId == surah.id) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Next Ayah
                IconButton(
                    onClick = {
                        if (currentAyahId < surah.totalVerses) {
                            playbackManager.jumpToAyah(currentAyahId + 1)
                        }
                    },
                    enabled = currentSurahId == surah.id
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Verse",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Row 3: Progress
            if (currentSurahId == surah.id) {
                val progress = currentAyahId.toFloat() / surah.totalVerses.toFloat()
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        strokeCap = StrokeCap.Round
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.player_ayah_progress, currentAyahId, surah.totalVerses),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

private val QuranHelper.Surah.totalVerses: Int get() = this.totalVerses
