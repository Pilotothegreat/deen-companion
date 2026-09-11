package com.pilotothegreat.deencompanion.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.ui.common.copyToClipboard
import com.pilotothegreat.deencompanion.ui.common.showsOwnCopyConfirmation
import com.pilotothegreat.deencompanion.ui.common.startSafely

private data class BankApp(val name: String, val packageName: String)

/** Declared in the manifest's <queries> so installed apps can be offered. */
private val OMANI_BANK_APPS = listOf(
    BankApp("Bank Muscat", "com.ducont.muscatbank"),
    BankApp("bm Wallet", "app.banking.bankmuscat"),
    BankApp("NBO", "om.nbo.nbo"),
    BankApp("Bank Dhofar", "com.bankdhofar.mobilebanking"),
    BankApp("Sohar International", "com.BankSoharMB"),
    BankApp("Oman Arab Bank", "com.oab.mobile"),
    BankApp("Ahli Bank", "com.ahlibank"),
)

private const val DONATION_PHONE = "91904926"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SupportSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val installed = remember {
        OMANI_BANK_APPS.mapNotNull { app -> context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { app to it } }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.support_title), style = MaterialTheme.typography.headlineSmallEmphasized)
            }
            Text(stringResource(R.string.support_body), style = MaterialTheme.typography.bodyLarge)
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    stringResource(R.string.palestine_note),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Card(
                onClick = {
                    context.copyToClipboard("phone", DONATION_PHONE)
                    if (!showsOwnCopyConfirmation) Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                },
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.bank_transfer_phone_pay), style = MaterialTheme.typography.labelLarge)
                        Text(DONATION_PHONE, style = MaterialTheme.typography.headlineSmallEmphasized)
                        Text(stringResource(R.string.payment_tap_hint), style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.copy))
                }
            }
            if (installed.isNotEmpty()) {
                Text(stringResource(R.string.open_bank_app_label), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    installed.forEach { (app, intent) ->
                        AssistChip(
                            onClick = { context.startSafely(intent) },
                            label = { Text(app.name) },
                            trailingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) },
                        )
                    }
                }
            }
        }
    }
}
