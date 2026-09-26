package com.pilotothegreat.deencompanion.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pilotothegreat.deencompanion.ui.theme.Spacing
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.ui.common.SystemIntents
import com.pilotothegreat.deencompanion.ui.common.startSafely

private val LOCATION_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)

private data class Step(
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val body: Int,
    @StringRes val action: Int?,
)

private val STEPS = listOf(
    Step(Icons.Rounded.Language, R.string.onboarding_welcome_title, R.string.onboarding_welcome_body, null),
    Step(Icons.Rounded.LocationOn, R.string.onboarding_location_title, R.string.onboarding_location_body, R.string.allow),
    Step(Icons.Rounded.Alarm, R.string.onboarding_alerts_title, R.string.onboarding_alerts_body, R.string.allow),
    Step(Icons.Rounded.AutoAwesome, R.string.onboarding_smart_title, R.string.onboarding_smart_body, null),
)

/**
 * First run, in four steps that can all be skipped.
 *
 * Each one says why before it asks, and the weather step discloses the only thing this app ever
 * sends anywhere — an approximate location — on the screen where it is turned on, not buried in a
 * policy. Skipping every step leaves a working app: prayer times fall back to a chosen city, and
 * nothing here is a gate.
 */
@Composable
fun OnboardingScreen(onOpenLocation: () -> Unit, onFinish: () -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = STEPS[index]
    val context = LocalContext.current
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    Scaffold { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.xxlarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.large, Alignment.CenterVertically),
        ) {
            Icon(step.icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(step.title), style = MaterialTheme.typography.headlineMediumEmphasized, textAlign = TextAlign.Center)
            Text(
                stringResource(step.body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            step.action?.let { label ->
                Button(onClick = {
                    when (index) {
                        1 -> locationPermission.launch(LOCATION_PERMISSIONS)
                        2 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            context.startSafely(SystemIntents.appNotifications(context))
                        }
                    }
                }) { Text(stringResource(label)) }
            }
            if (index == 1) {
                TextButton(onClick = onOpenLocation) { Text(stringResource(R.string.choose_city)) }
            }
            if (index == 2) {
                TextButton(onClick = { context.startSafely(SystemIntents.exactAlarms(context)) }) {
                    Text(stringResource(R.string.exact_alarms))
                }
            }
            Spacer(Modifier.size(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onFinish) { Text(stringResource(R.string.onboarding_skip)) }
                Text(
                    "${index + 1} / ${STEPS.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { if (index == STEPS.lastIndex) onFinish() else index++ }) {
                    Text(stringResource(if (index == STEPS.lastIndex) R.string.onboarding_start else R.string.onboarding_next))
                }
            }
        }
    }
}
