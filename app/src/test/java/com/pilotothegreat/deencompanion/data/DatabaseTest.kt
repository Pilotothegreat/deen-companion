package com.pilotothegreat.deencompanion.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pilotothegreat.deencompanion.data.db.AppDatabase
import com.pilotothegreat.deencompanion.data.db.MIGRATION_6_7
import com.pilotothegreat.deencompanion.data.quran.QuranRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test fun migrate6To7KeepsFavoritesAndBookmarks() {
        helper.createDatabase(DB_NAME, 6).apply {
            execSQL("INSERT INTO BookmarkedVerse VALUES ('2:255', 2, 255, 'Al-Baqarah', 1000)")
            execSQL("INSERT INTO HadithBookEntity VALUES ('bukhari', 'Sahih al-Bukhari', 'Imam al-Bukhari', 20)")
            execSQL("INSERT INTO HadithBookEntity VALUES ('muslim', 'Sahih Muslim', 'Imam Muslim', 7000)")
            execSQL("INSERT INTO HadithEntity VALUES ('bukhari_1', 'bukhari', 1, 'إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ', 'Actions are by intentions', 'Umar', 'Sahih', 1)")
            execSQL("INSERT INTO HadithEntity VALUES ('bukhari_2', 'bukhari', 2, 'نص', 'Text', '', 'Sahih', 0)")
            execSQL("INSERT INTO TasbihRecord VALUES ('default', 3, '[]', 'سبحان الله', 33)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 7, true, MIGRATION_6_7)

        db.query("SELECT hadithId FROM FavoriteHadith").use {
            assertEquals(1, it.count)
            it.moveToFirst()
            assertEquals("bukhari_1", it.getString(0))
        }
        db.query("SELECT searchText FROM HadithEntity WHERE id = 'bukhari_1'").use {
            it.moveToFirst()
            assertTrue(it.getString(0).contains("انما الاعمال بالنيات"))
        }
        db.query("SELECT id, isComplete FROM HadithBookEntity ORDER BY id").use {
            it.moveToFirst()
            assertEquals("bukhari" to 0, it.getString(0) to it.getInt(1))
            it.moveToNext()
            assertEquals("muslim" to 1, it.getString(0) to it.getInt(1))
        }
        db.query("SELECT COUNT(*) FROM BookmarkedVerse").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        db.query("SELECT name FROM sqlite_master WHERE name = 'TasbihRecord'").use { assertEquals(0, it.count) }
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class QuranRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val repository = QuranRepository(context, db.bookmarkDao())

    @After fun close() = db.close()

    @Test fun layoutCoversEveryAyahOn604Pages() = runTest {
        val quran = repository.quran()
        assertEquals(114, quran.surahs.size)
        assertEquals(604, quran.pages.size)
        assertEquals(6236, quran.pages.sumOf { it.verses.size })
        assertEquals((1..604).toList(), quran.pages.map { it.number })
    }

    @Test fun pageAndJuzLookups() = runTest {
        val quran = repository.quran()
        assertEquals(1, quran.pageOf(1, 7))
        assertEquals(42, quran.pageOf(2, 255))
        assertEquals(3, quran.juzOf(2, 255))
        assertEquals(604, quran.pageOf(114, 6))
        assertEquals(30, quran.juzOf(114, 6))
        assertTrue(quran.pages[41].verses.any { it.surah == 2 && it.number == 255 })
    }

    @Test fun searchIgnoresDiacritics() = runTest {
        val results = repository.search("الله الصمد")
        val hit = results.verses.first { it.verse.surah == 112 && it.verse.number == 2 }
        val range = hit.arabicMatch!!
        assertTrue(hit.verse.text.substring(range.first, range.last + 1).startsWith("ٱللَّهُ"))
    }

    @Test fun searchFindsSurahsByName() = runTest {
        assertEquals(18, repository.search("kahf").surahs.first().number)
    }

    @Test fun bookmarksRoundTrip() = runTest {
        val verse = repository.quran().verse(2, 255)!!
        repository.setBookmark(verse, true)
        assertEquals(listOf(2 to 255), repository.bookmarks.first().map { it.surah to it.ayah })
        repository.setBookmark(verse, false)
        assertTrue(repository.bookmarks.first().isEmpty())
    }
}
