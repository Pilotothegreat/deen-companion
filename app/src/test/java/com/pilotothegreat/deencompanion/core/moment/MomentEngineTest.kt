package com.pilotothegreat.deencompanion.core.moment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class MomentEngineTest {

    private val now: ZonedDateTime = ZonedDateTime.of(2026, 6, 12, 12, 0, 0, 0, ZoneId.of("Asia/Muscat"))

    private fun moment(
        id: String,
        priority: Int = 50,
        kind: MomentKind = MomentKind.OCCASION,
        from: Long = -1,
        until: Long = 1,
        peakInMinutes: Long? = null,
        dismissal: Dismissal = Dismissal.TODAY,
    ) = Moment(
        id = id,
        kind = kind,
        priority = priority,
        title = 0,
        body = null,
        startsAt = now.plusHours(from),
        endsAt = now.plusHours(until),
        peak = peakInMinutes?.let { now.plusMinutes(it) },
        dismissal = dismissal,
    )

    @Test fun expiredAndNotYetStartedMomentsAreDropped() {
        val ranked = MomentEngine.rank(
            listOf(
                moment("over", from = -5, until = -1),
                moment("later", from = 1, until = 5),
                moment("now"),
            ),
            now,
        )
        assertEquals(listOf("now"), ranked.map { it.id })
    }

    @Test fun priorityWinsAndImminenceBreaksTheTie() {
        val ranked = MomentEngine.rank(
            listOf(
                moment("low", priority = 10),
                moment("distant", priority = 90, peakInMinutes = 50),
                moment("soon", priority = 90, peakInMinutes = 5),
                moment("undated", priority = 90),
            ),
            now,
        )
        assertEquals(listOf("soon", "distant", "undated", "low"), ranked.map { it.id })
    }

    @Test fun dismissedMomentsStayAway() {
        val ranked = MomentEngine.rank(listOf(moment("a"), moment("b")), now, dismissed = setOf("a"))
        assertEquals(listOf("b"), ranked.map { it.id })
    }

    @Test fun theSameMomentTwiceIsStillOneCard() {
        val ranked = MomentEngine.rank(listOf(moment("a"), moment("a", priority = 90)), now)
        assertEquals(1, ranked.size)
    }

    @Test fun todayShowsAtMostThreeCardsAndOneThingToFix() {
        val ranked = MomentEngine.rank(
            listOf(
                moment("fix-a", priority = 99, kind = MomentKind.MAINTENANCE),
                moment("fix-b", priority = 98, kind = MomentKind.MAINTENANCE),
                moment("fix-c", priority = 97, kind = MomentKind.MAINTENANCE),
                moment("eid", priority = 96),
                moment("kahf", priority = 60),
                moment("weather", priority = 40, kind = MomentKind.NATURE),
            ),
            now,
        )
        val cards = MomentEngine.forToday(ranked)
        assertEquals(3, cards.size)
        assertEquals(1, cards.count { it.kind == MomentKind.MAINTENANCE })
        assertEquals(listOf("fix-a", "eid", "kahf"), cards.map { it.id })
    }

    @Test fun theWidgetNeverShowsAChore() {
        val ranked = MomentEngine.rank(
            listOf(moment("fix", priority = 99, kind = MomentKind.MAINTENANCE), moment("eid", priority = 50)),
            now,
        )
        assertEquals("eid", MomentEngine.forWidget(ranked)?.id)
        assertNull(MomentEngine.forWidget(MomentEngine.rank(listOf(moment("fix", kind = MomentKind.MAINTENANCE)), now)))
    }

    @Test fun rankingIsStableForTheSameInput() {
        val moments = listOf(moment("b", priority = 50), moment("a", priority = 50), moment("c", priority = 50))
        val first = MomentEngine.rank(moments, now).map { it.id }
        val again = MomentEngine.rank(moments.reversed(), now).map { it.id }
        assertEquals(first, again)
        assertTrue(first == listOf("a", "b", "c"))
    }
}
