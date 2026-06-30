package com.pilotothegreat.deencompanion.ui.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import com.pilotothegreat.deencompanion.services.QuranPlaybackManager
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QuranAudioPlayer(
    surah: QuranHelper.Surah,
    modifier: Modifier = Modifier
) {
    val playbackManager: QuranPlaybackManager = koinInject()
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val isPlaying by playbackManager.isPlaying.collectAsState()
    val currentSurahId by playbackManager.currentSurahId.collectAsState()
    val currentAyahId by playbackManager.currentAyahId.collectAsState()
    val selectedReciter by playbackManager.selectedReciter.collectAsState()
    val isBuffering by playbackManager.isBuffering.collectAsState()
    val lang by appPreferenceRepo.appLanguage.collectAsState(initial = "en")

    var showReciterMenu by remember { mutableStateOf(false) }
    val playScale by animateFloatAsState(if (isPlaying) 1.05f else 1f)

    AnimatedVisibility(
        visible = currentSurahId == surah.id,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 10.dp,
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding()
                .widthIn(max = 440.dp)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Info & Control Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Play/Pause button
                    Box(contentAlignment = Alignment.Center) {
                        FilledIconButton(
                            onClick = { playbackManager.togglePlayPause() },
                            modifier = Modifier
                                .size(44.dp)
                                .scale(playScale),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isBuffering) {
                                CircularWavyProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.cd_play_quran),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Center: Surah metadata + reciter selection
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .clickable { showReciterMenu = true },
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = if (lang == "ar") surah.name else surah.transliteration,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                                modifier = Modifier.size(14.dp)
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

                    // Right: Prev/Next & Dismiss Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (currentAyahId > 1) {
                                    playbackManager.jumpToAyah(currentAyahId - 1)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = stringResource(R.string.cd_prev_verse),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (currentAyahId < surah.verses.size) {
                                    playbackManager.jumpToAyah(currentAyahId + 1)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = stringResource(R.string.cd_next_verse),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))
                        
                        IconButton(
                            onClick = { playbackManager.stopAndClear() },
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Interactive M3 Progress Slider with Labels
                var isDragging by remember { mutableStateOf(false) }
                var sliderPosition by remember { mutableStateOf(currentAyahId.toFloat()) }
                LaunchedEffect(currentAyahId) {
                    if (!isDragging) {
                        sliderPosition = currentAyahId.toFloat()
                    }
                }
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = sliderPosition,
                        onValueChange = { 
                            isDragging = true
                            sliderPosition = it 
                        },
                        onValueChangeFinished = {
                            isDragging = false
                            playbackManager.jumpToAyah(sliderPosition.toInt().coerceIn(1, surah.verses.size))
                        },
                        valueRange = 1f..surah.verses.size.toFloat().coerceAtLeast(2f),
                        steps = (surah.verses.size - 2).coerceAtLeast(0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val progressLabel = if (lang == "ar") {
                            "آية ${toArabicNumerals(currentAyahId)} من ${toArabicNumerals(surah.verses.size)}"
                        } else {
                            stringResource(R.string.player_ayah_progress, currentAyahId, surah.verses.size)
                        }
                        Text(
                            text = progressLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
