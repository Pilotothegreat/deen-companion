package com.pilotothegreat.deencompanion.data.backup

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pilotothegreat.deencompanion.core.athkar.AthkarCategory
import com.pilotothegreat.deencompanion.core.athkar.AthkarIds
import com.pilotothegreat.deencompanion.core.athkar.AthkarItem
import com.pilotothegreat.deencompanion.data.athkar.AthkarRepository
import com.pilotothegreat.deencompanion.data.db.AppDatabase
import com.pilotothegreat.deencompanion.data.db.BookmarkEntity
import com.pilotothegreat.deencompanion.data.db.ReadingPlanEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * The Play build and the GitHub build are different packages, so moving between them used to lose
 * everything. A round trip has to come back byte for byte or the feature is worse than nothing.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class BackupRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val files = mutableListOf<File>()

    private fun store(name: String): DataStore<Preferences> {
        val file = File(context.cacheDir, "$name.preferences_pb").also(files::add)
        file.delete()
        return PreferenceDataStoreFactory.create { file }
    }

    @After fun close() {
        db.close()
        files.forEach { it.delete() }
    }

    private val city = stringPreferencesKey("city_name")
    private val adjustment = intPreferencesKey("hijri_adjustment")
    private val notifications = booleanPreferencesKey("notifications_enabled")
    private val muted = stringSetPreferencesKey("muted_prayers")

    @Test fun theReadersOwnAthkarTravelWithTheBackup() = runTest {
        val from = store("athkar-from")
        val mine = AthkarCategory(
            id = AthkarIds.newCustom(),
            titleEnglish = "بعد الجمعة",
            titleArabic = "بعد الجمعة",
            items = listOf(AthkarItem("a", "أستغفر الله", "", "", 100, noteEnglish = "بعد الصلاة", noteArabic = "بعد الصلاة")),
        )
        AthkarRepository({ "" }, from).saveCustom(mine)

        val json = BackupRepository(from, db.bookmarkDao(), db.readingPlanDao()).export()
        val to = store("athkar-to")
        assertNull(BackupRepository(to, db.bookmarkDao(), db.readingPlanDao()).restore(json))

        assertEquals(listOf(mine), AthkarRepository({ "" }, to).custom.first())
    }

    @Test fun everythingComesBackExactlyAsItWent() = runTest {
        val from = store("from")
        from.edit {
            it[city] = "Muscat"
            it[adjustment] = -1
            it[notifications] = true
            it[muted] = setOf("fajr", "isha")
        }
        db.bookmarkDao().upsert(BookmarkEntity(BookmarkEntity.idFor(2, 255), 2, 255, "Al-Baqarah", 42L))
        db.readingPlanDao().upsert(ReadingPlanEntity(startedOn = "2026-03-01", targetDays = 30, startPage = 1, lastPage = 63, updatedAt = 1L))

        val json = BackupRepository(from, db.bookmarkDao(), db.readingPlanDao()).export()

        val to = store("to")
        val restored = BackupRepository(to, db.bookmarkDao(), db.readingPlanDao())
        assertNull("a clean restore reports no error", restored.restore(json))

        val preferences = to.data.first()
        assertEquals("Muscat", preferences[city])
        assertEquals(-1, preferences[adjustment])
        assertEquals(true, preferences[notifications])
        assertEquals("a set survives as a set, not as its toString", setOf("fajr", "isha"), preferences[muted])

        val plan = db.readingPlanDao().observe().first()
        assertNotNull(plan)
        assertEquals(63, plan!!.lastPage)
        assertEquals(listOf(2 to 255), db.bookmarkDao().observeAll().first().map { it.surahNumber to it.ayahNumber })
    }

    @Test fun aRestoreReplacesRatherThanMerges() = runTest {
        val from = store("from2")
        from.edit { it[city] = "Muscat" }
        val json = BackupRepository(from, db.bookmarkDao(), db.readingPlanDao()).export()

        val to = store("to2")
        to.edit { it[stringPreferencesKey("left_over") ] = "stale" }
        BackupRepository(to, db.bookmarkDao(), db.readingPlanDao()).restore(json)
        assertNull("nothing from before survives", to.data.first()[stringPreferencesKey("left_over")])
    }

    @Test fun anythingThatIsNotABackupIsRefusedRatherThanHalfApplied() = runTest {
        val to = store("to3")
        to.edit { it[city] = "Muscat" }
        val repository = BackupRepository(to, db.bookmarkDao(), db.readingPlanDao())

        assertEquals(RestoreError.UNREADABLE, repository.restore("not json at all"))
        assertEquals(RestoreError.WRONG_FILE, repository.restore("""{"format":"something-else"}"""))
        assertEquals(
            RestoreError.TOO_NEW,
            repository.restore("""{"format":"${BackupRepository.MAGIC}","version":${BackupRepository.VERSION + 1}}"""),
        )
        assertEquals("and the phone is untouched", "Muscat", to.data.first()[city])
    }

    @Test fun aResetLeavesNothingBehind() = runTest {
        val store = store("reset")
        store.edit { it[city] = "Muscat" }
        db.readingPlanDao().upsert(ReadingPlanEntity(startedOn = "2026-03-01", targetDays = 30, startPage = 1, lastPage = 5, updatedAt = 1L))

        BackupRepository(store, db.bookmarkDao(), db.readingPlanDao()).reset()

        assertTrue(store.data.first().asMap().isEmpty())
        assertNull(db.readingPlanDao().observe().first())
    }
}
