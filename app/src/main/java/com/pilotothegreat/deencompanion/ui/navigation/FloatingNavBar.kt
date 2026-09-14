package com.pilotothegreat.deencompanion.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.currentLocale
import com.pilotothegreat.deencompanion.ui.common.rememberHaptics
import com.pilotothegreat.deencompanion.ui.theme.LocalAccessibility
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Extra bottom padding for tab lists and snackbars so they clear the floating bar. */
val LocalBottomBarPadding = compositionLocalOf { 0.dp }

/** The bar's height plus the gap beneath it. */
val FloatingBarClearance: Dp = 96.dp

/**
 * Samsung-style floating pill bar: the tabs on a rounded surface that hides while scrolling down
 * and comes back when scrolling up. The selected tab widens into a tinted pill with a spring.
 */
@Composable
fun FloatingNavBar(
    current: TopLevel,
    onSelect: (TopLevel) -> Unit,
    scrollBehavior: FloatingToolbarScrollBehavior,
    /** A count to show on a tab, or null. Zero is treated as nothing, not as a badge saying "0". */
    badges: Map<TopLevel, Int?> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        scrollBehavior = scrollBehavior,
    ) {
        TopLevel.entries.forEach { tab ->
            NavPill(tab, selected = tab == current, badge = badges[tab]?.takeIf { it > 0 }) {
                if (tab != current) haptics.tick()
                onSelect(tab)
            }
        }
    }
}

@Composable
private fun NavPill(tab: TopLevel, selected: Boolean, badge: Int?, onClick: () -> Unit) {
    val motion = MaterialTheme.motionScheme
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (selected) colors.secondaryContainer else Color.Transparent,
        motion.defaultEffectsSpec(),
        label = "pillContainer",
    )
    val content by animateColorAsState(
        if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
        motion.defaultEffectsSpec(),
        label = "pillContent",
    )
    val accessibility = LocalAccessibility.current
    val width by animateDpAsState(if (selected) 88.dp else 68.dp, motion.fastSpatialSpec(), label = "pillWidth")
    Column(
        modifier = Modifier
            .width(width)
            // A navigation target below 48 dp is a target people miss; large targets raise the floor
            // further for anyone whose aim is not steady.
            .heightIn(min = if (accessibility.largeTouchTargets) 64.dp else 48.dp)
            .clip(CircleShape)
            .background(container)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // The badge says how much is waiting without making anyone open the tab to find out.
        BadgedBox(
            badge = {
                if (badge != null) {
                    Badge { Text(Formatters.number(badge, currentLocale())) }
                }
            },
        ) {
            Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null, tint = content)
        }
        Text(
            stringResource(tab.label),
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
