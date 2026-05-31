package com.pilotothegreat.deencompanion.ui.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
    val isBuffering by playbackManager.isBuffering.collectAsState()

    var showReciterMenu by remember { mutableStateOf(false) }

    val playScale by animateFloatAsState(if (isPlaying) 1.05f else 1f)

    AnimatedVisibility(
        visible = currentSurahId == surah.id,
        modifier = modifier
    ) {
        Surface(
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                val progress = if (surah.verses.size > 0) currentAyahId.toFloat() / surah.verses.size.toFloat() else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                    strokeCap = StrokeCap.Square
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showReciterMenu = true }
                    ) {
                        Text(
                            text = surah.transliteration,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(selectedReciter.labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (currentAyahId > 1) {
                                    playbackManager.jumpToAyah(currentAyahId - 1)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = stringResource(R.string.previous_verse),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        FilledIconButton(
                            onClick = {
                                playbackManager.togglePlayPause()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .scale(playScale),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isBuffering) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.play_pause)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (currentAyahId < surah.verses.size) {
                                    playbackManager.jumpToAyah(currentAyahId + 1)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = stringResource(R.string.next_verse),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}


