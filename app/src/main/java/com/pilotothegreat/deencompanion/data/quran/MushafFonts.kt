package com.pilotothegreat.deencompanion.data.quran

import android.content.Context
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.LruCache
import com.pilotothegreat.deencompanion.data.net.Http
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
 * The King Fahd Complex's QCF V2 page fonts: one per page of the mushaf, fetched as the pages are read.
 *
 * Each font is that page's words, drawn as the 1421H print draws them. They come from the Quran.com
 * CDN, are checked against the size and SHA-256 that `scripts/quran/build_qcf.py` pinned, and are kept
 * in the app's own storage, so a page once opened reads offline from then on. A page whose font has
 * not arrived is set in the bundled font meanwhile; nothing waits on the network.
 */
class MushafFonts(private val context: Context) {

    private val folder = File(context.filesDir, "mushaf/qcf2")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<Int, Job>()
    private val parallel = Semaphore(3)

    private val pins: Pins? by lazy { runCatching { Pins.read(context) }.onFailure { Timber.w(it, "No QCF pins") }.getOrNull() }

    private val _ready = MutableStateFlow<Set<Int>>(emptySet())

    /** Pages whose font is on this phone. Page 0 stands for the surah-name font. */
    val ready: StateFlow<Set<Int>> = _ready.asStateFlow()

    private val _bulk = MutableStateFlow<Float?>(null)

    /** Progress of "download the whole mushaf", 0..1, or null when it is not running. */
    val bulk: StateFlow<Float?> = _bulk.asStateFlow()
    private var bulkJob: Job? = null

    private val typefaces = LruCache<Int, Typeface>(TYPEFACES_KEPT)

    init {
        scope.launch { _ready.value = scan() }
    }

    /** The whole set's size in bytes, for the download button. */
    val totalBytes: Long get() = pins?.pages?.sumOf { it.size } ?: 0L

    /** Bytes stored on this phone. */
    fun storedBytes(): Long = folder.listFiles()?.sumOf { it.length() } ?: 0L

    fun isReady(page: Int): Boolean = page in _ready.value

    /**
     * The page's font, or null while it is not here. Typefaces are kept for the few pages around the
     * one being read; a Typeface holds the whole font in memory.
     */
    fun typeface(page: Int): Typeface? {
        if (!isReady(page)) return null
        typefaces.get(page)?.let { return it }
        return runCatching { Typeface.createFromFile(fileFor(page)) }
            .onFailure { Timber.w(it, "Unreadable QCF font %d", page) }
            .getOrNull()
            ?.also { typefaces.put(page, it) }
    }

    /** The surah-name font, whose glyph for "003" is the calligraphed «سورة آل عمران». */
    fun surahNames(): Typeface? = typeface(NAMES)

    /**
     * Fetches what the reader on [page] needs: that page's font at once, on any connection, and the two
     * pages either side ahead of time on Wi-Fi.
     */
    fun prefetch(page: Int) {
        fetch(NAMES)
        fetch(page)
        if (unmetered()) {
            for (offset in intArrayOf(1, -1, 2, -2)) {
                val other = page + offset
                if (other in 1..PAGES) fetch(other)
            }
        }
    }

    /** Every page, for reading offline anywhere. Runs until done or [cancelAll]. */
    fun downloadAll() {
        if (bulkJob?.isActive == true) return
        bulkJob = scope.launch {
            try {
                _bulk.value = progress()
                fetchNow(NAMES)
                for (page in 1..PAGES) {
                    if (!isReady(page)) fetchNow(page)
                    _bulk.value = progress()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Whole-mushaf download stopped")
            } finally {
                _bulk.value = null
            }
        }
    }

    fun cancelAll() {
        bulkJob?.cancel()
        bulkJob = null
    }

    /** Removes every downloaded page font; the reader falls back to the bundled font until refetched. */
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        cancelAll()
        jobs.values.forEach { it.cancel() }
        typefaces.evictAll()
        folder.deleteRecursively()
        _ready.value = emptySet()
    }

    private fun progress(): Float = (_ready.value.count { it in 1..PAGES }.toFloat() / PAGES).coerceIn(0f, 1f)

    private fun fetch(page: Int) {
        if (isReady(page) || jobs[page]?.isActive == true) return
        jobs[page] = scope.launch {
            try {
                fetchNow(page)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.i(e, "QCF font %d not fetched", page)
            } finally {
                jobs.remove(page, coroutineContext[Job])
            }
        }
    }

    private suspend fun fetchNow(page: Int) {
        val pin = pins?.let { if (page == NAMES) it.names else it.pages.getOrNull(page - 1) } ?: return
        parallel.withPermit {
            if (isReady(page)) return
            Http.download(urlFor(page), fileFor(page), expectedSha256 = pin.sha256, timeoutMs = 45_000)
        }
        _ready.update { it + page }
    }

    private fun scan(): Set<Int> {
        val pins = pins ?: return emptySet()
        val found = HashSet<Int>()
        if (fileFor(NAMES).length() == pins.names.size) found += NAMES
        for (page in 1..PAGES) {
            if (fileFor(page).length() == pins.pages[page - 1].size) found += page
        }
        return found
    }

    private fun unmetered(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    private fun fileFor(page: Int) = File(folder, if (page == NAMES) "sura_names.ttf" else "p$page.ttf")

    private fun urlFor(page: Int) =
        if (page == NAMES) "https://static.qurancdn.com/fonts/quran/surah-names/v2/sura_names.ttf"
        else "https://static.qurancdn.com/fonts/quran/hafs/v2/ttf/p$page.ttf"

    private class Pin(val size: Long, val sha256: String)

    private class Pins(val pages: List<Pin>, val names: Pin) {
        companion object {
            fun read(context: Context): Pins {
                val root = JSONObject(context.assets.open(ASSET).bufferedReader().use { it.readText() })
                val pages = root.getJSONArray("pages")
                val names = root.getJSONArray("surahNames")
                return Pins(
                    (0 until pages.length()).map { pages.getJSONArray(it).let { p -> Pin(p.getLong(0), p.getString(1)) } },
                    Pin(names.getLong(0), names.getString(1)),
                )
            }
        }
    }

    companion object {
        const val ASSET = "qcf2-fonts.json"
        const val PAGES = 604

        /** Stands for the surah-name font in [ready]. */
        const val NAMES = 0

        private const val TYPEFACES_KEPT = 6
    }
}
