package com.pilotothegreat.deencompanion.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable

/**
 * A label for a control that is only an icon.
 *
 * The app had content descriptions everywhere, which tells a screen reader what a button does and
 * tells a sighted reader nothing at all — they get a shape and a guess. A long press now says it
 * out loud, which is the same word the screen reader was already using, so there is one label per
 * control rather than two that can drift apart.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Labelled(label: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        Box { content() }
    }
}
