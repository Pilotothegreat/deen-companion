package com.pilotothegreat.deencompanion.ui.athkar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.pilotothegreat.deencompanion.ui.theme.Spacing
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.tasbih.Dhikr
import com.pilotothegreat.deencompanion.core.tasbih.TasbihEngine
import com.pilotothegreat.deencompanion.core.tasbih.TasbihState
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.labelRes
import com.pilotothegreat.deencompanion.ui.components.RollingNumber
import com.pilotothegreat.deencompanion.ui.theme.animatedPolygonShape
import java.util.Locale

/** Free tasbih counter, shared with the home-screen widget. */
@Composable
fun TasbihCard(
    state: TasbihState,
    locale: Locale,
    onTap: () -> Unit,
    onReset: () -> Unit,
    onTargetChange: (Int) -> Unit,
    onDhikrChange: (Dhikr) -> Unit,
    modifier: Modifier = Modifier,
) {
    val target = TasbihEngine.roundTarget(state)
    val progress by animateFloatAsState(
        targetValue = state.count / target.toFloat(),
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "tasbihProgress",
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val buttonShape = animatedPolygonShape(
        target = if (pressed) MaterialShapes.Cookie12Sided else MaterialShapes.Circle,
        spec = MaterialTheme.motionScheme.fastSpatialSpec(),
    )
    val tapDescription = stringResource(R.string.cd_tasbih_button)
    val countState = stringResource(R.string.athkar_progress, Formatters.number(state.count, locale), Formatters.number(target, locale))
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.xlarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.large),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tasbih_counter), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onReset) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = stringResource(R.string.reset_tasbih))
                }
            }
            Box {
                AssistChip(
                    onClick = { menuOpen = true },
                    label = { Text(stringResource(state.dhikr.labelRes)) },
                    trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null) },
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    Dhikr.entries.forEach { dhikr ->
                        DropdownMenuItem(
                            text = { Text(stringResource(dhikr.labelRes)) },
                            onClick = {
                                onDhikrChange(dhikr)
                                menuOpen = false
                            },
                        )
                    }
                }
            }
            Box(Modifier.size(208.dp), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .size(164.dp)
                        .clip(buttonShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(interactionSource = interaction, indication = ripple(), onClick = onTap)
                        .semantics {
                            contentDescription = tapDescription
                            stateDescription = countState
                            role = Role.Button
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RollingNumber(
                            state.count,
                            locale,
                            style = MaterialTheme.typography.displayMediumEmphasized,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            stringResource(R.string.tasbih_of_target, Formatters.number(target, locale)),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                TasbihEngine.targets.forEach { value ->
                    FilterChip(
                        selected = state.target == value,
                        onClick = { onTargetChange(value) },
                        label = { Text(Formatters.number(value, locale)) },
                    )
                }
            }
        }
    }
}
