package com.pilotothegreat.deencompanion.core.analytics

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * What a usage report may contain, enforced rather than promised.
 *
 * The privacy policy says the report carries no identifier and no text. These checks are what make
 * that true a year from now, when someone adds an event in a hurry.
 */
@RunWith(AndroidJUnit4::class)
// The plain Application, so booting the real one does not start Koin twice in one sandbox.
@Config(sdk = [34], application = Application::class)
class UsageReportTest {

    private val report = UsageReport(
        environment = UsageEnvironment(
            appVersion = "2.2.0",
            appVersionCode = 220,
            installSource = "github",
            androidSdk = 34,
            deviceModel = "Google Pixel 8",
            language = "ar",
            country = "OM",
        ),
        firstSeen = "2026-09-01",
        daysActive = 12,
        sessions = 40,
        counts = listOf(
            UsageCount("2026-09-18", UsageEvent.SCREEN_QURAN.id, 7),
            UsageCount("2026-09-18", UsageEvent.RECITATION_PLAYED.id, 3),
            UsageCount("2026-09-19", UsageEvent.SCREEN_QURAN.id, 5),
        ),
    )

    @Test fun totalsAddUpAcrossDays() {
        assertEquals(12, report.totals.getValue(UsageEvent.SCREEN_QURAN.id))
        assertEquals(3, report.totals.getValue(UsageEvent.RECITATION_PLAYED.id))
    }

    @Test fun theReportCarriesNothingThatNamesADevice() {
        val json = report.toJson().toString()
        listOf("android_id", "ssaid", "advertising", "imei", "uuid", "device_id", "latitude", "longitude")
            .forEach { assertFalse("the report mentions $it", it in json) }
    }

    @Test fun theReportIsTheCountsAndTheBuildAndNothingElse() {
        val json = JSONObject(report.toJson().toString())
        assertEquals(
            setOf(
                "schema", "app_version", "app_version_code", "install_source", "android_sdk",
                "device_model", "language", "country", "first_seen", "days_active", "sessions",
                "totals", "days",
            ),
            json.keys().asSequence().toSet(),
        )
        assertEquals(UsageReport.SCHEMA, json.getInt("schema"))
    }

    @Test fun everyEventHasAStableIdOfItsOwn() {
        val ids = UsageEvent.entries.map { it.id }
        assertEquals("two events share an id", ids.size, ids.toSet().size)
        ids.forEach { id ->
            assertTrue("$id is not a wire name", id.matches(Regex("[a-z0-9_]+")))
            assertEquals(UsageEvent.byId(id)?.id, id)
        }
    }

    @Test fun anEventIsACounterAndNotAPlaceOrAPhrase() {
        // Events name what was pressed. Anything that reads like content or a place is a mistake.
        listOf("query", "text", "ayah_number", "surah_name", "city", "location", "lat", "lon")
            .forEach { banned ->
                assertTrue(
                    "an event id contains \"$banned\"",
                    UsageEvent.entries.none { banned in it.id },
                )
            }
    }
}
