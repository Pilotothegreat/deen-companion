package com.pilotothegreat.deencompanion.database

import android.content.Context
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.ui.hadith.HadithHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.URL

class HadithRepository(
    private val context: Context,
    private val hadithDao: HadithDao
) {

    init {
        val isRobolectric = try {
            Class.forName("org.robolectric.Robolectric") != null
        } catch (e: Exception) {
            false
        }
        if (!isRobolectric) {
            // Run database pre-population in a background dispatcher on initialization
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launchPrepopulation()
        }
    }

    private fun kotlinx.coroutines.CoroutineScope.launchPrepopulation() {
        this.launch {
            try {
                populateIfEmpty()
            } catch (e: Exception) {
                Timber.e(e, "Error pre-populating Hadith database")
            }
        }
    }

    private suspend fun populateIfEmpty() {
        val count = hadithDao.getHadithCount("bukhari")
        if (count == 0) {
            Timber.i("Hadith database is empty. Populating from raw JSON...")
            val json = context.resources.openRawResource(R.raw.hadiths)
                .bufferedReader()
                .use { it.readText() }
            val root = JSONObject(json)
            val collectionsArray = root.getJSONArray("collections")
            
            val books = mutableListOf<HadithBookEntity>()
            val hadiths = mutableListOf<HadithEntity>()

            for (i in 0 until collectionsArray.length()) {
                val colObj = collectionsArray.getJSONObject(i)
                val rawName = colObj.getString("name")
                val bookId = when {
                    rawName.contains("Bukhari", ignoreCase = true) -> "bukhari"
                    rawName.contains("Muslim", ignoreCase = true) -> "muslim"
                    rawName.contains("Tirmidhi", ignoreCase = true) -> "tirmidhi"
                    rawName.contains("Dawud", ignoreCase = true) -> "abudawud"
                    rawName.contains("Nasa", ignoreCase = true) -> "nasai"
                    rawName.contains("Majah", ignoreCase = true) -> "ibnmajah"
                    else -> rawName.lowercase().replace(" ", "_")
                }

                val hadithsArray = colObj.getJSONArray("hadiths")
                books.add(
                    HadithBookEntity(
                        id = bookId,
                        name = rawName,
                        compiler = colObj.getString("compiler"),
                        hadithCount = hadithsArray.length()
                    )
                )

                for (j in 0 until hadithsArray.length()) {
                    val hObj = hadithsArray.getJSONObject(j)
                    val num = hObj.getInt("number")
                    hadiths.add(
                        HadithEntity(
                            id = "${bookId}_$num",
                            bookId = bookId,
                            number = num,
                            arabic = hObj.getString("arabic"),
                            english = hObj.getString("english"),
                            narrator = hObj.getString("narrator"),
                            grade = hObj.getString("grade")
                        )
                    )
                }
            }

            hadithDao.insertHadithBooks(books)
            hadithDao.insertHadiths(hadiths)
            Timber.i("Successfully inserted ${books.size} books and ${hadiths.size} hadiths from local fallback.")
        }
    }

    fun getHadithBooks(): Flow<List<HadithBookEntity>> = hadithDao.getHadithBooks()

    suspend fun getHadithsPaged(bookId: String, page: Int, pageSize: Int): List<HadithEntity> {
        val offset = page * pageSize
        return hadithDao.getHadithsPaged(bookId, pageSize, offset)
    }

    fun getFavorites(): Flow<List<HadithEntity>> = hadithDao.getFavoritesFlow()

    suspend fun getHadithCount(bookId: String): Int = withContext(Dispatchers.IO) {
        hadithDao.getHadithCount(bookId)
    }

    suspend fun toggleFavorite(hadithId: String, isFav: Boolean) {
        hadithDao.updateFavorite(hadithId, isFav)
    }

    suspend fun searchHadiths(query: String, bookId: String? = null): List<HadithEntity> {
        val cleanQuery = "%${query.trim()}%"
        return if (bookId != null) {
            hadithDao.searchHadithsInBook(bookId, cleanQuery)
        } else {
            hadithDao.searchHadithsGlobal(cleanQuery)
        }
    }

    /**
     * Async task to download and cache the entire collection from the CDN API
     */
    suspend fun syncFullBook(bookId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val book = hadithDao.getHadithBook(bookId) ?: return@withContext false
            Timber.i("Syncing full book: ${book.name} from Fawaz Ahmed Hadith API...")

            val araUrl = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-$bookId.json"
            val engUrl = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/eng-$bookId.json"

            val araConnection = URL(araUrl).openConnection() as java.net.HttpURLConnection
            araConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            araConnection.connectTimeout = 15000
            araConnection.readTimeout = 15000
            val araJson = araConnection.inputStream.bufferedReader().use { it.readText() }

            val engConnection = URL(engUrl).openConnection() as java.net.HttpURLConnection
            engConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            engConnection.connectTimeout = 15000
            engConnection.readTimeout = 15000
            val engJson = engConnection.inputStream.bufferedReader().use { it.readText() }

            val araRoot = JSONObject(araJson)
            val engRoot = JSONObject(engJson)

            val araArray = araRoot.getJSONArray("hadiths")
            val engArray = engRoot.getJSONArray("hadiths")

            val count = minOf(araArray.length(), engArray.length())
            val entities = mutableListOf<HadithEntity>()

            for (i in 0 until count) {
                val araObj = araArray.getJSONObject(i)
                val engObj = engArray.getJSONObject(i)

                val num = araObj.optInt("hadithnumber", i + 1)
                val arabText = araObj.optString("text", "")
                val engText = engObj.optString("text", "")

                entities.add(
                    HadithEntity(
                        id = "${bookId}_$num",
                        bookId = bookId,
                        number = num,
                        arabic = arabText,
                        english = engText,
                        narrator = "", // Fawaz Ahmed's API embeds the narrator inside english text
                        grade = "Sahih" // Default grade for canonical collections
                    )
                )
            }

            hadithDao.insertHadiths(entities)
            
            // Update book metadata with new count
            hadithDao.insertHadithBooks(
                listOf(
                    HadithBookEntity(
                        id = book.id,
                        name = book.name,
                        compiler = book.compiler,
                        hadithCount = count
                    )
                )
            )
            Timber.i("Finished syncing full book: ${book.name} containing $count items.")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync full book: $bookId")
            false
        }
    }
}
