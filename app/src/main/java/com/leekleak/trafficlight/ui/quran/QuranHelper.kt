package com.leekleak.trafficlight.ui.quran

import android.content.Context
import com.leekleak.trafficlight.R
import org.json.JSONArray
import org.json.JSONObject

object QuranHelper {

    data class Verse(
        val id: Int,
        val text: String,       // Arabic text
        val translation: String // English translation
    )

    data class Surah(
        val id: Int,
        val name: String,           // Arabic name
        val transliteration: String, // English transliteration
        val type: String,           // "meccan" or "medinan"
        val totalVerses: Int,
        val verses: List<Verse>
    )

    data class JuzInfo(
        val juzNumber: Int,
        val startSurah: Int,
        val startVerse: Int,
        val endSurah: Int,
        val endVerse: Int
    )

    // Juz boundaries: (startSurah, startVerse) pairs for each of the 30 Juz
    private val juzBoundaries = listOf(
        JuzInfo(1, 1, 1, 2, 141),
        JuzInfo(2, 2, 142, 2, 252),
        JuzInfo(3, 2, 253, 3, 92),
        JuzInfo(4, 3, 93, 4, 23),
        JuzInfo(5, 4, 24, 4, 147),
        JuzInfo(6, 4, 148, 5, 81),
        JuzInfo(7, 5, 82, 6, 110),
        JuzInfo(8, 6, 111, 7, 87),
        JuzInfo(9, 7, 88, 8, 40),
        JuzInfo(10, 8, 41, 9, 92),
        JuzInfo(11, 9, 93, 11, 5),
        JuzInfo(12, 11, 6, 12, 52),
        JuzInfo(13, 12, 53, 14, 52),
        JuzInfo(14, 15, 1, 16, 128),
        JuzInfo(15, 17, 1, 18, 74),
        JuzInfo(16, 18, 75, 20, 135),
        JuzInfo(17, 21, 1, 22, 78),
        JuzInfo(18, 23, 1, 25, 20),
        JuzInfo(19, 25, 21, 27, 55),
        JuzInfo(20, 27, 56, 29, 45),
        JuzInfo(21, 29, 46, 33, 30),
        JuzInfo(22, 33, 31, 36, 27),
        JuzInfo(23, 36, 28, 39, 31),
        JuzInfo(24, 39, 32, 41, 46),
        JuzInfo(25, 41, 47, 45, 37),
        JuzInfo(26, 46, 1, 51, 30),
        JuzInfo(27, 51, 31, 57, 29),
        JuzInfo(28, 58, 1, 66, 12),
        JuzInfo(29, 67, 1, 77, 50),
        JuzInfo(30, 78, 1, 114, 6)
    )

    @Volatile
    private var cachedSurahs: List<Surah>? = null

    fun getSurahs(context: Context): List<Surah> {
        cachedSurahs?.let { return it }
        synchronized(this) {
            cachedSurahs?.let { return it }
            val json = context.resources.openRawResource(R.raw.quran)
                .bufferedReader()
                .use { it.readText() }
            val array = JSONArray(json)
            val surahs = mutableListOf<Surah>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val verses = mutableListOf<Verse>()
                val versesArray = obj.getJSONArray("verses")
                for (j in 0 until versesArray.length()) {
                    val vObj = versesArray.getJSONObject(j)
                    verses.add(
                        Verse(
                            id = vObj.getInt("id"),
                            text = vObj.getString("text"),
                            translation = vObj.getString("translation")
                        )
                    )
                }
                surahs.add(
                    Surah(
                        id = obj.getInt("id"),
                        name = obj.getString("name"),
                        transliteration = obj.getString("transliteration"),
                        type = obj.getString("type"),
                        totalVerses = obj.getInt("total_verses"),
                        verses = verses
                    )
                )
            }
            cachedSurahs = surahs
            return surahs
        }
    }

    fun getVersesForJuz(context: Context, juzNumber: Int): List<Pair<Surah, Verse>> {
        val surahs = getSurahs(context)
        val juz = juzBoundaries.getOrNull(juzNumber - 1) ?: return emptyList()
        val result = mutableListOf<Pair<Surah, Verse>>()

        for (surah in surahs) {
            if (surah.id < juz.startSurah || surah.id > juz.endSurah) continue

            for (verse in surah.verses) {
                val isInRange = when {
                    surah.id == juz.startSurah && surah.id == juz.endSurah ->
                        verse.id >= juz.startVerse && verse.id <= juz.endVerse
                    surah.id == juz.startSurah -> verse.id >= juz.startVerse
                    surah.id == juz.endSurah -> verse.id <= juz.endVerse
                    else -> true
                }
                if (isInRange) {
                    result.add(Pair(surah, verse))
                }
            }
        }

        return result
    }

    fun isSajdahVerse(surahId: Int, verseId: Int): Boolean {
        return when (surahId) {
            7 -> verseId == 206
            13 -> verseId == 15
            16 -> verseId == 49
            17 -> verseId == 109
            19 -> verseId == 58
            22 -> verseId == 18 || verseId == 77
            25 -> verseId == 60
            27 -> verseId == 26
            32 -> verseId == 15
            38 -> verseId == 24
            41 -> verseId == 38
            53 -> verseId == 62
            84 -> verseId == 21
            96 -> verseId == 19
            else -> false
        }
    }
}
