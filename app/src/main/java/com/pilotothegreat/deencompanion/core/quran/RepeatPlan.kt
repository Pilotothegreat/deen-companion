package com.pilotothegreat.deencompanion.core.quran

/** What the player does when an ayah finishes. */
sealed interface PlaybackStep {
    /** Let the player carry on to the next ayah. */
    data object Advance : PlaybackStep

    /** Go back to this position in the current surah (0-based). */
    data class SeekTo(val index: Int) : PlaybackStep

    /** Load the next surah and keep reciting. */
    data object NextSurah : PlaybackStep

    /** Nothing left to play. */
    data object Stop : PlaybackStep
}

/**
 * The repeat rules, kept free of the player so they can be reasoned about and tested. A count of
 * zero or less repeats without limit, which is what memorisation usually wants.
 */
object RepeatPlan {

    /**
     * @param finished 0-based index of the ayah that just ended.
     * @param lastIndex 0-based index of the last ayah of the surah.
     * @param repeatsDone how many times the current unit has already repeated.
     * @param range the ayahs RANGE repeats over, 0-based and inclusive.
     */
    fun onAyahFinished(
        finished: Int,
        lastIndex: Int,
        mode: RepeatMode,
        repeatCount: Int,
        repeatsDone: Int,
        range: IntRange,
        continueToNextSurah: Boolean,
    ): PlaybackStep {
        val more = repeatCount <= 0 || repeatsDone + 1 < repeatCount
        return when (mode) {
            RepeatMode.AYAH -> if (more) PlaybackStep.SeekTo(finished) else afterSurah(finished, lastIndex, continueToNextSurah)
            RepeatMode.RANGE -> when {
                finished != range.last -> afterSurah(finished, lastIndex, continueToNextSurah)
                more -> PlaybackStep.SeekTo(range.first)
                else -> afterSurah(finished, lastIndex, continueToNextSurah)
            }
            RepeatMode.SURAH -> when {
                finished < lastIndex -> PlaybackStep.Advance
                more -> PlaybackStep.SeekTo(0)
                else -> afterSurah(finished, lastIndex, continueToNextSurah)
            }
            RepeatMode.OFF -> afterSurah(finished, lastIndex, continueToNextSurah)
        }
    }

    /** True once the unit being repeated has come round again, so the counter should reset. */
    fun unitRestarted(step: PlaybackStep): Boolean = step is PlaybackStep.SeekTo

    private fun afterSurah(finished: Int, lastIndex: Int, continueToNextSurah: Boolean): PlaybackStep = when {
        finished < lastIndex -> PlaybackStep.Advance
        continueToNextSurah -> PlaybackStep.NextSurah
        else -> PlaybackStep.Stop
    }
}
