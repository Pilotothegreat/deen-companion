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
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.data.update.InstallState

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
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.whats_new_in, version), style = MaterialTheme.typography.headlineSmallEmphasized)
            Text(notes, style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.done)) }
        }
    }
}
