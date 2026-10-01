package com.pilotothegreat.deencompanion.core.weather

import com.pilotothegreat.deencompanion.core.travel.Travel
import com.pilotothegreat.deencompanion.core.travel.TravelState
import com.pilotothegreat.deencompanion.data.weather.WeatherRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WmoCodesTest {

    private fun at(code: Int, temperature: Double = 25.0, wind: Double = 5.0) =
        WmoCodes.condition(code, temperature, wind)

    @Test fun everyCodeTheServiceCanReturnMapsToSomething() {
        // Open-Meteo's WMO subset. None may throw, and none may come back as a condition with no dua
        // when it plainly has one.
        val codes = listOf(0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 99)
        codes.forEach { assertNotNull("code $it", at(it)) }
        assertEquals(WeatherCondition.CLEAR, at(0))
        assertEquals(WeatherCondition.CLOUDY, at(3))
        assertEquals(WeatherCondition.FOG, at(48))
        assertEquals(WeatherCondition.RAIN, at(65))
        assertEquals(WeatherCondition.RAIN, at(82))
        assertEquals(WeatherCondition.SNOW, at(75))
        assertEquals(WeatherCondition.SNOW, at(86))
        assertEquals(WeatherCondition.THUNDER, at(99))
    }

    @Test fun anUnknownCodeIsTreatedAsOrdinaryWeather() {
        assertEquals(WeatherCondition.CLOUDY, at(7734))
    }

    @Test fun aStormIsNeverReportedAsMerelyHot() {
        assertEquals(WeatherCondition.THUNDER, at(95, temperature = 46.0, wind = 90.0))
        assertEquals(WeatherCondition.RAIN, at(61, temperature = 46.0, wind = 90.0))
    }

    @Test fun windAndTemperatureOnlySpeakWhenTheSkyHasNothingToSay() {
        assertEquals(WeatherCondition.WIND, at(1, wind = 60.0))
        assertEquals(WeatherCondition.HEAT, at(0, temperature = 47.0))
        assertEquals(WeatherCondition.COLD, at(2, temperature = -3.0))
        assertEquals("wind beats temperature", WeatherCondition.WIND, at(0, temperature = 47.0, wind = 60.0))
    }

    @Test fun anOrdinaryDaySaysNothingAtAll() {
        listOf(WeatherCondition.CLEAR, WeatherCondition.CLOUDY, WeatherCondition.FOG).forEach {
            assertEquals("${it.name} is silent", 0, it.priority)
            assertNull(it.athkarCategory)
        }
    }

    @Test fun everyConditionWorthShowingRoutesSomewhereOrExplainsItself() {
        assertEquals("rain", WeatherCondition.RAIN.athkarCategory)
        assertEquals("thunder", WeatherCondition.THUNDER.athkarCategory)
        assertEquals("wind", WeatherCondition.WIND.athkarCategory)
        // Heat and cold have no bundled dua of their own; their card carries its own words.
        assertNull(WeatherCondition.HEAT.athkarCategory)
        assertTrue(WeatherCondition.HEAT.priority > 0)
    }
}

class WeatherPrivacyTest {

    @Test fun coordinatesAreBluntedBeforeTheyLeaveTheDevice() {
        // Two decimals is about a kilometre: enough for weather, not enough to place a house.
        assertEquals(23.59, WeatherRepository.blunt(23.588912345), 0.0)
        assertEquals(-58.38, WeatherRepository.blunt(-58.382900001), 0.0)
    }

    @Test fun theRequestCarriesNothingButTheRoundedPosition() {
        val url = WeatherRepository.url(23.588912345, 58.382900001)
        assertTrue(url.startsWith("https://"))
        assertTrue("latitude=23.59" in url)
        assertTrue("longitude=58.38" in url)
        assertFalse("the precise position never goes out", "23.5889" in url)
        assertFalse("and nothing identifying goes with it", "id=" in url)
    }
}

class TravelTest {

    private val safarKm = 80

    @Test fun nearHomeIsHome() {
        assertEquals(TravelState.HOME, Travel.state(5.0, safarKm, TravelState.HOME))
    }

    @Test fun crossingTheThresholdOnlyEverSuspects() {
        assertEquals(
            "the app asks, it does not decide",
            TravelState.SUSPECTED,
            Travel.state(120.0, safarKm, TravelState.HOME),
        )
    }

    @Test fun sayingYesSticksWhileYouAreStillAway() {
        assertEquals(TravelState.CONFIRMED, Travel.state(300.0, safarKm, TravelState.CONFIRMED))
    }

    @Test fun theBoundaryDoesNotFlap() {
        // Between the return line (64 km) and the threshold (80 km) nothing changes either way.
        assertEquals(TravelState.CONFIRMED, Travel.state(70.0, safarKm, TravelState.CONFIRMED))
        assertEquals(TravelState.SUSPECTED, Travel.state(70.0, safarKm, TravelState.SUSPECTED))
        assertEquals(TravelState.HOME, Travel.state(70.0, safarKm, TravelState.HOME))
    }

    @Test fun comingWellBackInsideEndsTheTrip() {
        assertEquals(TravelState.HOME, Travel.state(40.0, safarKm, TravelState.CONFIRMED))
    }

    @Test fun distanceIsTheGreatCircleOne() {
        // Muscat to Dubai, about 340 km.
        val km = Travel.distanceKm(23.5880, 58.3829, 25.2048, 55.2708)
        assertTrue("$km km", km in 300.0..400.0)
    }

    @Test fun drivingIsOnlyEverSuspectedFromAFixTakenAnyway() {
        assertFalse("no fix, no guess", Travel.looksLikeDriving(null))
        assertFalse(Travel.looksLikeDriving(1.5f))
        assertTrue(Travel.looksLikeDriving(20f))
    }
}
