package com.pilotothegreat.deencompanion.ui.common

import android.Manifest
import android.app.AlarmManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.Brightness5
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import java.util.Locale

val Prayer.icon: ImageVector
    get() = when (this) {
        Prayer.FAJR -> Icons.Rounded.WbTwilight
        Prayer.SUNRISE -> Icons.Rounded.WbSunny
        Prayer.DHUHR -> Icons.Rounded.LightMode
        Prayer.ASR -> Icons.Rounded.Brightness5
        Prayer.MAGHRIB -> Icons.Rounded.Brightness4
        Prayer.ISHA -> Icons.Rounded.Bedtime
    }

/** Locale of the current UI (the per-app language when one is set). */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

val Locale.isArabic: Boolean get() = language == "ar"

val LOCATION_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)

fun hasLocationPermission(context: Context): Boolean = LOCATION_PERMISSIONS.any {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

fun canScheduleExactAlarms(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true

/** Intents into system settings pages, each safe to start from any context. */
object SystemIntents {
    fun appNotifications(context: Context) = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun channel(context: Context, channelId: String) = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)

    fun exactAlarms(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())
        } else {
            appDetails(context)
        }

    fun appDetails(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())

    /** The system list where the app can be exempted from battery optimisation. */
    fun batteryOptimisation(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    /** Where Do Not Disturb access is granted, which silence-during-prayer needs. */
    fun doNotDisturbAccess(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    fun url(url: String) = Intent(Intent.ACTION_VIEW, url.toUri())

    fun shareText(text: String): Intent =
        Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null)
}

/** Starts [intent], ignoring devices without a handler for it. */
fun Context.startSafely(intent: Intent) {
    runCatching { startActivity(intent) }
}

fun Context.copyToClipboard(label: String, text: String) {
    getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(label, text))
}

/** Android 13+ shows its own confirmation when something is copied. */
val showsOwnCopyConfirmation: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
