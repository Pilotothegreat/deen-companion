package com.pilotothegreat.deencompanion.core.quran

/** How recitation repeats, for memorisation. */
enum class RepeatMode {
    /** Play through and stop at the end of the surah. */
    OFF,

    /** Repeat the current ayah. */
    AYAH,

    /** Repeat from the ayah playback started at to the current one. */
    RANGE,

    /** Repeat the whole surah. */
    SURAH,
}
