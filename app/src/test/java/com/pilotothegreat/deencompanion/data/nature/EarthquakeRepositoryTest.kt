package com.pilotothegreat.deencompanion.data.nature

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pilotothegreat.deencompanion.data.db.NaturalEventDao
import com.pilotothegreat.deencompanion.data.db.NaturalEventEntity
import androidx.datastore.preferences.core.emptyPreferences
import com.pilotothegreat.deencompanion.data.settings.LocationSource
import com.pilotothegreat.deencompanion.data.settings.SavedLocation
import com.pilotothegreat.deencompanion.data.settings.SmartSettings
import com.pilotothegreat.deencompanion.data.settings.toAppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class EarthquakeRepositoryTest {

    private val muscat = SavedLocation(
        latitude = 23.5880,
        longitude = 58.3829,
        cityName = "Muscat",
        timezoneId = "Asia/Muscat",
        countryCode = "OM",
        updatedAt = 1L,
        isDefault = false,
        source = LocationSource.DEVICE,
    )

    private class FakeDao : NaturalEventDao {
        val rows = MutableStateFlow<List<NaturalEventEntity>>(emptyList())
        override fun observeSince(since: Long): Flow<List<NaturalEventEntity>> =
            rows.map { list -> list.filter { it.at >= since } }

        override suspend fun upsert(events: List<NaturalEventEntity>) {
            rows.value = (rows.value.filterNot { row -> events.any { it.id == row.id } } + events)
        }

        override suspend fun deleteBefore(before: Long) {
            rows.value = rows.value.filter { it.at >= before }
        }
    }

    /** Two quakes: one just off Oman, one in Chile. */
    private fun feed(now: Long) = """
        {"features":[
          {"id":"near","properties":{"mag":5.8,"time":$now,"place":"Gulf of Oman"},
           "geometry":{"coordinates":[57.8,24.6,10]}},
          {"id":"tiny","properties":{"mag":4.6,"time":$now,"place":"Gulf of Oman"},
           "geometry":{"coordinates":[57.8,24.6,10]}},
          {"id":"weak","properties":{"mag":3.1,"time":$now,"place":"Gulf of Oman"},
           "geometry":{"coordinates":[57.8,24.6,10]}},
          {"id":"far","properties":{"mag":7.2,"time":$now,"place":"Chile"},
           "geometry":{"coordinates":[-70.6,-33.4,10]}}
        ]}
    """.trimIndent()

    /** Defaults, with only the two things these tests care about set. */
    private fun settings(smart: SmartSettings = SmartSettings(), location: SavedLocation = muscat) =
        emptyPreferences().toAppSettings().copy(location = location, smart = smart)

    @Test fun onlyNearbyQuakesWorthNoticingAreKept() = runTest {
        val dao = FakeDao()
        val now = 1_800_000_000_000L
        EarthquakeRepository(dao) { feed(now) }.refresh(settings(), now)
        val kept = dao.rows.value.map { it.id }
        assertEquals("the distant and the weak are both dropped", listOf("quake:near", "quake:tiny"), kept)
    }

    @Test fun theStrongestNearbyQuakeComesFirst() = runTest {
        val dao = FakeDao()
        val now = 1_800_000_000_000L
        val repository = EarthquakeRepository(dao) { feed(now) }
        repository.refresh(settings(), now)
        val recent = repository.recent(now).first()
        assertEquals(5.8, recent.first().magnitude, 0.001)
        assertTrue("and it knows how far away it was", recent.first().distanceKm < EarthquakeRepository.NEARBY_KM)
    }

    @Test fun theFeedIsNotAskedWhenTheFeatureIsOffOrTheLocationIsUnknown() = runTest {
        val now = 1_800_000_000_000L
        var calls = 0
        val counting: suspend (String) -> String = { calls++; feed(now) }

        EarthquakeRepository(FakeDao(), counting).refresh(settings(SmartSettings(naturalEvents = false)), now)
        assertEquals("off means off", 0, calls)

        EarthquakeRepository(FakeDao(), counting).refresh(
            settings(location = muscat.copy(isDefault = true)),
            now,
        )
        assertEquals("and an unknown location has nothing to be near", 0, calls)
    }

    @Test fun theFeedIsFetchedAtMostOnceAnHour() = runTest {
        var calls = 0
        val now = 1_800_000_000_000L
        val repository = EarthquakeRepository(FakeDao()) { calls++; feed(now) }
        repository.refresh(settings(), now)
        repository.refresh(settings(), now + 60_000)
        assertEquals(1, calls)
        repository.refresh(settings(), now + 2 * 60 * 60 * 1000L)
        assertEquals(2, calls)
    }

    @Test fun aBrokenFeedIsSilentRatherThanFatal() = runTest {
        val dao = FakeDao()
        EarthquakeRepository(dao) { "not json" }.refresh(settings(), 1L)
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test fun theDefaultsAreDeliberate() {
        // Loud enough to matter, near enough to be felt, and held long enough to still be there
        // when the phone is next opened.
        assertEquals(4.5, EarthquakeRepository.MIN_MAGNITUDE, 0.0)
        assertEquals(800.0, EarthquakeRepository.NEARBY_KM, 0.0)
        assertEquals(48 * 60 * 60 * 1000L, EarthquakeRepository.WINDOW_MILLIS)
        assertTrue(EarthquakeRepository.FEED.startsWith("https://"))
    }
}
