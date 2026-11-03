package com.pilotothegreat.deencompanion.data.adhan

import android.content.ContentResolver
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.data.net.Http
import com.pilotothegreat.deencompanion.data.settings.SoundSettings
import java.io.File

/**
 * The adhans Bilal can call with, all freely licensed recordings from Wikimedia Commons.
 *
 * Two ship inside the app, so a phone that has never been online still hears a real adhan, and a real
 * Fajr adhan, at the right time: the Prophet's Mosque, and a Fajr adhan with «الصلاة خير من النوم».
 * The others are fetched once, from the original upload, when someone chooses them, and checked against
 * the checksum pinned here. Each is credited in ATTRIBUTION.md, as its licence asks.
 */
enum class Muezzin(
    val id: String,
    @get:StringRes val nameRes: Int,
    @get:StringRes val noteRes: Int,
    @get:RawRes val raw: Int?,
    val url: String?,
    val sha256: String?,
    val extension: String,
    /** Says «الصلاة خير من النوم», so it belongs to Fajr. */
    val fajr: Boolean = false,
    val credit: String,
) {
    MADINAH(
        "madinah", R.string.muezzin_madinah, R.string.muezzin_madinah_note, R.raw.adhan_madinah, null, null, "ogg",
        credit = "ejaz215, CC BY 3.0, via Wikimedia Commons",
    ),
    FAJR(
        "fajr", R.string.muezzin_fajr, R.string.muezzin_fajr_note, R.raw.adhan_fajr, null, null, "ogg", fajr = true,
        credit = "daSupremo, CC BY-SA 4.0, via Wikimedia Commons",
    ),
    GENTLE(
        "gentle", R.string.muezzin_gentle, R.string.muezzin_gentle_note, null,
        "https://upload.wikimedia.org/wikipedia/commons/b/b0/Beautiful_adhan.ogg",
        "35fe06b08fe80505c550c33fed8a783fa9901ddc81ac884958b4be048f5b2a79", "ogg",
        credit = "Adam-synagda, CC0, via Wikimedia Commons",
    ),
    WITH_DUA(
        "with-dua", R.string.muezzin_with_dua, R.string.muezzin_with_dua_note, null,
        "https://upload.wikimedia.org/wikipedia/commons/8/86/Azan.ogg",
        "2163ceafb8a05064a5ea5e1e0a7fac5fff958a21fcf830844d557d496b9b23c3", "ogg",
        credit = "Andrewler, CC BY-SA 4.0, via Wikimedia Commons",
    ),
    BRISK(
        "brisk", R.string.muezzin_brisk, R.string.muezzin_brisk_note, null,
        "https://upload.wikimedia.org/wikipedia/commons/7/7d/The_Adhan_-_Muslim_Call_to_Prayer_-_Aaqib_Azeez.mp3",
        "0faa59e2028bbb2f60a670b53ec89ffcf628f3c195b43fb41d9bdbc99000c1b3", "mp3",
        credit = "Aaqib Azeez (uploaded by Atcovi), CC BY-SA 4.0, via Wikimedia Commons",
    ),
    ;

    val bundled: Boolean get() = raw != null

    companion object {
        fun byId(id: String?): Muezzin? = entries.firstOrNull { it.id == id }

        /** What a prayer calls with until someone chooses otherwise. */
        fun defaultFor(fajr: Boolean): Muezzin = if (fajr) FAJR else MADINAH
    }
}

/** Where each adhan's audio is, and fetching the ones that are not bundled. */
object AdhanAudio {

    private fun folder(context: Context) = File(context.filesDir, "adhan")

    fun file(context: Context, muezzin: Muezzin): File = File(folder(context), "${muezzin.id}.${muezzin.extension}")

    /** True when [muezzin] can play with no connection: bundled, or already fetched. */
    fun isReady(context: Context, muezzin: Muezzin): Boolean = muezzin.bundled || file(context, muezzin).exists()

    suspend fun download(context: Context, muezzin: Muezzin, onProgress: (Float) -> Unit = {}) {
        val url = muezzin.url ?: return
        Http.download(url, file(context, muezzin), expectedSha256 = muezzin.sha256, timeoutMs = 45_000, onProgress = onProgress)
    }

    /**
     * The audio for a stored sound choice: a muezzin's file (the bundled adhan when a downloaded one
     * has gone missing, say after a restore onto a new phone), the phone's alarm sound, a sound the
     * reader picked, or null for silence.
     */
    fun resolve(context: Context, sound: String, fajr: Boolean): Uri? = when (sound) {
        SoundSettings.SILENT -> null
        SoundSettings.SYSTEM_SOUND -> RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        else -> when {
            Muezzin.byId(sound) != null -> uriFor(context, Muezzin.byId(sound)!!) ?: uriFor(context, Muezzin.defaultFor(fajr))
            // A sound picked from the phone.
            "://" in sound -> Uri.parse(sound)
            // An id this version does not know (an older build's, a hand-edited backup): still an adhan.
            else -> uriFor(context, Muezzin.defaultFor(fajr))
        }
    }

    /** A playable URI for [muezzin], or null when it is neither bundled nor fetched. */
    fun uriFor(context: Context, muezzin: Muezzin): Uri? = when {
        muezzin.raw != null -> Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.packageName)
            .appendPath(muezzin.raw.toString())
            .build()
        file(context, muezzin).exists() -> Uri.fromFile(file(context, muezzin))
        else -> null
    }
}
