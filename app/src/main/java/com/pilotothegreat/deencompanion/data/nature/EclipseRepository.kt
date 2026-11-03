package com.pilotothegreat.deencompanion.data.nature

import android.content.Context
import com.pilotothegreat.deencompanion.core.astro.MoonPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant

enum class EclipseKind { SOLAR, LUNAR }

data class Eclipse(
    val kind: EclipseKind,
    /** Greatest eclipse, in UTC. */
    val at: Instant,
    /** total, annular, partial, penumbral or hybrid, as NASA classifies it. */
    val type: String,
    val magnitude: Double,
    /** NASA's own summary of where on earth it can be seen. */
    val regions: String,
)

/**
 * The bundled eclipse table.
 *
 * Eclipses are predictable centuries ahead, so this asks no server: a phone with no signal still
 * knows one is coming, which matters because salat al-kusuf follows witnessing the eclipse.
 */
class EclipseRepository(private val context: Context) {

    private val lock = Mutex()
    @Volatile private var cached: List<Eclipse>? = null

    suspend fun all(): List<Eclipse> = cached ?: lock.withLock {
        cached ?: withContext(Dispatchers.IO) { load() }.also { cached = it }
    }

    /** Anything within [withinDays], soonest first. */
    suspend fun upcoming(now: Instant, withinDays: Long = 3): List<Eclipse> {
        val until = now.plusSeconds(withinDays * 86_400)
        return all().filter { it.at.isAfter(now.minusSeconds(6 * 3600)) && it.at.isBefore(until) }
    }

    /**
     * Whether the moon is above the horizon at greatest eclipse. Solar eclipses get no equivalent:
     * the path of totality is a narrow track this app has no table for, so it says the date and
     * NASA's regions and claims nothing about local times.
     */
    fun isLunarEclipseVisible(eclipse: Eclipse, latitude: Double, longitude: Double): Boolean =
        eclipse.kind == EclipseKind.LUNAR && MoonPosition.isUp(eclipse.at, latitude, longitude)

    private fun load(): List<Eclipse> {
        val root = JSONObject(context.assets.open("eclipses.json").bufferedReader().use { it.readText() })
        val array = root.getJSONArray("eclipses")
        return (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            Eclipse(
                kind = if (item.getString("kind") == "solar") EclipseKind.SOLAR else EclipseKind.LUNAR,
                at = Instant.parse(item.getString("at")),
                type = item.getString("type"),
                magnitude = item.getDouble("magnitude"),
                regions = item.getString("regions"),
            )
        }
    }
}
