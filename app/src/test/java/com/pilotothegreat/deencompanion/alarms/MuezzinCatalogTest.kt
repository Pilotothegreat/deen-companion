package com.pilotothegreat.deencompanion.alarms

import android.content.ContentResolver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pilotothegreat.deencompanion.data.adhan.AdhanAudio
import com.pilotothegreat.deencompanion.data.adhan.Muezzin
import com.pilotothegreat.deencompanion.data.settings.SoundSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * The adhan must sound at its time. These pin the promises that make that so: a real adhan ships with
 * the app, every fetched voice is checked against a pinned checksum, and no stored choice resolves to
 * silence unless silence was chosen.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class MuezzinCatalogTest {

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()

    @Test fun theDefaultsAreBundled() {
        assertTrue(Muezzin.defaultFor(fajr = false).bundled)
        assertTrue(Muezzin.defaultFor(fajr = true).bundled)
        assertTrue("the Fajr default must be a Fajr adhan", Muezzin.defaultFor(fajr = true).fajr)
        assertEquals(SoundSettings.DEFAULT_ADHAN, Muezzin.defaultFor(fajr = false).id)
        assertEquals(SoundSettings.DEFAULT_FAJR, Muezzin.defaultFor(fajr = true).id)
    }

    @Test fun theBundledAudioIsInTheApp() {
        for (name in listOf("adhan_madinah", "adhan_fajr")) {
            val file = File("src/main/res/raw/$name.ogg")
            assertTrue("$name is missing", file.exists())
            assertTrue("$name is suspiciously small", file.length() > 100_000)
        }
    }

    @Test fun everyFetchedVoiceIsPinnedAndCredited() {
        assertEquals("ids repeat", Muezzin.entries.size, Muezzin.entries.map { it.id }.distinct().size)
        Muezzin.entries.forEach { voice ->
            assertTrue("${voice.id} has no credit", voice.credit.isNotBlank())
            if (!voice.bundled) {
                assertTrue("${voice.id} is not fetched over https", voice.url!!.startsWith("https://upload.wikimedia.org/"))
                assertEquals("${voice.id} has no pinned checksum", 64, voice.sha256!!.length)
            }
        }
    }

    @Test fun aChosenVoiceResolvesToItsAudio() {
        val uri = AdhanAudio.resolve(context, Muezzin.MADINAH.id, fajr = false)
        assertNotNull(uri)
        assertEquals(ContentResolver.SCHEME_ANDROID_RESOURCE, uri!!.scheme)
    }

    @Test fun aVoiceNotYetFetchedStillCallsTheAdhan() {
        val uri = AdhanAudio.resolve(context, Muezzin.GENTLE.id, fajr = true)
        assertEquals(AdhanAudio.uriFor(context, Muezzin.FAJR), uri)
    }

    @Test fun anUnknownIdIsStillAnAdhan() {
        assertEquals(AdhanAudio.uriFor(context, Muezzin.MADINAH), AdhanAudio.resolve(context, "makkah", fajr = false))
    }

    @Test fun onlySilenceIsSilent() {
        assertNull(AdhanAudio.resolve(context, SoundSettings.SILENT, fajr = false))
        assertEquals("content://media/x", AdhanAudio.resolve(context, "content://media/x", fajr = false).toString())
    }
}
