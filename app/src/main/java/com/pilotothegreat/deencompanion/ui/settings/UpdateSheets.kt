package com.pilotothegreat.deencompanion.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pilotothegreat.deencompanion.ui.theme.Spacing
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.data.update.InstallState
import com.pilotothegreat.deencompanion.data.update.UpdateChecker
import com.pilotothegreat.deencompanion.ui.common.startSafely
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * What the update will change, before it is downloaded.
 *
 * Release notes used to appear only on the web page the app sent you to, which meant the choice
 * was "download something, find out afterwards". Nobody should have to agree to a download to
 * learn what it does.
 */
@Composable
fun UpdateDialog(
    version: String?,
    notes: String?,
    install: InstallState,
    canInstall: Boolean,
    onUpdate: () -> Unit,
    onOpenPage: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(version?.let { stringResource(R.string.update_available_version, it) } ?: stringResource(R.string.update_available_generic)) },
        text = {
            Column(
                Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                Text(notes?.takeIf { it.isNotBlank() } ?: stringResource(R.string.update_no_notes), style = MaterialTheme.typography.bodyMedium)
                when (install) {
                    is InstallState.Downloading -> {
                        LinearProgressIndicator(progress = { install.fraction }, modifier = Modifier.fillMaxWidth())
                        Text(stringResource(R.string.update_downloading), style = MaterialTheme.typography.labelMedium)
                    }
                    InstallState.Ready -> Text(stringResource(R.string.update_ready), style = MaterialTheme.typography.labelMedium)
                    is InstallState.Failed -> Text(
                        stringResource(
                            when (install.reason) {
                                InstallState.Reason.CHECKSUM -> R.string.update_checksum_failed
                                InstallState.Reason.NOT_ALLOWED -> R.string.update_not_allowed
                                InstallState.Reason.DOWNLOAD -> R.string.update_download_failed
                            },
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    InstallState.Idle -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(onClick = if (canInstall) onUpdate else onOpenPage) {
                Text(stringResource(if (canInstall) R.string.update_now else R.string.update_open_page))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.later)) } },
    )
}

/**
 * What changed, once, after an update. Shown on the first launch of a new version and never again,
 * because a changelog that keeps reappearing is an advert.
 */
@Composable
fun WhatsNewSheet(version: String, notes: String, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.xxlarge, end = Spacing.xxlarge, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Text(stringResource(R.string.whats_new_in, version), style = MaterialTheme.typography.headlineSmallEmphasized)
            Text(notes, style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.done)) }
        }
    }
}

/**
 * The update dialog with everything it needs to act: Play's in-place flow for a Play install, the
 * in-app download for a sideloaded one, and the web page only when neither can do it. Shared by the
 * version row in Settings and the pop-up the app shows on launch or from the notification.
 */
@Composable
fun UpdatePrompt(available: UpdateChecker.State.Available, viewModel: SettingsViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val install by viewModel.install.collectAsStateWithLifecycle()
    val playUpdate = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        viewModel.completePlayUpdate()
    }
    UpdateDialog(
        version = available.version,
        notes = available.notes,
        install = install,
        canInstall = viewModel.canInstallUpdates() && available.apkUrl != null,
        onUpdate = { viewModel.downloadAndInstall(available) },
        onOpenPage = {
            onDismiss()
            if (viewModel.isPlayInstall) {
                // Play updates in place; only if it has nothing on offer is anyone sent to the listing.
                viewModel.startPlayUpdate(playUpdate) { context.startSafely(viewModel.updateIntent()) }
            } else {
                context.startSafely(viewModel.updateIntent())
            }
        },
        onDismiss = onDismiss,
    )
}

