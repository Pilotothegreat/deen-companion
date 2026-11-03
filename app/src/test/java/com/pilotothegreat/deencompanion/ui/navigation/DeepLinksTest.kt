package com.pilotothegreat.deencompanion.ui.navigation

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class DeepLinksTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    /** The update notification opens the dialog, not the Settings screen it used to drop people on. */
    @Test fun theUpdateLinkAsksForTheDialog() {
        assertEquals(UpdateKey, DeepLinks.parse(DeepLinks.screen(context, DeepLinks.UPDATE)))
    }

    @Test fun screensStillParse() {
        assertEquals(SettingsKey, DeepLinks.parse(DeepLinks.screen(context, DeepLinks.SETTINGS)))
        assertEquals(ReaderKey(2, 2, 255), DeepLinks.parse(DeepLinks.reader(context, 2, 2, 255)))
    }
}
