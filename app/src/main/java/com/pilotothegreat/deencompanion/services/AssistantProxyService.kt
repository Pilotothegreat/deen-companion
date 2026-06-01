package com.pilotothegreat.deencompanion.services

import android.content.Context
import com.pilotothegreat.deencompanion.database.HadithDao
import com.pilotothegreat.deencompanion.ui.quran.QuranHelper
import timber.log.Timber
import java.util.Locale

class AssistantProxyService(
    private val context: Context,
    private val hadithDao: HadithDao
) {
    suspend fun query(prompt: String): Pair<String, String> {
        val query = prompt.trim()
        if (query.isEmpty()) {
            return Pair("", "")
        }

        Timber.d("Querying Assistant Proxy for: %s", query)
        
        // 1. Keyword extraction (simple splitting and filtering)
        val stopWords = setOf("a", "an", "the", "and", "or", "but", "about", "for", "on", "in", "to", "of", "tell", "me", "show", "what", "is")
        val keywords = query.lowercase(Locale.US)
            .split(Regex("[^a-zA-Z\\u0621-\\u064A0-9]+"))
            .filter { it.isNotEmpty() && !stopWords.contains(it) }

        if (keywords.isEmpty()) {
            return getFallbackResponse()
        }

        // 2. Query Quran JSON locally
        val surahs = try {
            QuranHelper.getSurahs(context)
        } catch (e: Exception) {
            Timber.e(e, "Error loading Quran helper")
            emptyList()
        }

        var matchedVerse: QuranHelper.Verse? = null
        var matchedSurah: QuranHelper.Surah? = null

        // Try to find the first verse that matches our keywords
        outer@ for (surah in surahs) {
            for (verse in surah.verses) {
                // check translation and text
                val matchesTranslation = keywords.any { kw -> verse.translation.lowercase(Locale.US).contains(kw) }
                val matchesArabic = keywords.any { kw -> verse.text.contains(kw) }
                
                if (matchesTranslation || matchesArabic) {
                    matchedVerse = verse
                    matchedSurah = surah
                    break@outer
                }
            }
        }

        // 3. Query Hadith DB locally
        var matchedHadith: com.pilotothegreat.deencompanion.database.HadithEntity? = null
        for (kw in keywords) {
            val dbMatches = try {
                hadithDao.searchHadithsGlobal("%$kw%")
            } catch (e: Exception) {
                Timber.e(e, "Error querying DB for hadiths")
                emptyList()
            }
            if (dbMatches.isNotEmpty()) {
                matchedHadith = dbMatches.first()
                break
            }
        }

        // 4. Synthesize prompt strictly based on retrieved context
        val arabicResponse = StringBuilder()
        val englishResponse = StringBuilder()

        if (matchedSurah != null && matchedVerse != null) {
            arabicResponse.append("المصدر الموثق من القرآن الكريم:\n")
            arabicResponse.append("سورة ${matchedSurah.name} الآية ${matchedVerse.id}:\n")
            arabicResponse.append("${matchedVerse.text}\n\n")

            englishResponse.append("Verified Source from the Holy Quran:\n")
            englishResponse.append("Surah ${matchedSurah.transliteration} (${matchedSurah.id}) Verse ${matchedVerse.id}:\n")
            englishResponse.append("\"${matchedVerse.translation}\"\n\n")
        }

        if (matchedHadith != null) {
            arabicResponse.append("المصدر الموثق من الحديث الشريف:\n")
            arabicResponse.append("رواه ${matchedHadith.narrator}:\n")
            arabicResponse.append("${matchedHadith.arabic}\n\n")

            englishResponse.append("Verified Source from Hadith:\n")
            englishResponse.append("Narrated by ${matchedHadith.narrator} (Grade: ${matchedHadith.grade}):\n")
            englishResponse.append("\"${matchedHadith.english}\"\n\n")
        }

        if (arabicResponse.isEmpty() && englishResponse.isEmpty()) {
            return getFallbackResponse()
        }

        return Pair(arabicResponse.toString().trim(), englishResponse.toString().trim())
    }

    private fun getFallbackResponse(): Pair<String, String> {
        return Pair(
            "عذراً، لم أجد نصوصاً موثقة مطابقة لسؤالك في قاعدة البيانات المحلية.",
            "Sorry, I could not find any verified sources matching your query in the local offline database."
        )
    }
}
