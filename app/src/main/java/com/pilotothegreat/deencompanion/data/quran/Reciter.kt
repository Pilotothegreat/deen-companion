package com.pilotothegreat.deencompanion.data.quran

import androidx.annotation.StringRes
import com.pilotothegreat.deencompanion.R

/**
 * Ayah-by-ayah recitations streamed from everyayah.com. Names are persisted in settings.
 *
 * Every folder here was checked against everyayah.com's own list of recitations, and each one asked
 * for the first, last and a handful of middle ayahs before it was added: a reciter whose folder is
 * spelled a shade wrong is not a missing feature, it is a reader who plays nothing.
 *
 * All of them are murattal, the reading a listener follows along with. The lower bitrates are the
 * only ones the site holds for that reciter, not a choice made here.
 */
enum class Reciter(private val folder: String, @get:StringRes val label: Int) {
    MISHARY("Alafasy_128kbps", R.string.reciter_mishary),
    HUSARY("Husary_128kbps", R.string.reciter_husary),
    ABDUL_BASIT("Abdul_Basit_Murattal_64kbps", R.string.reciter_abdul_basit),
    MINSHAWI("Minshawy_Murattal_128kbps", R.string.reciter_minshawi),
    SUDAIS("Abdurrahmaan_As-Sudais_64kbps", R.string.reciter_sudais),
    SHURAIM("Saood_ash-Shuraym_128kbps", R.string.reciter_shuraim),
    GHAMDI("Ghamadi_40kbps", R.string.reciter_ghamdi),
    HUDHAIFY("Hudhaify_128kbps", R.string.reciter_hudhaify),
    MUAIQLY("Maher_AlMuaiqly_64kbps", R.string.reciter_muaiqly),
    SHATRI("Abu_Bakr_Ash-Shaatree_128kbps", R.string.reciter_shatri),
    AYYUB("Muhammad_Ayyoub_128kbps", R.string.reciter_ayyub),
    BASFAR("Abdullah_Basfar_64kbps", R.string.reciter_basfar),
    DOSARI("Yasser_Ad-Dussary_128kbps", R.string.reciter_dosari),
    QATAMI("Nasser_Alqatami_128kbps", R.string.reciter_qatami),
    JUHANY("Abdullaah_3awwaad_Al-Juhaynee_128kbps", R.string.reciter_juhany),
    ALI_JABER("Ali_Jaber_64kbps", R.string.reciter_ali_jaber),
    FARES_ABBAD("Fares_Abbad_64kbps", R.string.reciter_fares_abbad),
    JIBREEL("Muhammad_Jibreel_128kbps", R.string.reciter_jibreel),
    HANI_RIFAI("Hani_Rifai_64kbps", R.string.reciter_hani_rifai),
    TABLAWI("Mohammad_al_Tablaway_128kbps", R.string.reciter_tablawi);

    fun audioUrl(surah: Int, ayah: Int): String =
        "https://everyayah.com/data/$folder/%03d%03d.mp3".format(surah, ayah)
}
