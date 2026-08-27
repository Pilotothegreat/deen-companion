package com.pilotothegreat.deencompanion.ui.hadith

import android.content.Context
import com.pilotothegreat.deencompanion.R
import org.json.JSONObject

object HadithHelper {

    data class Hadith(
        val number: Int,
        val arabic: String,
        val english: String,
        val narrator: String,
        val grade: String
    )

    data class HadithCollection(
        val name: String,
        val compiler: String,
        val hadiths: List<Hadith>
    )

    @Volatile
    private var cachedCollections: List<HadithCollection>? = null

    fun getCollections(context: Context): List<HadithCollection> {
        cachedCollections?.let { return it }
        synchronized(this) {
            cachedCollections?.let { return it }
            try {
                val json = context.resources.openRawResource(R.raw.hadiths)
                    .bufferedReader()
                    .use { it.readText() }
                val root = JSONObject(json)
                val collectionsArray = root.getJSONArray("collections")
                val collections = mutableListOf<HadithCollection>()
                for (i in 0 until collectionsArray.length()) {
                    val colObj = collectionsArray.getJSONObject(i)
                    val hadithsList = mutableListOf<Hadith>()
                    val hadithsArray = colObj.getJSONArray("hadiths")
                    for (j in 0 until hadithsArray.length()) {
                        val hObj = hadithsArray.getJSONObject(j)
                        hadithsList.add(
                            Hadith(
                                number = hObj.getInt("number"),
                                arabic = hObj.getString("arabic"),
                                english = hObj.getString("english"),
                                narrator = hObj.getString("narrator"),
                                grade = hObj.getString("grade")
                            )
                        )
                    }
                    collections.add(
                        HadithCollection(
                            name = colObj.getString("name"),
                            compiler = colObj.getString("compiler"),
                            hadiths = hadithsList
                        )
                    )
                }
                cachedCollections = collections
                return collections
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Error loading local Hadith collections")
                return emptyList()
            }
        }
    }

    fun searchHadiths(context: Context, query: String): List<Pair<String, Hadith>> {
        if (query.isBlank()) return emptyList()
        val collections = getCollections(context)
        val result = mutableListOf<Pair<String, Hadith>>()
        val lowerQuery = query.trim().lowercase()
        val normalizedQuery = com.pilotothegreat.deencompanion.util.normalizeArabic(query)
        for (col in collections) {
            for (hadith in col.hadiths) {
                val normalizedArabic = com.pilotothegreat.deencompanion.util.normalizeArabic(hadith.arabic)
                if (hadith.english.lowercase().contains(lowerQuery) ||
                    normalizedArabic.contains(normalizedQuery, ignoreCase = true) ||
                    hadith.narrator.lowercase().contains(lowerQuery)
                ) {
                    result.add(Pair(col.name, hadith))
                }
            }
        }
        return result
    }
}

