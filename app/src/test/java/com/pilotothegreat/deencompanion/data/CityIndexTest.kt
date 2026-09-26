package com.pilotothegreat.deencompanion.data

import com.pilotothegreat.deencompanion.data.location.CityIndex
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

/** Runs against the bundled GeoNames asset. */
class CityIndexTest {

    private val index = CityIndex { File("src/main/assets/cities.json").readText() }

    @Test fun englishPrefixFindsTheBiggestMatchFirst() = runTest {
        val first = index.search("musc").first()
        assertEquals("OM", first.countryCode)
        assertEquals("Asia/Muscat", first.timezoneId)
    }

    @Test fun accentsAndCaseAreIgnored() = runTest {
        val nizwa = index.search("NIZWA").first()
        assertEquals("OM", nizwa.countryCode)
    }

    @Test fun arabicNamesAreSearchable() = runTest {
        val results = index.search("مسقط")
        assertTrue(results.isNotEmpty())
        assertEquals("OM", results.first().countryCode)
    }

    @Test fun countryNameListsItsCities() = runTest {
        val results = index.search("oman")
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.countryCode == "OM" || "oman" in CityIndex.fold(it.name) })
    }

    @Test fun nearestCityNamesACoordinate() = runTest {
        val city = index.nearest(23.60, 58.40)
        assertEquals("OM", city?.countryCode)
        assertNull(index.nearest(0.0, -30.0))
    }

    @Test fun suggestionsStayInTheCountry() = runTest {
        val cities = index.suggestions("OM")
        assertTrue(cities.size > 5)
        assertTrue(cities.all { it.countryCode == "OM" })
    }

    @Test fun labelsAreLocalized() = runTest {
        val muscat = index.search("muscat").first()
        assertTrue(muscat.label(Locale.ENGLISH).endsWith(", Oman"))
        assertTrue(muscat.label(Locale.forLanguageTag("ar")).contains("، "))
    }

    @Test fun unknownQueryFindsNothing() = runTest {
        assertTrue(index.search("zzqqxx").isEmpty())
    }
}
