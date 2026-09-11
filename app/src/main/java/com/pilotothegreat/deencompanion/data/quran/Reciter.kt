package com.pilotothegreat.deencompanion.data.quran

import androidx.annotation.StringRes
import com.pilotothegreat.deencompanion.R

/** Ayah-by-ayah recitations streamed from everyayah.com. Names are persisted in settings. */
enum class Reciter(private val folder: String, @get:StringRes val label: Int) {
    MISHARY("Alafasy_128kbps", R.string.reciter_mishary),
    HUSARY("Husary_128kbps", R.string.reciter_husary),
    ABDUL_BASIT("Abdul_Basit_Murattal_64kbps", R.string.reciter_abdul_basit);

    fun audioUrl(surah: Int, ayah: Int): String =
        "https://everyayah.com/data/$folder/%03d%03d.mp3".format(surah, ayah)
}
