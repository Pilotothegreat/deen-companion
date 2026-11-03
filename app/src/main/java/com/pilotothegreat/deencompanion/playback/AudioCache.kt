package com.pilotothegreat.deencompanion.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * The recitation cache. Every ayah used to be fetched again each time it played, so listening to a
 * surah twice cost twice the data and nothing worked without a connection. Ayahs now stay on disk
 * up to a fixed budget, least-recently-used first out.
 *
 * SimpleCache locks its directory, so exactly one instance may exist per process.
 */
@OptIn(UnstableApi::class)
object AudioCache {

    private const val DIRECTORY = "recitation"

    @Volatile private var cache: SimpleCache? = null

    /**
     * The size the cache is built with. 2.1 removed the setting for it: a number of megabytes is not a
     * choice anyone makes knowingly, and the space comes back from the reciter sheet in the player.
     */
    const val BUDGET_BYTES: Long = 512L * 1024 * 1024

    /** Kept in files rather than the cache directory, so an offline surah survives a cleanup. */
    private fun directory(context: Context) = File(context.filesDir, DIRECTORY)

    fun get(context: Context, maxBytes: Long): SimpleCache = cache ?: synchronized(this) {
        cache ?: SimpleCache(
            directory(context),
            LeastRecentlyUsedCacheEvictor(maxBytes),
            StandaloneDatabaseProvider(context),
        ).also { cache = it }
    }

    /** Bytes currently held, for the settings screen. */
    fun sizeBytes(context: Context): Long = cache?.cacheSpace ?: directory(context).walkBottomUp()
        .filter { it.isFile }
        .sumOf { it.length() }

    /** Empties the cache without releasing it, so playback can continue afterwards. */
    fun clear() {
        val open = cache ?: return
        open.keys.toList().forEach { key ->
            open.getCachedSpans(key).forEach { span -> runCatching { open.removeSpan(span) } }
        }
    }

    /** Releases the directory lock; only for tests and process teardown. */
    fun release() = synchronized(this) {
        cache?.release()
        cache = null
    }
}
