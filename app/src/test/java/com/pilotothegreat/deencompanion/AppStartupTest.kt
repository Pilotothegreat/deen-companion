package com.pilotothegreat.deencompanion

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import io.mockk.mockkObject
import io.mockk.coEvery
import io.mockk.unmockkObject
import org.junit.Before
import org.junit.After
import com.pilotothegreat.deencompanion.util.LocationHelper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = DeenApplication::class)
class AppStartupTest {

    @Before
    fun setUp() {
        println(">>> DEBUG: AppStartupTest.setUp() start")
        mockkObject(LocationHelper)
        coEvery { LocationHelper.getDeviceLocation(any()) } returns LocationHelper.LocationData(
            latitude = 21.3891,
            longitude = 39.8579,
            cityName = "Makkah, Saudi Arabia",
            timezoneId = "Asia/Riyadh"
        )
        coEvery { LocationHelper.fetchIpLocation() } returns LocationHelper.LocationData(
            latitude = 21.3891,
            longitude = 39.8579,
            cityName = "Makkah, Saudi Arabia",
            timezoneId = "Asia/Riyadh"
        )
        println(">>> DEBUG: AppStartupTest.setUp() end")
    }

    @After
    fun tearDown() {
        println(">>> DEBUG: AppStartupTest.tearDown() start")
        unmockkObject(LocationHelper)
        println(">>> DEBUG: AppStartupTest.tearDown() end")
    }

    @Test
    fun testAppLaunch() {
        println(">>> DEBUG: AppStartupTest.testAppLaunch() start")
        println(">>> DEBUG: AppStartupTest.testAppLaunch() building activity")
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        println(">>> DEBUG: AppStartupTest.testAppLaunch() setting up activity")
        controller.setup()
        println(">>> DEBUG: AppStartupTest.testAppLaunch() getting activity")
        val activity = controller.get()
        assert(activity != null)
        println(">>> DEBUG: AppStartupTest.testAppLaunch() assert complete")
    }
}
