package com.pilotothegreat.deencompanion.core.tasbih

/** [arabic] is the value persisted in settings; keep it stable. */
enum class Dhikr(val arabic: String) {
    SUBHAN_ALLAH("سبحان الله"),
    ALHAMDULILLAH("الحمد لله"),
    ALLAHU_AKBAR("الله أكبر"),
    LA_ILAHA_ILLALLAH("لا إله إلا الله");

    fun next(): Dhikr = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromStored(value: String?): Dhikr =
            entries.firstOrNull { it.arabic == value || it.name == value } ?: SUBHAN_ALLAH
    }
}

data class TasbihState(
    val count: Int = 0,
    val dhikr: Dhikr = Dhikr.SUBHAN_ALLAH,
    val target: Int = TasbihEngine.DEFAULT_TARGET,
)

/**
 * Counting rules shared by the app and the widget. With the default target of 33 the counter
 * follows the post-prayer tasbih (Sahih Muslim 597): SubhanAllah ×33, Alhamdulillah ×33,
 * Allahu Akbar ×34, then starts over. Any other target counts a single dhikr in rounds.
 */
object TasbihEngine {
    const val DEFAULT_TARGET = 33
    val targets = listOf(33, 99, 100)

    private val postPrayerCycle = listOf(Dhikr.SUBHAN_ALLAH, Dhikr.ALHAMDULILLAH, Dhikr.ALLAHU_AKBAR)

    data class Step(val state: TasbihState, val roundCompleted: Boolean)

    fun roundTarget(state: TasbihState): Int =
        if (state.target == DEFAULT_TARGET && state.dhikr == Dhikr.ALLAHU_AKBAR) 34 else state.target

    fun increment(state: TasbihState): Step {
        val next = state.count + 1
        if (next < roundTarget(state)) return Step(state.copy(count = next), roundCompleted = false)
        val nextDhikr = if (state.target == DEFAULT_TARGET && state.dhikr in postPrayerCycle) {
            postPrayerCycle[(postPrayerCycle.indexOf(state.dhikr) + 1) % postPrayerCycle.size]
        } else {
            state.dhikr
        }
        return Step(state.copy(count = 0, dhikr = nextDhikr), roundCompleted = true)
    }
}
