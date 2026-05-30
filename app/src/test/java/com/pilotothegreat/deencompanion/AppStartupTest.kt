package com.pilotothegreat.deencompanion

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = DeenApplication::class) // Using standard API 33 and custom Application class
class AppStartupTest {

    @Test
    fun testAppLaunch() {
        // This will launch MainActivity and run its onCreate lifecycle method,
        // which will trigger Koin initialization and compose UI layout rendering.
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assert(activity != null)
    }
}

