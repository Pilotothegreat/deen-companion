package com.pilotothegreat.deencompanion.alarms

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Do Not Disturb belongs to the reader. Bilal may turn it on for a prayer and must turn off only
 * what it turned on: a restore used to switch off a reader's own DND, for a meeting or for sleep.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class QuietDuringPrayerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(NotificationManager::class.java)

    @Before
    fun grantAccess() {
        shadowOf(manager).setNotificationPolicyAccessGranted(true)
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        QuietDuringPrayer.restore(context)
    }

    @After
    fun tearDown() = stopKoin()

    @Test fun silencesAndRestoresItsOwnSilence() {
        QuietDuringPrayer.silence(context)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY, manager.currentInterruptionFilter)
        QuietDuringPrayer.restore(context)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL, manager.currentInterruptionFilter)
    }

    @Test fun leavesTheReadersOwnDoNotDisturbOn() {
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        QuietDuringPrayer.silence(context)
        QuietDuringPrayer.restore(context)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY, manager.currentInterruptionFilter)
    }

    @Test fun restoreWithoutASilenceDoesNothing() {
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        QuietDuringPrayer.restore(context)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY, manager.currentInterruptionFilter)
    }

    @Test fun aChangeTheReaderMadeMidSilenceStands() {
        QuietDuringPrayer.silence(context)
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        QuietDuringPrayer.restore(context)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_NONE, manager.currentInterruptionFilter)
    }
}
