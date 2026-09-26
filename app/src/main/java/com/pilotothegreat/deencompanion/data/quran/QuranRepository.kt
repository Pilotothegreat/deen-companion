package com.pilotothegreat.deencompanion.data.quran

import android.content.Context
import com.pilotothegreat.deencompanion.core.quran.QuranDestination
import com.pilotothegreat.deencompanion.core.quran.QuranQuery
import com.pilotothegreat.deencompanion.core.quran.SurahNames
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
import org.json.JSONObject

enum class Revelation { MECCAN, MEDINAN }

data class Verse(val surah: Int, val number: Int, val text: String, val translation: String) {
    /** Set when this ayah carries a prostration of recitation. */
    val sajdah: Sajdah? get() = MushafLayout.sajdahs[surah to number]

    val isSajdah: Boolean get() = sajdah != null

    /** The rub' al-hizb (1..240) that opens at this ayah, or null: where the reader draws a ۞. */
    val quarterStart: Int? get() = MushafLayout.quarterStartingAt(surah, number)

    /** The translation as a standalone excerpt: a quote that runs on into the next ayah is closed. */
    val standaloneTranslation: String
        get() = if (translation.count { it == '"' } % 2 == 1) "$translation\"" else translation
}

data class Surah(
    val number: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val revelation: Revelation,
    val verses: List<Verse>,
    /**
     * The Basmala the mushaf writes above this surah, or null where it has none: al-Fatihah, whose
     * first ayah it is, and at-Tawbah, which opens without it. It is a heading, never part of ayah 1.
     */
    val bismillah: String?,
)

data class MushafPage(
    val number: Int,
    val juz: Int,
    val verses: List<Verse>,
    /**
     * The fifteen lines the printed page breaks into, or empty when the layout table could not be
     * read. Empty means the reader flows the page instead, which is what it always did.
     */
    val lines: List<MushafLine> = emptyList(),
)

data class Bookmark(val surah: Int, val ayah: Int, val surahName: String, val createdAt: Long)

data class VerseMatch(val verse: Verse, val arabicMatch: IntRange?, val translationMatch: IntRange?)

/**
 * What a query found, in the order it is worth showing.
 *
 * [destinations] are the places the query named outright — Ayat al-Kursi, 2:255, juz 30 — and go first,
 * because someone who names a place has already found what they were looking for. The text search runs
 * all the same and its results follow, with the ones matching the Arabic ahead of the ones matching only
 * the translation.
 */
data class QuranSearchResults(
    val destinations: List<ResolvedDestination>,
    val surahs: List<Surah>,
    val verses: List<VerseMatch>,
) {
    val isEmpty: Boolean get() = destinations.isEmpty() && surahs.isEmpty() && verses.isEmpty()
}

/** A [QuranDestination] with the page it opens at worked out. */
data class ResolvedDestination(val destination: QuranDestination, val page: Int, val verse: Verse?)

class Quran internal constructor(
    val surahs: List<Surah>,
    val pages: List<MushafPage>,
    /** Shown wherever the translation is, because its licence asks for the credit. */
    val translation: TranslationInfo,
    val textSource: TextSource,
) {
    fun surah(number: Int): Surah = surahs[number - 1]

    fun verse(surah: Int, ayah: Int): Verse? = surahs.getOrNull(surah - 1)?.verses?.getOrNull(ayah - 1)

    /** Mushaf page (1..604) containing the ayah. */
    fun pageOf(surah: Int, ayah: Int): Int = MushafLayout.pageOf(surah, ayah)

    fun juzOf(surah: Int, ayah: Int): Int = MushafLayout.juzOf(surah, ayah)

    fun hizbOf(surah: Int, ayah: Int): Int = MushafLayout.hizbOf(surah, ayah)

    /** Which quarter of its hizb the ayah falls in, 1 through 4. */
    fun rubOf(surah: Int, ayah: Int): Int = MushafLayout.rubOf(surah, ayah)

    fun manzilOf(surah: Int, ayah: Int): Int = MushafLayout.manzilOf(surah, ayah)

    fun rukuOf(surah: Int, ayah: Int): Int = MushafLayout.rukuOf(surah, ayah)

    /** The printed mushaf these divisions follow, shown in About. */
    val edition: String get() = MushafLayout.EDITION
}

class QuranRepository(private val context: Context, private val bookmarkDao: BookmarkDao) {

    private companion object {
        /** Below this, a query searches surah names only. */
        const val MIN_VERSE_QUERY = 2

        /** Below this, a word is too short to be anyone's idea of a surah's name. */
        const val MIN_NAME_QUERY = 3
    }

    private val lock = Mutex()
    @Volatile private var cached: Quran? = null
    @Volatile private var searchIndex: List<String>? = null

    /**
     * The mushaf with [translationId] alongside it. The Arabic is parsed once; switching translation
     * only re-reads the much smaller translation file.
     */
    suspend fun quran(translationId: String = Translations.DEFAULT_ID): Quran {
        cached?.takeIf { it.translation.id == translationId }?.let { return it }
        return lock.withLock {
            cached?.takeIf { it.translation.id == translationId }
                ?: withContext(Dispatchers.IO) { load(translationId) }.also { cached = it }
        }
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

    /**
     * What the query names, and what it matches.
     *
     * The query is read first — a passage by name, a reference, a juz or a page — and only then looked
     * for in the text, so "ayat al kursi" opens 2:255 instead of finding nothing: those words are the
     * ayah's name, not its words.
     */
    suspend fun search(query: String, limit: Int = 100): QuranSearchResults = withContext(Dispatchers.Default) {
        val quran = quran()
        val parsed = QuranQuery.parse(
            raw = query,
            verseCount = { surah -> quran.surahs.getOrNull(surah - 1)?.verses?.size ?: 0 },
            surahNamed = { name -> surahNamed(quran, name) },
        )
        val destinations = parsed.destinations.mapNotNull { resolve(quran, it) }

        val normalized = ArabicText.normalize(parsed.text).trim()
        if (normalized.isEmpty()) return@withContext QuranSearchResults(destinations, emptyList(), emptyList())

        val named = destinations.mapNotNull { (it.destination as? QuranDestination.SurahStart)?.surah }.toSet()
        val surahs = quran.surahs.filter {
            it.number !in named &&
                (it.nameEnglish.contains(normalized, ignoreCase = true) || ArabicText.normalize(it.nameArabic).contains(normalized))
        }
        // One letter is a reasonable way to look for a surah and a hopeless way to look through six
        // thousand ayahs, which would match nearly all of them and take a moment doing it.
        if (normalized.length < MIN_VERSE_QUERY) return@withContext QuranSearchResults(destinations, surahs, emptyList())

        val index = searchIndex ?: quran.surahs.flatMap { s -> s.verses.map { ArabicText.normalize(it.text) } }
            .also { searchIndex = it }
        val arabic = ArrayList<VerseMatch>()
        val translated = ArrayList<VerseMatch>()
        var i = 0
        for (surah in quran.surahs) {
            for (verse in surah.verses) {
                val arabicHit = index[i++].contains(normalized)
                val translationHit = !arabicHit && verse.translation.contains(normalized, ignoreCase = true)
                if (arabicHit) {
                    arabic += VerseMatch(verse, ArabicText.findMatch(verse.text, parsed.text), null)
                } else if (translationHit) {
                    translated += VerseMatch(verse, null, ArabicText.findMatch(verse.translation, parsed.text))
                }
                if (arabic.size + translated.size >= limit) {
                    return@withContext QuranSearchResults(destinations, surahs, arabic + translated)
                }
            }
        }
        QuranSearchResults(destinations, surahs, arabic + translated)
    }

    /** The surah a name belongs to: its transliteration, its Arabic name, or a spelling people use. */
    private fun surahNamed(quran: Quran, name: String): Int? {
        val folded = QuranQuery.fold(name).replace(" ", "")
        if (folded.length < MIN_NAME_QUERY) return null
        SurahNames.ALIASES[folded]?.let { return it }
        return quran.surahs.firstOrNull { surah ->
            val english = QuranQuery.fold(surah.nameEnglish).replace(" ", "")
            val arabic = QuranQuery.fold(surah.nameArabic).replace(" ", "")
            folded == english || folded == arabic ||
                folded == english.removeArticle() || folded == arabic.removePrefix("ال")
        }?.number
    }

    /** "al baqarah" and "baqarah" are the same surah; so are "an nas" and "nas". */
    private fun String.removeArticle(): String =
        listOf("al", "ash", "adh", "ath", "ad", "an", "ar", "as", "at", "az").firstNotNullOfOrNull { article ->
            removePrefix(article).takeIf { it.length != length }
        } ?: this

    private fun resolve(quran: Quran, destination: QuranDestination): ResolvedDestination? = when (destination) {
        is QuranDestination.Ayah -> quran.verse(destination.surah, destination.ayah)
            ?.let { ResolvedDestination(destination, quran.pageOf(destination.surah, destination.ayah), it) }
        is QuranDestination.SurahStart -> quran.surahs.getOrNull(destination.surah - 1)
            ?.let { ResolvedDestination(destination, quran.pageOf(it.number, 1), it.verses.first()) }
        is QuranDestination.Page -> quran.pages.getOrNull(destination.number - 1)
            ?.let { ResolvedDestination(destination, it.number, it.verses.firstOrNull()) }
        is QuranDestination.Juz -> quran.pages.firstOrNull { it.juz == destination.number }
            ?.let { ResolvedDestination(destination, it.number, it.verses.firstOrNull()) }
    }

    /**
     * Reads the Uthmani text and the chosen translation. The text is used exactly as published:
     * Tanzil's licence forbids altering it, and an earlier "font fix" here silently dropped more
     * than fifteen thousand vowel marks.
     */
    private fun load(translationId: String): Quran {
        val root = JSONObject(context.assets.open("quran-ar.json").bufferedReader().use { it.readText() })
        val sourceJson = root.getJSONObject("source")
        val surahsJson = root.getJSONArray("surahs")
        val info = Translations.byId(translationId)
        val translated = readTranslation(info)

        val surahs = (0 until surahsJson.length()).map { i ->
            val obj = surahsJson.getJSONObject(i)
            val number = obj.getInt("id")
            val versesJson = obj.getJSONArray("verses")
            val meanings = translated?.optJSONArray(i)
            Surah(
                number = number,
                nameArabic = obj.getString("name"),
                nameEnglish = obj.getString("transliteration"),
                revelation = if (obj.getString("type").equals("meccan", ignoreCase = true)) Revelation.MECCAN else Revelation.MEDINAN,
                verses = (0 until versesJson.length()).map { j ->
                    Verse(number, j + 1, versesJson.getString(j), meanings?.optString(j).orEmpty())
                },
                bismillah = obj.optString("bismillah").takeIf { it.isNotEmpty() },
            )
        }
        val source = TextSource(
            name = sourceJson.getString("name"),
            source = sourceJson.getString("source"),
            terms = sourceJson.getString("terms"),
        )
        return Quran(surahs, buildPages(surahs, readLines(surahs)), info, source)
    }

    /** Null when the translation file is missing, so the Arabic still opens. */
    private fun readTranslation(info: TranslationInfo): JSONArray? = runCatching {
        val json = context.assets.open(Translations.assetFor(info.id)).bufferedReader().use { it.readText() }
        JSONObject(json).getJSONArray("verses")
    }.getOrNull()

    /** Null when the table is missing or does not fit the text; the reader then flows the page. */
    private fun readLines(surahs: List<Surah>): List<List<MushafLine>>? = runCatching {
        val json = context.assets.open(MushafLines.ASSET).bufferedReader().use { it.readText() }
        MushafLines.parse(json, surahs)
    }.getOrNull()

    private fun buildPages(surahs: List<Surah>, lines: List<List<MushafLine>>?): List<MushafPage> {
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

        for (surah in surahs) {
            for (verse in surah.verses) {
                while (current.isNotEmpty() && isStartOf(pageIndex + 1, verse)) {
                    pages += MushafPage(
                        pageIndex + 1,
                        MushafLayout.juzOf(current[0].surah, current[0].number),
                        current,
                        lines?.getOrNull(pageIndex).orEmpty(),
                    )
                    current = ArrayList()
                    pageIndex++
                }
                current += verse
            }
        }
        pages += MushafPage(
            pageIndex + 1,
            MushafLayout.juzOf(current[0].surah, current[0].number),
            current,
            lines?.getOrNull(pageIndex).orEmpty(),
        )
        return pages
    }
}
