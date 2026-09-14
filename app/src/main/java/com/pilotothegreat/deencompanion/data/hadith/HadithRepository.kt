package com.pilotothegreat.deencompanion.data.hadith

import android.content.Context
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.text.ArabicText
import com.pilotothegreat.deencompanion.data.db.HadithBookEntity
import com.pilotothegreat.deencompanion.data.db.HadithDao
import com.pilotothegreat.deencompanion.data.db.HadithEntity
import com.pilotothegreat.deencompanion.data.net.Http
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class HadithBookInfo(
    val id: String,
    val nameEnglish: String,
    val nameArabic: String,
    val compilerEnglish: String,
    val compilerArabic: String,
) {
    fun name(locale: Locale) = if (locale.language == "ar") nameArabic else nameEnglish
    fun compiler(locale: Locale) = if (locale.language == "ar") compilerArabic else compilerEnglish
}

object HadithBooks {
    val all = listOf(
        HadithBookInfo("bukhari", "Sahih al-Bukhari", "صحيح البخاري", "Imam al-Bukhari", "الإمام البخاري"),
        HadithBookInfo("muslim", "Sahih Muslim", "صحيح مسلم", "Imam Muslim", "الإمام مسلم"),
        HadithBookInfo("tirmidhi", "Jami' at-Tirmidhi", "جامع الترمذي", "Imam at-Tirmidhi", "الإمام الترمذي"),
        HadithBookInfo("abudawud", "Sunan Abu Dawud", "سنن أبي داود", "Imam Abu Dawud", "الإمام أبو داود"),
        HadithBookInfo("nasai", "Sunan an-Nasa'i", "سنن النسائي", "Imam an-Nasa'i", "الإمام النسائي"),
        HadithBookInfo("ibnmajah", "Sunan Ibn Majah", "سنن ابن ماجه", "Imam Ibn Majah", "الإمام ابن ماجه"),
    )

    fun info(id: String): HadithBookInfo? = all.firstOrNull { it.id == id }

    /** Maps a collection name in the bundled sample to its book id. */
    internal fun idForName(name: String): String? = when {
        name.contains("Bukhari", ignoreCase = true) -> "bukhari"
        name.contains("Muslim", ignoreCase = true) -> "muslim"
        name.contains("Tirmidhi", ignoreCase = true) -> "tirmidhi"
        name.contains("Dawud", ignoreCase = true) -> "abudawud"
        name.contains("Nasa", ignoreCase = true) -> "nasai"
        name.contains("Majah", ignoreCase = true) -> "ibnmajah"
        else -> null
    }
}

data class HadithBook(val info: HadithBookInfo, val hadithCount: Int, val isComplete: Boolean)

data class Hadith(
    val id: String,
    val bookId: String,
    val number: Int,
    val arabic: String,
    val english: String,
    val narrator: String,
    val grade: String,
)

private fun HadithEntity.toHadith() = Hadith(id, bookId, number, arabic, english, narrator, grade)

/**
 * Six collections ship as a small bundled sample; a full collection is downloaded only when the
 * user asks for it, from the fawazahmed0/hadith-api editions on jsDelivr.
 */
class HadithRepository(private val context: Context, private val dao: HadithDao) {

    private val seedLock = Mutex()
    private val _downloads = MutableStateFlow<Map<String, Float?>>(emptyMap())

    /** Book id to download progress (0..1), or null while the size is unknown. */
    val downloads: StateFlow<Map<String, Float?>> = _downloads.asStateFlow()

    // Downloads outlive the screen that started them.
    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloadJobs = ConcurrentHashMap<String, Job>()
    private val _failures = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** Emits the id of a book whose download failed. */
    val failures: SharedFlow<String> = _failures.asSharedFlow()

    fun startDownload(bookId: String) {
        if (downloadJobs[bookId]?.isActive == true) return
        downloadJobs[bookId] = downloadScope.launch {
            try {
                download(bookId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Download failed for %s", bookId)
                _failures.emit(bookId)
            } finally {
                downloadJobs.remove(bookId)
            }
        }
    }

    fun cancelDownload(bookId: String) {
        downloadJobs.remove(bookId)?.cancel()
    }

    /** Every catalog collection, including ones with nothing stored yet, so all of them can be downloaded. */
    val books: Flow<List<HadithBook>> = dao.observeBooks().map { entities ->
        val stored = entities.associateBy { it.id }
        HadithBooks.all.map { info ->
            val entity = stored[info.id]
            HadithBook(info, entity?.hadithCount ?: 0, entity?.isComplete ?: false)
        }
    }

    val favoriteIds: Flow<Set<String>> = dao.observeFavoriteIds().map { it.toSet() }

    val favorites: Flow<List<Hadith>> = dao.observeFavorites().map { list -> list.map { it.toHadith() } }

    suspend fun ensureSeeded() = seedLock.withLock {
        if (dao.bookCount() > 0) return@withLock
        val json = withContext(Dispatchers.IO) {
            context.resources.openRawResource(R.raw.hadiths).bufferedReader().use { it.readText() }
        }
        val collections = JSONObject(json).getJSONArray("collections")
        for (i in 0 until collections.length()) {
            val collection = collections.getJSONObject(i)
            val bookId = HadithBooks.idForName(collection.getString("name")) ?: continue
            val items = collection.getJSONArray("hadiths")
            val hadiths = (0 until items.length()).map { j ->
                val h = items.getJSONObject(j)
                entity(
                    bookId = bookId,
                    number = h.getInt("number"),
                    arabic = h.getString("arabic"),
                    english = h.getString("english"),
                    narrator = h.optString("narrator"),
                    grade = h.optString("grade"),
                )
            }
            val book = HadithBookEntity(
                id = bookId,
                name = collection.getString("name"),
                compiler = collection.optString("compiler"),
                hadithCount = hadiths.size,
                isComplete = false,
            )
            dao.insertBook(book, hadiths)
        }
    }

    suspend fun page(bookId: String, page: Int, pageSize: Int): List<Hadith> =
        dao.page(bookId, pageSize, page * pageSize).map { it.toHadith() }

    suspend fun search(query: String, limit: Int = 200): List<Hadith> {
        val normalized = ArabicText.normalize(query).trim().filterNot { it == '%' || it == '_' }
        if (normalized.length < 2) return emptyList()
        return dao.search(normalized, limit).map { it.toHadith() }
    }

    suspend fun setFavorite(hadithId: String, favorite: Boolean) {
        if (favorite) dao.addFavorite(hadithId, System.currentTimeMillis()) else dao.removeFavorite(hadithId)
    }

    /** Downloads the complete collection; throws on network or parse failure. */
    suspend fun download(bookId: String) {
        val info = requireNotNull(HadithBooks.info(bookId)) { "Unknown book $bookId" }
        _downloads.update { it + (bookId to null) }
        try {
            val arabic = Http.getText(editionUrl("ara", bookId), timeoutMs = 30_000) { p ->
                _downloads.update { it + (bookId to p / 2) }
            }
            val english = Http.getText(editionUrl("eng", bookId), timeoutMs = 30_000) { p ->
                _downloads.update { it + (bookId to 0.5f + p / 2) }
            }
            val hadiths = withContext(Dispatchers.Default) { mergeEditions(bookId, arabic, english) }
            check(hadiths.isNotEmpty()) { "Empty collection $bookId" }
            val existing = dao.book(bookId)
            dao.insertBook(
                HadithBookEntity(
                    id = bookId,
                    name = existing?.name ?: info.nameEnglish,
                    compiler = existing?.compiler ?: info.compilerEnglish,
                    hadithCount = hadiths.size,
                    isComplete = true,
                ),
                hadiths,
            )
        } finally {
            _downloads.update { it - bookId }
        }
    }

    private fun mergeEditions(bookId: String, arabicJson: String, englishJson: String): List<HadithEntity> {
        val arabic = JSONObject(arabicJson).getJSONArray("hadiths").byNumber()
        val english = JSONObject(englishJson).getJSONArray("hadiths").byNumber()
        return (arabic.keys + english.keys).sorted().mapNotNull { number ->
            val ar = arabic[number]
            val en = english[number]
            val arabicText = ar?.optString("text").orEmpty().trim()
            val englishText = en?.optString("text").orEmpty().trim()
            if (arabicText.isEmpty() && englishText.isEmpty()) return@mapNotNull null
            entity(bookId, number, arabicText, englishText, narrator = "", grade = gradeOf(bookId, ar, en))
        }
    }

    private fun JSONArray.byNumber(): Map<Int, JSONObject> = buildMap {
        for (i in 0 until length()) {
            val item = getJSONObject(i)
            put(item.optDouble("hadithnumber").toInt(), item)
        }
    }

    /** Both Sahih collections are graded by definition; others use Al-Albani's grade when given. */
    private fun gradeOf(bookId: String, arabic: JSONObject?, english: JSONObject?): String {
        if (bookId == "bukhari" || bookId == "muslim") return "Sahih"
        val grades = arabic?.optJSONArray("grades")?.takeIf { it.length() > 0 }
            ?: english?.optJSONArray("grades")
            ?: return ""
        val all = (0 until grades.length()).map { grades.getJSONObject(it) }
        val preferred = all.firstOrNull { it.optString("name").contains("Albani", ignoreCase = true) } ?: all.firstOrNull()
        return preferred?.optString("grade").orEmpty()
    }

    private fun entity(bookId: String, number: Int, arabic: String, english: String, narrator: String, grade: String) =
        HadithEntity(
            id = HadithEntity.idFor(bookId, number),
            bookId = bookId,
            number = number,
            arabic = arabic,
            english = english,
            narrator = narrator,
            grade = grade,
            searchText = HadithEntity.searchTextOf(arabic, english, narrator),
        )

    private fun editionUrl(language: String, bookId: String) =
        "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/$language-$bookId.json"
}
