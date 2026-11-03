package com.pilotothegreat.deencompanion.data

import android.app.Application
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pilotothegreat.deencompanion.data.db.AppDatabase
import com.pilotothegreat.deencompanion.data.db.MIGRATION_7_8
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** v8 only adds tables; bookmarks, hadiths and favorites must come through untouched. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class Migration7to8Test {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test fun migrate7To8AddsTablesAndKeepsData() {
        helper.createDatabase(DB_NAME, 7).apply {
            execSQL("INSERT INTO BookmarkedVerse VALUES ('18:10', 18, 10, 'Al-Kahf', 1000)")
            execSQL("INSERT INTO FavoriteHadith VALUES ('bukhari_1', 1000)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 8, true, MIGRATION_7_8)

        db.query("SELECT COUNT(*) FROM BookmarkedVerse").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM FavoriteHadith").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }

        db.execSQL("INSERT INTO NaturalEvent VALUES ('quake:us1', 'EARTHQUAKE', 1000, 5.4, 120.0, 'Near Muscat', 2000)")
        db.execSQL("INSERT INTO PrayerLog VALUES ('2026-09-12:FAJR', '2026-09-12', 'FAJR', 1000)")
        db.execSQL("INSERT INTO ReadingPlan VALUES (1, '2026-09-12', 30, 1, 20, 1000)")

        db.query("SELECT kind, magnitude FROM NaturalEvent").use {
            it.moveToFirst()
            assertEquals("EARTHQUAKE", it.getString(0))
            assertEquals(5.4, it.getDouble(1), 0.001)
        }
        db.query("SELECT day FROM PrayerLog").use {
            it.moveToFirst()
            assertEquals("2026-09-12", it.getString(0))
        }
        db.query("SELECT targetDays FROM ReadingPlan WHERE id = 1").use {
            it.moveToFirst()
            assertEquals(30, it.getInt(0))
        }
    }

    private companion object {
        const val DB_NAME = "migration-7-8-test"
    }
}
