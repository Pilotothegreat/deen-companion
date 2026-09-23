package com.pilotothegreat.deencompanion.ui.reliability

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pilotothegreat.deencompanion.ui.theme.Spacing
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.alarms.Notifications as AppNotifications
import com.pilotothegreat.deencompanion.core.device.OemGuidance
import com.pilotothegreat.deencompanion.core.device.ReliabilityCheck
import com.pilotothegreat.deencompanion.ui.common.SystemIntents
import com.pilotothegreat.deencompanion.ui.common.startSafely
import com.pilotothegreat.deencompanion.ui.components.SectionHeader
import com.pilotothegreat.deencompanion.ui.navigation.LocalBottomBarPadding

/** One check, its current state, and the one place the reader can do something about it. */
data class ReliabilityItem(val check: ReliabilityCheck, val satisfied: Boolean)

/**
 * Why alarms do not arrive, and what to do about each reason.
 *
 * This is the single biggest thing the app can do for "works on more phones". The prayer engine was
 * never the problem; Doze, a revoked notification permission and above all the manufacturers' own
 * battery managers are, and none of them produces an error the app can see. Every check is shown
 * with its state, so the reader is pointed at the one thing that is actually wrong.
 */
@Composable
fun ReliabilityScreen(canScheduleExact: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val items = remember(context, canScheduleExact) { checks(context, canScheduleExact) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reliability)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.go_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.large, end = Spacing.large, bottom = Spacing.xxlarge + LocalBottomBarPadding.current),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            item { SectionHeader(stringResource(R.string.reliability_intro), Modifier.padding(start = 0.dp)) }
            itemsIndexed(items, key = { _, item -> item.check }) { index, item ->
                SegmentedListItem(
                    onClick = { context.startSafely(intentFor(context, item.check)) },
                    shapes = ListItemDefaults.segmentedShapes(index, items.size),
                    leadingContent = {
                        Icon(
                            if (item.satisfied) Icons.Rounded.CheckCircle else item.check.icon,
                            contentDescription = null,
                            tint = if (item.satisfied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                    },
                    supportingContent = { Text(stringResource(item.check.body)) },
                ) { Text(stringResource(item.check.title)) }
            }
        }
    }
}

/** True when something is actually wrong, which is the only time Today shows a card about it. */
fun hasReliabilityProblem(context: Context, canScheduleExact: Boolean): Boolean =
    checks(context, canScheduleExact).any { !it.satisfied }

private fun checks(context: Context, canScheduleExact: Boolean): List<ReliabilityItem> = listOf(
    ReliabilityItem(ReliabilityCheck.NOTIFICATION_PERMISSION, AppNotifications.canPost(context)),
    ReliabilityItem(ReliabilityCheck.EXACT_ALARMS, canScheduleExact),
    ReliabilityItem(ReliabilityCheck.BATTERY_OPTIMISATION, isExemptFromBatteryOptimisation(context)),
    ReliabilityItem(ReliabilityCheck.CHANNEL_BLOCKED, !AppNotifications.isAdhanChannelBlocked(context)),
    // Nothing reports this, so it is never marked satisfied on a phone known to need the step.
    ReliabilityItem(ReliabilityCheck.MANUFACTURER_RESTRICTION, !OemGuidance.needsExtraStep(Build.MANUFACTURER)),
)

private fun isExemptFromBatteryOptimisation(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) ?: true

private fun intentFor(context: Context, check: ReliabilityCheck) = when (check) {
    ReliabilityCheck.NOTIFICATION_PERMISSION -> SystemIntents.appNotifications(context)
    ReliabilityCheck.EXACT_ALARMS -> SystemIntents.exactAlarms(context)
    ReliabilityCheck.BATTERY_OPTIMISATION -> SystemIntents.batteryOptimisation()
    ReliabilityCheck.CHANNEL_BLOCKED -> SystemIntents.channel(context, AppNotifications.CHANNEL_ADHAN)
    ReliabilityCheck.MANUFACTURER_RESTRICTION -> SystemIntents.appDetails(context)
}

private val ReliabilityCheck.icon: ImageVector
    get() = when (this) {
        ReliabilityCheck.NOTIFICATION_PERMISSION, ReliabilityCheck.CHANNEL_BLOCKED -> Icons.Rounded.Notifications
        ReliabilityCheck.EXACT_ALARMS -> Icons.Rounded.Alarm
        ReliabilityCheck.BATTERY_OPTIMISATION -> Icons.Rounded.BatteryAlert
        ReliabilityCheck.MANUFACTURER_RESTRICTION -> Icons.Rounded.PhoneAndroid
    }

private val ReliabilityCheck.title: Int
    get() = when (this) {
        ReliabilityCheck.NOTIFICATION_PERMISSION -> R.string.reliability_notifications
        ReliabilityCheck.EXACT_ALARMS -> R.string.reliability_exact
        ReliabilityCheck.BATTERY_OPTIMISATION -> R.string.reliability_battery
        ReliabilityCheck.CHANNEL_BLOCKED -> R.string.reliability_channel
        ReliabilityCheck.MANUFACTURER_RESTRICTION -> R.string.reliability_manufacturer
    }

private val ReliabilityCheck.body: Int
    get() = when (this) {
        ReliabilityCheck.NOTIFICATION_PERMISSION -> R.string.reliability_notifications_body
        ReliabilityCheck.EXACT_ALARMS -> R.string.reliability_exact_body
        ReliabilityCheck.BATTERY_OPTIMISATION -> R.string.reliability_battery_body
        ReliabilityCheck.CHANNEL_BLOCKED -> R.string.reliability_channel_body
        ReliabilityCheck.MANUFACTURER_RESTRICTION -> oemBody(OemGuidance.forManufacturer(Build.MANUFACTURER))
    }

private fun oemBody(guidance: OemGuidance) = when (guidance) {
    OemGuidance.XIAOMI -> R.string.reliability_oem_xiaomi
    OemGuidance.HUAWEI -> R.string.reliability_oem_huawei
    OemGuidance.OPPO -> R.string.reliability_oem_oppo
    OemGuidance.VIVO -> R.string.reliability_oem_vivo
    OemGuidance.SAMSUNG -> R.string.reliability_oem_samsung
    OemGuidance.OTHER -> R.string.reliability_oem_other
}
