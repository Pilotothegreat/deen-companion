package com.pilotothegreat.deencompanion.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button

import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.alarms.Notifications
import com.pilotothegreat.deencompanion.ui.common.LOCATION_PERMISSIONS
import com.pilotothegreat.deencompanion.ui.common.SystemIntents
import com.pilotothegreat.deencompanion.ui.common.hasLocationPermission
import com.pilotothegreat.deencompanion.ui.common.startSafely
import com.pilotothegreat.deencompanion.ui.theme.Spacing

/**
 * First run: one sheet over Today, not a tour. Two permissions the app actually needs and the one
 * question about data, each a single line with a single action. Closing it, however it is closed,
 * leaves a working app — a chosen city stands in for location, and nothing is counted unless the
 * switch was turned on.
 */
@Composable
fun SetupSheet(onChooseCity: () -> Unit, onDone: (shareUsage: Boolean) -> Unit) {
    val context = LocalContext.current
    var notifications by remember { mutableStateOf(Notifications.canPost(context)) }
    var location by remember { mutableStateOf(hasLocationPermission(context)) }
    var shareUsage by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        notifications = Notifications.canPost(context)
        location = hasLocationPermission(context)
        onPauseOrDispose { }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        location = hasLocationPermission(context)
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifications = Notifications.canPost(context)
    }

    ModalBottomSheet(
        onDismissRequest = { onDone(shareUsage) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        SetupContent(
            notifications = notifications,
            location = location,
            shareUsage = shareUsage,
            onAllowNotifications = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    context.startSafely(SystemIntents.appNotifications(context))
                }
            },
            onAllowLocation = { locationPermission.launch(LOCATION_PERMISSIONS) },
            onChooseCity = onChooseCity,
            onShareUsage = { shareUsage = it },
            onDone = { onDone(shareUsage) },
        )
    }
}

/** The sheet's body, apart from the permission launchers, so it can be rendered in a test. */
@Composable
internal fun SetupContent(
    notifications: Boolean,
    location: Boolean,
    shareUsage: Boolean,
    onAllowNotifications: () -> Unit,
    onAllowLocation: () -> Unit,
    onChooseCity: () -> Unit,
    onShareUsage: (Boolean) -> Unit,
    onDone: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Spacing.xlarge).padding(bottom = Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.hair)) {
            Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineSmallEmphasized)
            Text(
                stringResource(R.string.setup_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SetupRow(Icons.Rounded.Notifications, stringResource(R.string.setup_notifications), stringResource(R.string.setup_notifications_desc)) {
            Allow(granted = notifications, onClick = onAllowNotifications)
        }
        SetupRow(
            Icons.Rounded.MyLocation,
            stringResource(R.string.setup_location),
            stringResource(R.string.setup_location_desc),
            below = {
                if (!location) {
                    TextButton(onClick = onChooseCity, contentPadding = PaddingValues(0.dp)) {
                        Text(stringResource(R.string.choose_city))
                    }
                }
            },
        ) {
            Allow(granted = location, onClick = onAllowLocation)
        }
        Row(
            Modifier.toggleable(value = shareUsage, role = Role.Switch, onValueChange = onShareUsage),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.large),
        ) {
            Icon(Icons.Rounded.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.setup_usage), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.setup_usage_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = shareUsage, onCheckedChange = null)
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.setup_done)) }
    }
}

@Composable
private fun SetupRow(
    icon: ImageVector,
    title: String,
    body: String,
    below: @Composable () -> Unit = {},
    action: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.large)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            below()
        }
        action()
    }
}

/** An Allow button until the permission is held, then a tick in its place. */
@Composable
private fun Allow(granted: Boolean, onClick: () -> Unit) {
    if (granted) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = stringResource(R.string.setup_allowed), tint = MaterialTheme.colorScheme.primary)
    } else {
        FilledTonalButton(onClick = onClick) { Text(stringResource(R.string.allow)) }
    }
}
