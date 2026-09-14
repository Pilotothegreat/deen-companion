package com.pilotothegreat.deencompanion.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Shrinks slightly while pressed and springs back on release. Pass the interaction source of the
 * clickable it decorates.
 */
@Suppress("ComposableModifierFactory", "ModifierFactoryExtensionFunction")
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.96f): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * True when decorative motion should be skipped: either the system has animations off, or the
 * reader asked for less motion, or simple mode is on. Resolved at the theme (see [Accessibility]),
 * so it is one answer for the whole app and it changes the moment the setting does.
 */
@Composable
fun rememberReducedMotion(): Boolean = LocalAccessibility.current.reduceMotion
