package com.pilotothegreat.deencompanion.data.update

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.pilotothegreat.deencompanion.data.net.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/** What the download is doing, so the reader sees progress rather than a frozen row. */
sealed interface InstallState {
    data object Idle : InstallState
    data class Downloading(val fraction: Float) : InstallState
    data object Ready : InstallState
    data class Failed(val reason: Reason) : InstallState

    enum class Reason { DOWNLOAD, CHECKSUM, NOT_ALLOWED }
}

/**
 * Downloads and hands a release APK to the system installer.
 *
 * The download is verified against the SHA-256 GitHub publishes for the asset, and a file that does
 * not match is deleted rather than offered — an unverified APK is the one thing an app should never
 * ask someone to install. This exists only in the sideloaded build: the Play build has no business
 * installing packages and the permission is stripped from its manifest.
 */
class ApkInstaller(private val context: Context) {

    companion object {
        const val AUTHORITY_SUFFIX = ".updates"
        private const val DIRECTORY = "updates"
    }

    /** True when the system will let this app install packages at all. */
    fun canInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /**
     * @return the downloaded file, or null when it could not be fetched or did not match its hash.
     */
    suspend fun download(url: String, sha256: String, onProgress: (Float) -> Unit): File? =
        withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, DIRECTORY).apply { mkdirs() }
            directory.listFiles()?.forEach { it.delete() }
            val destination = File(directory, "bilal-update.apk")
            runCatching { Http.download(url, destination, expectedSha256 = sha256, onProgress = onProgress) }
                .onFailure {
                    Timber.w(it, "Update download failed")
                    destination.delete()
                }
                .getOrNull()
        }

    /** Opens the system installer. The reader still confirms; nothing installs silently. */
    fun install(file: File): Boolean = runCatching {
        val uri = FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
        true
    }.onFailure { Timber.w(it, "Could not open the installer") }.getOrDefault(false)
}
