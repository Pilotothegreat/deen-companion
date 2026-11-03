package com.pilotothegreat.deencompanion.data.quran

import android.content.Context
import androidx.compose.runtime.Immutable
import com.pilotothegreat.deencompanion.data.net.Http
import com.pilotothegreat.deencompanion.data.net.NetError
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Where a commentary comes from, and in what shape. */
enum class TafsirFeed { BUNDLED, QURAN_COM, MIRROR }

/**
 * A commentary on the Quran the reader can open on any ayah.
 *
 * al-Muyassar ships with the app. The others are fetched a surah at a time the first time that surah's
 * commentary is opened, and kept, so a surah read once reads offline afterwards.
 */
@Immutable
enum class Tafsir(
    val id: String,
    val arabic: Boolean,
    val nameAr: String,
    val nameEn: String,
    val authorEn: String,
    val feed: TafsirFeed,
    /** The Quran.com resource id, or the jsDelivr mirror's slug. */
    val remote: String,
) {
    MUYASSAR("muyassar", true, "التفسير الميسر", "al-Tafsir al-Muyassar", "King Fahd Complex", TafsirFeed.BUNDLED, "16"),
    SAADI("saadi", true, "تفسير السعدي", "Tafsir as-Sa'di", "Abd al-Rahman al-Sa'di", TafsirFeed.QURAN_COM, "91"),
    JALALAYN_AR("jalalayn-ar", true, "تفسير الجلالين", "Tafsir al-Jalalayn", "al-Mahalli and al-Suyuti", TafsirFeed.MIRROR, "ar-tafsir-al-jalalayn"),
    IBN_KATHIR_EN("ibnkathir-en", false, "تفسير ابن كثير (بالإنجليزية)", "Ibn Kathir (abridged)", "Ibn Kathir", TafsirFeed.QURAN_COM, "169"),
    JALALAYN_EN("jalalayn-en", false, "الجلالين (بالإنجليزية)", "al-Jalalayn (English)", "tr. Feras Hamza", TafsirFeed.MIRROR, "tafsir-al-jalalayn"),
    ;

    fun name(arabicUi: Boolean): String = if (arabicUi) nameAr else nameEn

    fun urlFor(surah: Int): String = when (feed) {
        TafsirFeed.BUNDLED -> ""
        TafsirFeed.QURAN_COM -> "https://api.quran.com/api/v4/tafsirs/$remote/by_chapter/$surah?per_page=300"
        TafsirFeed.MIRROR -> "https://cdn.jsdelivr.net/gh/spa5k/tafsir_api@main/tafsir/$remote/$surah.json"
    }

    companion object {
        fun byId(id: String?): Tafsir? = entries.firstOrNull { it.id == id }

        /** The first commentary offered: al-Muyassar in Arabic, Ibn Kathir in English. */
        fun defaultFor(arabicUi: Boolean): Tafsir = if (arabicUi) MUYASSAR else IBN_KATHIR_EN
    }
}

/**
 * One passage of commentary, and the ayahs it explains: a commentary often takes a run of ayahs
 * together, and the passage is then shown once for the whole run.
 */
@Immutable
data class TafsirPassage(val text: String, val fromAyah: Int, val toAyah: Int)

object TafsirText {

    /**
     * The passage that explains [ayah], given a surah's passages one per ayah (empty for an ayah that
     * the passage before it covers). Null when the surah has nothing for it at all.
     */
    fun passage(passages: List<String>, ayah: Int): TafsirPassage? {
        if (ayah !in 1..passages.size) return null
        var start = ayah
        while (start > 1 && passages[start - 1].isBlank()) start--
        val text = passages[start - 1]
        if (text.isBlank()) return null
        var end = ayah
        while (end < passages.size && passages[end].isBlank()) end++
        return TafsirPassage(text, start, end)
    }

    /** Quran.com's `by_chapter`: `{"tafsirs":[{"verse_key":"2:6","text":"<p>…"}]}`. */
    fun fromQuranCom(json: String, surah: Int, ayahs: Int): List<String> {
        val out = MutableList(ayahs) { "" }
        val entries = JSONObject(json).getJSONArray("tafsirs")
        for (i in 0 until entries.length()) {
            val entry = entries.getJSONObject(i)
            val (s, a) = entry.getString("verse_key").split(':').map(String::toInt)
            if (s == surah && a in 1..ayahs) out[a - 1] = entry.optString("text")
        }
        return out
    }

    /** The jsDelivr mirror: `[{"surah":2,"ayah":6,"text":"…"}]`. */
    fun fromMirror(json: String, surah: Int, ayahs: Int): List<String> {
        val out = MutableList(ayahs) { "" }
        val entries = JSONArray(json)
        for (i in 0 until entries.length()) {
            val entry = entries.getJSONObject(i)
            val a = entry.getInt("ayah")
            if (entry.optInt("surah", surah) == surah && a in 1..ayahs) out[a - 1] = entry.optString("text")
        }
        return out
    }
}

/** What the tafsir sheet can show for an ayah. */
sealed interface TafsirResult {
    data class Ready(val passage: TafsirPassage) : TafsirResult
    /** The commentary is silent on this ayah. */
    data object Empty : TafsirResult
    data class Failed(val error: NetError) : TafsirResult
}

class TafsirRepository(private val context: Context, private val quran: QuranRepository) {

    private val folder = File(context.filesDir, "tafsir")
    private val lock = Mutex()
    private var bundled: List<List<String>>? = null
    private val memory = object : LinkedHashMap<String, List<String>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String>>?) = size > 12
    }

    /** True when [tafsir]'s commentary on [surah] can be read without a connection. */
    fun isOffline(tafsir: Tafsir, surah: Int): Boolean =
        tafsir.feed == TafsirFeed.BUNDLED || fileFor(tafsir, surah).exists()

    suspend fun passage(tafsir: Tafsir, surah: Int, ayah: Int): TafsirResult = withContext(Dispatchers.IO) {
        runCatching { surahPassages(tafsir, surah) }
            .fold(
                onSuccess = { passages -> TafsirText.passage(passages, ayah)?.let { TafsirResult.Ready(it) } ?: TafsirResult.Empty },
                onFailure = { TafsirResult.Failed(NetError.of(it)) },
            )
    }

    /** Bytes of downloaded commentary on this phone. */
    fun storedBytes(): Long = folder.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    suspend fun deleteDownloads() = withContext(Dispatchers.IO) {
        lock.withLock {
            folder.deleteRecursively()
            memory.keys.removeAll { !it.startsWith(Tafsir.MUYASSAR.id) }
        }
    }

    private suspend fun surahPassages(tafsir: Tafsir, surah: Int): List<String> {
        val key = "${tafsir.id}/$surah"
        lock.withLock { memory[key] }?.let { return it }
        val passages = when (tafsir.feed) {
            TafsirFeed.BUNDLED -> bundledSurah(surah)
            else -> stored(tafsir, surah) ?: fetch(tafsir, surah)
        }
        lock.withLock { memory[key] = passages }
        return passages
    }

    private suspend fun bundledSurah(surah: Int): List<String> {
        val all = lock.withLock {
            bundled ?: run {
                val root = JSONObject(context.assets.open(MUYASSAR_ASSET).bufferedReader().use { it.readText() })
                val surahs = root.getJSONArray("surahs")
                (0 until surahs.length()).map { s ->
                    val passages = surahs.getJSONArray(s)
                    (0 until passages.length()).map { passages.getString(it) }
                }.also { bundled = it }
            }
        }
        return all[surah - 1]
    }

    private fun stored(tafsir: Tafsir, surah: Int): List<String>? = fileFor(tafsir, surah).takeIf { it.exists() }?.let { file ->
        runCatching {
            val array = JSONArray(file.readText())
            (0 until array.length()).map { array.getString(it) }
        }.getOrNull()
    }

    private suspend fun fetch(tafsir: Tafsir, surah: Int): List<String> {
        val ayahs = quran.quran().surah(surah).verses.size
        val body = Http.getText(tafsir.urlFor(surah), timeoutMs = 30_000)
        val passages = when (tafsir.feed) {
            TafsirFeed.QURAN_COM -> TafsirText.fromQuranCom(body, surah, ayahs)
            TafsirFeed.MIRROR -> TafsirText.fromMirror(body, surah, ayahs)
            TafsirFeed.BUNDLED -> error("bundled")
        }
        if (passages.all { it.isBlank() }) throw java.io.IOException("No commentary for surah $surah from ${tafsir.id}")
        val file = fileFor(tafsir, surah)
        file.parentFile?.mkdirs()
        val partial = File(file.parentFile, file.name + ".part")
        partial.writeText(JSONArray(passages).toString())
        partial.renameTo(file)
        return passages
    }

    private fun fileFor(tafsir: Tafsir, surah: Int) = File(folder, "${tafsir.id}/$surah.json")

    companion object {
        const val MUYASSAR_ASSET = "tafsir-muyassar.json"
    }
}
