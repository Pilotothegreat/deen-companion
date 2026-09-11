package com.pilotothegreat.deencompanion.data.update

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.pilotothegreat.deencompanion.BuildConfig
import com.pilotothegreat.deencompanion.core.update.AppVersion
import com.pilotothegreat.deencompanion.data.net.Http
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import timber.log.Timber

/** Checks Google Play for Play installs and GitHub releases for sideloaded installs. */
class UpdateChecker(private val context: Context, private val settings: SettingsRepository) {

    sealed interface State {
        data object Idle : State
        data object Checking : State
        data object UpToDate : State
        /** [version] is the release tag for GitHub installs and null for Play installs. */
        data class Available(val version: String?) : State
        data object Failed : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    val lastCheckedAt = settings.lastUpdateCheckedAt

    val isPlayInstall: Boolean by lazy {
        val installer = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
        }.getOrNull()
        installer == PLAY_STORE_PACKAGE
    }

    /** Without [force], a GitHub result from the last 24 hours is reused. */
    suspend fun check(force: Boolean = false) {
        if (_state.value == State.Checking) return
        val now = System.currentTimeMillis()
        val (checkedAt, cachedTag) = settings.lastUpdateCheck()
        if (!force && !isPlayInstall && now - checkedAt < DAY_MILLIS) {
            _state.value = stateFor(cachedTag)
            return
        }
        _state.value = State.Checking
        _state.value = try {
            if (isPlayInstall) {
                val info = AppUpdateManagerFactory.create(context).requestAppUpdateInfo()
                settings.saveUpdateCheck(now, "")
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) State.Available(null) else State.UpToDate
            } else {
                val release = JSONObject(Http.getText(RELEASES_API, headers = mapOf("Accept" to "application/vnd.github+json")))
                val tag = release.getString("tag_name")
                settings.saveUpdateCheck(now, tag)
                stateFor(tag)
            }
        } catch (e: Exception) {
            Timber.w(e, "Update check failed")
            State.Failed
        }
    }

    /** Opens the Play listing or the GitHub releases page. */
    fun updateIntent(): Intent {
        val uri = if (isPlayInstall) "market://details?id=${context.packageName}" else RELEASES_PAGE
        return Intent(Intent.ACTION_VIEW, uri.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun stateFor(tag: String): State =
        if (tag.isNotBlank() && AppVersion.isNewer(BuildConfig.VERSION_NAME, tag)) State.Available(tag) else State.UpToDate

    private companion object {
        const val PLAY_STORE_PACKAGE = "com.android.vending"
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        const val RELEASES_API = "https://api.github.com/repos/Pilotothegreat/deen-companion/releases/latest"
        const val RELEASES_PAGE = "https://github.com/Pilotothegreat/deen-companion/releases/latest"
    }
}
