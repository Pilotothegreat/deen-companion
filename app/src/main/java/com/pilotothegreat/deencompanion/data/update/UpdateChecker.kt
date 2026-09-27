package com.pilotothegreat.deencompanion.data.update

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.net.toUri
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.pilotothegreat.deencompanion.BuildConfig
import com.pilotothegreat.deencompanion.core.update.AppVersion
import com.pilotothegreat.deencompanion.core.update.UpdatePlan
import com.pilotothegreat.deencompanion.core.update.UpdateUrgency
import com.pilotothegreat.deencompanion.data.net.Http
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import timber.log.Timber
/**
 * Checks Google Play for Play installs and GitHub releases for sideloaded installs.
 *
 * It checks four times a day rather than once, remembers when the waiting version was first seen, and
 * hands [UpdatePlan] that date so the app can escalate from a quiet row in Settings to a line on Today
 * to a sheet on every launch. A release that fixes a missed adhan is worth more than one snackbar on
 * whichever day someone happened to open the app.
 */
class UpdateChecker(private val context: Context, private val settings: SettingsRepository) {

    sealed interface State {
        data object Idle : State
        data object Checking : State
        data object UpToDate : State
        /**
         * [version] is the release tag for GitHub installs and null for Play installs. [notes] is
         * the release's own changelog, shown before the update rather than after: people deserve to
         * know what a download is going to change before they agree to it.
         */
        data class Available(
            val version: String?,
            val notes: String? = null,
            val apkUrl: String? = null,
            val apkSha256: String? = null,
        ) : State
        data object Failed : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    /** Play's own numbers for the waiting update; both 0 for a GitHub release. */
    @Volatile private var priority = 0
    @Volatile private var stalenessDays = 0

    /** The version code Play has on offer, so a Play update can be told apart from the last one. */
    @Volatile var playVersionCode = 0
        private set

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

    /** Without [force], a GitHub result from the last few hours is reused. */
    suspend fun check(force: Boolean = false) {
        if (_state.value == State.Checking) return
        val now = System.currentTimeMillis()
        val (checkedAt, cachedTag) = settings.lastUpdateCheck()
        if (!force && !isPlayInstall && now - checkedAt < CHECK_INTERVAL_MILLIS) {
            _state.value = stateFor(cachedTag)
            rememberAvailability(now)
            return
        }
        _state.value = State.Checking
        _state.value = try {
            if (isPlayInstall) {
                val info = AppUpdateManagerFactory.create(context).requestAppUpdateInfo()
                settings.saveUpdateCheck(now, "")
                priority = runCatching { info.updatePriority() }.getOrDefault(0)
                stalenessDays = runCatching { info.clientVersionStalenessDays() ?: 0 }.getOrDefault(0)
                playVersionCode = runCatching { info.availableVersionCode() }.getOrDefault(0)
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) State.Available(null) else State.UpToDate
            } else {
                val release = JSONObject(Http.getText(RELEASES_API, headers = mapOf("Accept" to "application/vnd.github+json")))
                val tag = release.getString("tag_name")
                settings.saveUpdateCheck(now, tag)
                latestRelease = release
                stateFor(tag, release)
            }
        } catch (e: Exception) {
            Timber.w(e, "Update check failed")
            State.Failed
        }
        rememberAvailability(now)
    }

    /**
     * Keeps the date the waiting version was first seen, and clears it once there is nothing waiting.
     *
     * The date is the whole of the escalation: without it the app cannot tell an update published an
     * hour ago from one that has been ignored for a fortnight, and so has to treat them the same.
     */
    private suspend fun rememberAvailability(now: Long) {
        when (_state.value) {
            is State.Available -> if (settings.updateAvailableSince() == 0L) settings.setUpdateAvailableSince(now)
            State.UpToDate -> if (settings.updateAvailableSince() != 0L) {
                settings.setUpdateAvailableSince(0L)
                settings.setUpdatePromptedAt(0L)
            }
            else -> Unit
        }
    }

    /**
     * How hard to push what is waiting, and the record that it was pushed.
     *
     * Calling this counts as having told the reader, so a caller that asks must act on the answer.
     */
    suspend fun urgencyForLaunch(): UpdateUrgency {
        if (_state.value !is State.Available) return UpdateUrgency.QUIET
        val now = System.currentTimeMillis()
        val urgency = UpdatePlan.urgency(
            now = now,
            availableSince = settings.updateAvailableSince(),
            lastPromptedAt = settings.updatePromptedAt(),
            priority = priority,
            stalenessDays = stalenessDays,
        )
        if (urgency != UpdateUrgency.QUIET) settings.setUpdatePromptedAt(now)
        return urgency
    }

    /** True when Play should take the screen rather than download quietly behind the app. */
    val wantsImmediateFlow: Boolean get() = UpdatePlan.useImmediateFlow(priority, stalenessDays)

    /** Opens the Play listing or the GitHub releases page. */
    fun updateIntent(): Intent {
        val uri = if (isPlayInstall) "market://details?id=${context.packageName}" else RELEASES_PAGE
        return Intent(Intent.ACTION_VIEW, uri.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /**
     * Play's flexible update: it downloads in the background and the app keeps working, rather than
     * sending someone out to the store listing and hoping they come back. The dependency has been
     * in this app since the beginning and this flow was never once called.
     */
    suspend fun startPlayUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean {
        if (!isPlayInstall) return false
        return runCatching {
            val manager = AppUpdateManagerFactory.create(context)
            val info = manager.requestAppUpdateInfo()
            if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return false
            // An important update takes the screen and installs; a routine one downloads behind the
            // app and asks at the end. Falling back the other way is better than doing nothing.
            val type = when {
                wantsImmediateFlow && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                else -> return false
            }
            manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
            true
        }.onFailure { Timber.w(it, "Play update flow failed") }.getOrDefault(false)
    }

    /**
     * Finishes an update Play already downloaded, when the app comes back to the front.
     *
     * A flexible update sits downloaded until the app is restarted, and an app people leave open for
     * a prayer at a time may not be restarted for days. Returns true when there was one to finish.
     */
    suspend fun completeDownloadedPlayUpdate(): Boolean {
        if (!isPlayInstall) return false
        return runCatching {
            val manager = AppUpdateManagerFactory.create(context)
            val info = manager.requestAppUpdateInfo()
            if (info.installStatus() != InstallStatus.DOWNLOADED) return false
            manager.completeUpdate()
            true
        }.onFailure { Timber.w(it, "Could not finish the downloaded update") }.getOrDefault(false)
    }

    /** Installs an update Play has already downloaded, once the reader agrees. */
    fun completePlayUpdate() {
        runCatching { AppUpdateManagerFactory.create(context).completeUpdate() }
    }

    @Volatile private var latestRelease: JSONObject? = null

    private fun stateFor(tag: String, release: JSONObject? = latestRelease): State {
        if (tag.isBlank() || !AppVersion.isNewer(BuildConfig.VERSION_NAME, tag)) return State.UpToDate
        val apk = release?.optJSONArray("assets")?.let { assets ->
            (0 until assets.length())
                .map { assets.getJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk") }
        }
        return State.Available(
            version = tag,
            notes = release?.optString("body")?.takeIf { it.isNotBlank() },
            apkUrl = apk?.optString("browser_download_url"),
            // GitHub publishes a digest for release assets; without one the download is not offered
            // for install, only for opening in a browser.
            apkSha256 = apk?.optString("digest")?.removePrefix("sha256:")?.takeIf { it.length == 64 },
        )
    }

    private companion object {
        const val PLAY_STORE_PACKAGE = "com.android.vending"

        /** Four times a day. A day between checks meant a release could sit unseen for a day. */
        const val CHECK_INTERVAL_MILLIS = 6 * 60 * 60 * 1000L
        const val RELEASES_API = "https://api.github.com/repos/Pilotothegreat/deen-companion/releases/latest"
        const val RELEASES_PAGE = "https://github.com/Pilotothegreat/deen-companion/releases/latest"
    }
}
