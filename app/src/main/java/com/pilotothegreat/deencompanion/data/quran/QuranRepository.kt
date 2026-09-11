package com.pilotothegreat.deencompanion.data.quran

import android.content.Context
import com.pilotothegreat.deencompanion.core.text.ArabicText
import com.pilotothegreat.deencompanion.data.db.BookmarkDao
import com.pilotothegreat.deencompanion.data.db.BookmarkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.text.Normalizer

enum class Revelation { MECCAN, MEDINAN }

data class Verse(val surah: Int, val number: Int, val text: String, val translation: String) {
    val isSajdah: Boolean get() = (surah to number) in MushafLayout.sajdahs
}

data class Surah(
    val number: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val revelation: Revelation,
    val verses: List<Verse>,
)

data class MushafPage(val number: Int, val juz: Int, val verses: List<Verse>)

data class Bookmark(val surah: Int, val ayah: Int, val surahName: String, val createdAt: Long)

data class VerseMatch(val verse: Verse, val arabicMatch: IntRange?, val translationMatch: IntRange?)

data class QuranSearchResults(val surahs: List<Surah>, val verses: List<VerseMatch>) {
    val isEmpty: Boolean get() = surahs.isEmpty() && verses.isEmpty()
}

class Quran internal constructor(val surahs: List<Surah>, val pages: List<MushafPage>) {
    fun surah(number: Int): Surah = surahs[number - 1]

    fun verse(surah: Int, ayah: Int): Verse? = surahs.getOrNull(surah - 1)?.verses?.getOrNull(ayah - 1)

    /** Mushaf page (1..604) containing the ayah. */
    fun pageOf(surah: Int, ayah: Int): Int = lastStartAtOrBefore(MushafLayout.pageStarts, surah, ayah) + 1

    fun juzOf(surah: Int, ayah: Int): Int = lastStartAtOrBefore(MushafLayout.juzStarts, surah, ayah) + 1

    private fun lastStartAtOrBefore(starts: IntArray, surah: Int, ayah: Int): Int {
        var low = 0
        var high = starts.size / 2 - 1
        while (low < high) {
            val mid = (low + high + 1) / 2
            val s = starts[mid * 2]
            val a = starts[mid * 2 + 1]
            if (s < surah || (s == surah && a <= ayah)) low = mid else high = mid - 1
        }
        return low
    }
}

class QuranRepository(private val context: Context, private val bookmarkDao: BookmarkDao) {

    private val lock = Mutex()
    @Volatile private var cached: Quran? = null
    @Volatile private var searchIndex: List<String>? = null

    suspend fun quran(): Quran = cached ?: lock.withLock {
        cached ?: withContext(Dispatchers.IO) { load() }.also { cached = it }
    }

    val bookmarks: Flow<List<Bookmark>> = bookmarkDao.observeAll().map { list ->
        list.map { Bookmark(it.surahNumber, it.ayahNumber, it.surahName, it.timestamp) }
    }

    suspend fun setBookmark(verse: Verse, bookmarked: Boolean) {
        val id = BookmarkEntity.idFor(verse.surah, verse.number)
        if (bookmarked) {
            val surah = quran().surah(verse.surah)
            bookmarkDao.upsert(BookmarkEntity(id, verse.surah, verse.number, surah.nameEnglish, System.currentTimeMillis()))
        } else {
            bookmarkDao.delete(id)
        }
    }

    /** Matches surah names and verse text or translation, ignoring diacritics and case. */
    suspend fun search(query: String, limit: Int = 100): QuranSearchResults = withContext(Dispatchers.Default) {
        val normalized = ArabicText.normalize(query).trim()
        if (normalized.length < 2) return@withContext QuranSearchResults(emptyList(), emptyList())
        val quran = quran()
        val index = searchIndex ?: quran.surahs.flatMap { s -> s.verses.map { ArabicText.normalize(it.text) } }
            .also { searchIndex = it }

        val surahs = quran.surahs.filter {
            it.nameEnglish.contains(normalized, ignoreCase = true) || ArabicText.normalize(it.nameArabic).contains(normalized)
        }
        val verses = ArrayList<VerseMatch>()
        var i = 0
        for (surah in quran.surahs) {
            for (verse in surah.verses) {
                val arabicHit = index[i++].contains(normalized)
                val translationHit = !arabicHit && verse.translation.contains(normalized, ignoreCase = true)
                if (arabicHit || translationHit) {
                    verses += VerseMatch(
                        verse = verse,
                        arabicMatch = if (arabicHit) ArabicText.findMatch(verse.text, query) else null,
                        translationMatch = if (translationHit) ArabicText.findMatch(verse.translation, query) else null,
                    )
                    if (verses.size >= limit) return@withContext QuranSearchResults(surahs, verses)
                }
            }
        }
        QuranSearchResults(surahs, verses)
    }

    private fun load(): Quran {
        val json = context.assets.open("quran.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        val surahs = (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val number = obj.getInt("id")
            val versesJson = obj.getJSONArray("verses")
            Surah(
                number = number,
                nameArabic = obj.getString("name"),
                nameEnglish = obj.getString("transliteration"),
                revelation = if (obj.getString("type").equals("meccan", ignoreCase = true)) Revelation.MECCAN else Revelation.MEDINAN,
                verses = (0 until versesJson.length()).map { j ->
                    val v = versesJson.getJSONObject(j)
                    Verse(number, v.getInt("id"), cleanArabic(v.getString("text")), v.getString("translation"))
                },
            )
        }
        return Quran(surahs, buildPages(surahs))
    }

    /** Splits the ayahs into the 604 mushaf pages in a single pass. */
    private fun buildPages(surahs: List<Surah>): List<MushafPage> {
        val starts = MushafLayout.pageStarts
        val pages = ArrayList<MushafPage>(MushafLayout.PAGE_COUNT)
        var current = ArrayList<Verse>()
        var pageIndex = 0

        fun isStartOf(page: Int, verse: Verse): Boolean {
            if (page >= MushafLayout.PAGE_COUNT) return false
            val s = starts[page * 2]
            val a = starts[page * 2 + 1]
            return verse.surah > s || (verse.surah == s && verse.number >= a)
        }

        val quranForJuz = Quran(surahs, emptyList())
        for (surah in surahs) {
            for (verse in surah.verses) {
                while (current.isNotEmpty() && isStartOf(pageIndex + 1, verse)) {
                    pages += MushafPage(pageIndex + 1, quranForJuz.juzOf(current[0].surah, current[0].number), current)
                    current = ArrayList()
                    pageIndex++
                }
                current += verse
            }
        }
        pages += MushafPage(pageIndex + 1, quranForJuz.juzOf(current[0].surah, current[0].number), current)
        return pages
    }

    /** Precomposes hamza forms that the Uthmanic Hafs font renders poorly when decomposed. */
    private fun cleanArabic(raw: String): String = Normalizer.normalize(raw, Normalizer.Form.NFC)
        .replace("إِ", "إ")
        .replace("أُ", "أ")
        .replace("أَ", "أ")
        .replace("ءَأَ", "أَأَ")
        .replace("ءَا", "آ")
}
