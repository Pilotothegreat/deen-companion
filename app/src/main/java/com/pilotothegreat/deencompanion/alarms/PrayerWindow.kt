package com.pilotothegreat.deencompanion.alarms

/** Where a prayer stands between its adhan and its iqama. */
sealed interface PrayerStage {

    /** The adhan is sounding. */
    data object Adhan : PrayerStage

    /** Called, waiting for the iqama. [fraction] is how much of the gap has passed, 0f..1f. */
    data class Waiting(val fraction: Float) : PrayerStage

    /** The iqama has come. */
    data object Iqama : PrayerStage

    /** Nothing left to say; the notification goes. */
    data object Done : PrayerStage
}

/**
 * One notification per prayer, from the adhan to the iqama.
 *
 * Until now the adhan produced two notifications at once — one from the receiver and one from the
 * foreground service playing the audio — and the iqama added a third. They said the same thing
 * three times. This decides, for any instant, which single thing should be on screen; the parts
 * that touch Android are kept out of it so the answer can be tested rather than observed.
 */
object PrayerWindow {

    /** How long the iqama notice stays before it removes itself. */
    const val IQAMA_LINGER_MILLIS = 10 * 60 * 1000L

    /** Without an iqama to count towards, the adhan notice stands on its own for this long. */
    const val ADHAN_LINGER_MILLIS = 20 * 60 * 1000L

    /**
     * @param iqamaAt null when this prayer has no iqama — none is configured, or travel has
     *   suppressed it, in which case there is nothing to count towards.
     * @param adhanPlaying true while the audio is still sounding, which outranks the clock: the
     *   notification should offer Stop for as long as there is something to stop.
     */
    fun stageAt(now: Long, adhanAt: Long, iqamaAt: Long?, adhanPlaying: Boolean): PrayerStage {
        if (now < adhanAt) return PrayerStage.Done
        if (adhanPlaying) return PrayerStage.Adhan
        if (iqamaAt == null || iqamaAt <= adhanAt) {
            return if (now - adhanAt < ADHAN_LINGER_MILLIS) PrayerStage.Adhan else PrayerStage.Done
        }
        if (now >= iqamaAt) {
            return if (now - iqamaAt < IQAMA_LINGER_MILLIS) PrayerStage.Iqama else PrayerStage.Done
        }
        return PrayerStage.Waiting(fractionElapsed(now, adhanAt, iqamaAt))
    }

    /** How much of the wait is behind us, for the progress bar. */
    fun fractionElapsed(now: Long, adhanAt: Long, iqamaAt: Long): Float {
        val total = (iqamaAt - adhanAt).toFloat()
        if (total <= 0f) return 1f
        return ((now - adhanAt) / total).coerceIn(0f, 1f)
    }

    /**
     * When to redraw the progress bar.
     *
     * The countdown itself is a Chronometer, which ticks on its own with nothing running and costs
     * nothing. Only the bar needs the app to wake, so it wakes four times across the whole gap
     * rather than every minute — a frozen bar beside a live countdown would look broken, and a
     * per-minute alarm for a progress bar would not be worth anyone's battery.
     */
    fun checkpoints(adhanAt: Long, iqamaAt: Long?): List<Long> {
        if (iqamaAt == null || iqamaAt <= adhanAt) return emptyList()
        val gap = iqamaAt - adhanAt
        return listOf(0.25f, 0.5f, 0.75f).map { adhanAt + (gap * it).toLong() }
    }

    /** The most checkpoints any one prayer can have, which fixes the alarm id space. */
    const val MAX_CHECKPOINTS = 3
}
