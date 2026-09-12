package com.pilotothegreat.deencompanion.core.quran

import androidx.annotation.StringRes
import com.pilotothegreat.deencompanion.R

/** How recitation repeats, for memorisation. */
enum class RepeatMode(@get:StringRes val label: Int) {
    /** Play through and stop at the end of the surah. */
    OFF(R.string.repeat_off),

    /** Repeat the current ayah. */
    AYAH(R.string.repeat_ayah),

    /** Repeat from the ayah playback started at to the current one. */
    RANGE(R.string.repeat_range),

    /** Repeat the whole surah. */
    SURAH(R.string.repeat_surah),
}
