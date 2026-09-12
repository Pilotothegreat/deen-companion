package com.pilotothegreat.deencompanion.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.data.backup.AutoBackups
import com.pilotothegreat.deencompanion.data.backup.BackupRepository
import com.pilotothegreat.deencompanion.data.backup.RestoreError
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pilotothegreat.deencompanion.alarms.PrayerAlarmScheduler
import com.pilotothegreat.deencompanion.core.prayer.AsrSchool
import com.pilotothegreat.deencompanion.core.prayer.CalculationMethod
import com.pilotothegreat.deencompanion.core.prayer.HighLatitudeMode
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.data.location.LocationRepository
import com.pilotothegreat.deencompanion.data.quran.QuranRepository
import com.pilotothegreat.deencompanion.data.quran.Reciter
import com.pilotothegreat.deencompanion.data.quran.TextSource
import com.pilotothegreat.deencompanion.data.quran.TranslationInfo
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.data.settings.AppSettings
import com.pilotothegreat.deencompanion.data.settings.ContrastMode
import com.pilotothegreat.deencompanion.data.settings.ReduceMotion
import com.pilotothegreat.deencompanion.data.settings.IqamaSetting
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import com.pilotothegreat.deencompanion.data.settings.ThemeMode
import com.pilotothegreat.deencompanion.data.update.ApkInstaller
import com.pilotothegreat.deencompanion.data.update.InstallState
import com.pilotothegreat.deencompanion.data.update.UpdateChecker
import com.pilotothegreat.deencompanion.playback.AudioCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One of the copies Bilal writes for itself each week. */
data class AutoBackup(val path: String, val savedAt: Long, val bytes: Long)

/** What the Quran text and its translation must be credited as, read from the assets themselves. */
data class QuranCredits(val text: TextSource, val translation: TranslationInfo, val edition: String)

class SettingsViewModel(
    private val context: Context,
    private val repository: SettingsRepository,
    private val location: LocationRepository,
    private val updates: UpdateChecker,
    private val scheduler: PrayerAlarmScheduler,
    quran: QuranRepository,
    private val backup: BackupRepository,
    private val installer: ApkInstaller,
) : ViewModel() {

    val quranCredits: StateFlow<QuranCredits?> = flow { quran.quran().let { emit(QuranCredits(it.textSource, it.translation, it.edition)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val settings: StateFlow<AppSettings?> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val updateState: StateFlow<UpdateChecker.State> = updates.state
    val lastUpdateCheck: StateFlow<Long> =
        updates.lastCheckedAt.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val isPlayInstall: Boolean get() = updates.isPlayInstall

    private val _audioCacheBytes = MutableStateFlow(0L)
    /** What the recitation cache is holding, refreshed whenever settings are shown. */
    val audioCacheBytes: StateFlow<Long> = _audioCacheBytes.asStateFlow()

    fun canScheduleExactAlarms(): Boolean = scheduler.canScheduleExact()

    fun setAudioCacheMb(mb: Int) = launch {
        repository.setAudioCacheMb(mb)
        AudioCache.setBudgetMb(mb)
    }

private val _backupMessage = MutableStateFlow<Int?>(null)

    /** A string resource to show once, then forget. */
    val backupMessage: StateFlow<Int?> = _backupMessage.asStateFlow()

    fun clearBackupMessage() {
        _backupMessage.value = null
    }

    fun exportBackup(destination: Uri) = launch {
        val message = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(destination)?.use { it.write(backup.export().toByteArray()) }
                    ?: error("no stream")
                R.string.backup_saved
            }.getOrDefault(R.string.backup_error_unreadable)
        }
        _backupMessage.value = message
    }

    /**
     * The copies Bilal wrote for itself, newest first, so a reinstall does not depend on someone
     * having remembered to export by hand.
     */
    val autoBackups: StateFlow<List<AutoBackup>> = flow {
        emit(
            withContext(Dispatchers.IO) {
                AutoBackups.list(context).map { AutoBackup(it.absolutePath, it.lastModified(), it.length()) }
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restoreAutoBackup(path: String) = launch {
        val message = withContext(Dispatchers.IO) {
            val json = runCatching { java.io.File(path).readText() }.getOrNull()
            when {
                json == null -> R.string.backup_error_unreadable
                else -> when (backup.restore(json)) {
                    null -> R.string.backup_restored
                    RestoreError.UNREADABLE -> R.string.backup_error_unreadable
                    RestoreError.WRONG_FILE -> R.string.backup_error_wrong_file
                    RestoreError.TOO_NEW -> R.string.backup_error_too_new
                }
            }
        }
        _backupMessage.value = message
    }

    fun importBackup(source: Uri) = launch {
        val message = withContext(Dispatchers.IO) {
            val json = runCatching {
                context.contentResolver.openInputStream(source)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            when {
                json == null -> R.string.backup_error_unreadable
                else -> when (backup.restore(json)) {
                    null -> R.string.backup_restored
                    RestoreError.UNREADABLE -> R.string.backup_error_unreadable
                    RestoreError.WRONG_FILE -> R.string.backup_error_wrong_file
                    RestoreError.TOO_NEW -> R.string.backup_error_too_new
                }
            }
        }
        _backupMessage.value = message
    }

    /** The sound one prayer's adhan plays: a picked URI, the phone's alarm sound, or silence. */
    fun setAdhanSound(prayer: Prayer, sound: String) = launch { repository.setAdhanSound(prayer, sound) }

    fun setPreReminderPrayers(prayers: Set<Prayer>) = launch { repository.setPreReminderPrayers(prayers) }

    fun resetEverything() = launch { backup.reset() }

    private val _install = MutableStateFlow<InstallState>(InstallState.Idle)

    /** Progress of a sideloaded update, which the reader watches rather than guesses at. */
    val install: StateFlow<InstallState> = _install.asStateFlow()

    /** True when the system will let this build install an update at all. */
    fun canInstallUpdates(): Boolean = !updates.isPlayInstall && installer.canInstall()

    /**
     * Downloads the release APK, verifies it against the checksum GitHub publishes, and only then
     * opens the system installer. A download that does not match is deleted without being offered.
     */
    fun downloadAndInstall(available: UpdateChecker.State.Available) = launch {
        val url = available.apkUrl
        val sha = available.apkSha256
        if (url == null || sha == null) {
            _install.value = InstallState.Failed(InstallState.Reason.DOWNLOAD)
            return@launch
        }
        if (!installer.canInstall()) {
            _install.value = InstallState.Failed(InstallState.Reason.NOT_ALLOWED)
            return@launch
        }
        _install.value = InstallState.Downloading(0f)
        val file = installer.download(url, sha) { _install.value = InstallState.Downloading(it) }
        _install.value = when {
            file == null -> InstallState.Failed(InstallState.Reason.CHECKSUM)
            installer.install(file) -> InstallState.Ready
            else -> InstallState.Failed(InstallState.Reason.NOT_ALLOWED)
        }
    }

    fun completeOnboarding() = launch { repository.setOnboardingCompleted(true) }

    fun markVersionSeen(versionCode: Int) = launch { repository.setLastSeenVersionCode(versionCode) }

        fun setSimpleMode(on: Boolean) = launch { repository.setSimpleMode(on) }

    fun setTextScale(scale: Float) = launch { repository.setTextScale(scale) }

    fun setReactToTheWorld(on: Boolean) = launch { repository.setReactToTheWorld(on) }

    fun setPreReminder(minutes: Int) = launch { repository.setPreReminderMinutes(minutes) }

    fun setSilenceMinutes(minutes: Int) = launch { repository.setSilenceMinutes(minutes) }

    /** Zero turns it off; otherwise it runs for this many days and then lapses on its own. */
    fun setCalamityDays(days: Int) = launch {
        repository.setCalamityUntil(if (days <= 0) 0L else System.currentTimeMillis() + days * 86_400_000L)
    }

    /** Home is wherever you are when you say so; travel is measured from it. */
    fun anchorHomeHere() = launch {
        val current = repository.current()
        if (!current.location.isDefault) repository.setHome(current.location.latitude, current.location.longitude)
    }

    fun clearAudioCache() = launch {
        withContext(Dispatchers.IO) { AudioCache.clear() }
        refreshAudioCacheSize()
    }

    fun refreshAudioCacheSize() = launch {
        _audioCacheBytes.value = withContext(Dispatchers.IO) { AudioCache.sizeBytes(context) }
    }

    fun setMethod(method: CalculationMethod) = launch { repository.setMethod(method) }
    fun setMethodAuto() = launch { repository.setMethodAuto() }
    fun setAsrSchool(school: AsrSchool) = launch { repository.setAsrSchool(school) }
    fun setHighLatitude(mode: HighLatitudeMode) = launch { repository.setHighLatitude(mode) }
    fun setAdjustment(prayer: Prayer, minutes: Int) = launch { repository.setAdjustment(prayer, minutes) }
    fun resetAdjustments() = launch { repository.resetAdjustments() }
    fun setIqama(prayer: Prayer, value: IqamaSetting) = launch { repository.setIqama(prayer, value) }

    fun setHijriAdjustment(days: Int) = launch { repository.setHijriAdjustment(days) }
    fun setNotificationsEnabled(enabled: Boolean) = launch { repository.setNotificationsEnabled(enabled) }
    fun setThemeMode(mode: ThemeMode) = launch { repository.setThemeMode(mode) }
    fun setQuranFontSize(size: Int) = launch { repository.setQuranFontSize(size) }
    fun setReciter(reciter: Reciter) = launch { repository.setReciter(reciter) }
    fun setAthkarReminders(enabled: Boolean) = launch { repository.setAthkarReminders(enabled) }

    fun setLanguage(tag: String) = launch {
        repository.setAppLanguage(tag)
        AppLanguage.apply(tag)
        location.relocalizeCity()
    }

    fun checkForUpdates() = launch { updates.check(force = true) }

    fun updateIntent(): Intent = updates.updateIntent()

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
