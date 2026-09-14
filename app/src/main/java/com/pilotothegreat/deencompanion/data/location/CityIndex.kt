package com.pilotothegreat.deencompanion.data.location

import com.pilotothegreat.deencompanion.core.text.ArabicText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.Normalizer
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class City(
    val name: String,
    val nameArabic: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneId: String,
    val population: Int,
) {
    fun displayName(locale: Locale): String = if (locale.language == "ar" && nameArabic.isNotBlank()) nameArabic else name

    fun countryName(locale: Locale): String = CityIndex.countryName(countryCode, locale)

    /** "Nizwa, Oman" or "نزوى، عُمان". */
    fun label(locale: Locale): String =
        displayName(locale) + (if (locale.language == "ar") "، " else ", ") + countryName(locale)
}

/** Offline search over the bundled GeoNames city list (assets/cities.json). */
class CityIndex(private val readJson: () -> String) {

    private class Loaded(val cities: List<City>, val names: List<String>, val arabicNames: List<String>, val countryNames: Map<String, List<String>>)

    private val mutex = Mutex()

    @Volatile
    private var loaded: Loaded? = null

    private suspend fun data(): Loaded = loaded ?: mutex.withLock {
        loaded ?: withContext(Dispatchers.IO) { index(parse(readJson())) }.also { loaded = it }
    }

    /** Cities whose name (or country) matches [query], best matches and biggest cities first. */
    suspend fun search(query: String, limit: Int = 40): List<City> {
        val data = data()
        val folded = fold(query.trim())
        if (folded.isEmpty()) return emptyList()
        return withContext(Dispatchers.Default) {
            val countries = if (folded.length >= 3) {
                data.countryNames.filterValues { names -> names.any { it.startsWith(folded) } }.keys
            } else {
                emptySet()
            }
            data.cities.indices
                .mapNotNull { i ->
                    val name = data.names[i]
                    val arabic = data.arabicNames[i]
                    val score = when {
                        name.startsWith(folded) || arabic.startsWith(folded) -> 0
                        name.split(' ', '-').any { it.startsWith(folded) } || arabic.split(' ').any { it.startsWith(folded) } -> 1
                        name.contains(folded) || arabic.contains(folded) -> 2
                        data.cities[i].countryCode in countries -> 3
                        else -> return@mapNotNull null
                    }
                    score to i
                }
                .sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenByDescending { data.cities[it.second].population })
                .take(limit)
                .map { data.cities[it.second] }
        }
    }

    /** The biggest cities in [countryCode], or in the world when it's unknown. */
    suspend fun suggestions(countryCode: String?, limit: Int = 30): List<City> {
        val cities = data().cities
        val inCountry = countryCode?.let { code -> cities.filter { it.countryCode.equals(code, ignoreCase = true) } }.orEmpty()
        return inCountry.ifEmpty { cities }.take(limit)
    }

    /** The closest city within [withinKm], used to name places when the geocoder can't. */
    suspend fun nearest(latitude: Double, longitude: Double, withinKm: Double = 50.0): City? {
        val cities = data().cities
        return withContext(Dispatchers.Default) {
            cities.minByOrNull { distanceKm(latitude, longitude, it.latitude, it.longitude) }
                ?.takeIf { distanceKm(latitude, longitude, it.latitude, it.longitude) <= withinKm }
        }
    }

    private fun index(cities: List<City>) = Loaded(
        cities = cities,
        names = cities.map { fold(it.name) },
        arabicNames = cities.map { fold(it.nameArabic) },
        countryNames = cities.map { it.countryCode }.distinct().associateWith { code ->
            listOf(Locale.ENGLISH, ARABIC).map { fold(countryName(code, it)) }
        },
    )

    companion object {
        private val ARABIC: Locale = Locale.forLanguageTag("ar")
        private val MARKS = Regex("\\p{Mn}+")
        private val PUNCTUATION = Regex("[’'`ʻʼ.]")
        private const val EARTH_RADIUS_KM = 6371.0

        fun parse(json: String): List<City> =
            Json.parseToJsonElement(json).jsonObject.getValue("cities").jsonArray.map { element ->
                val row = element.jsonArray
                City(
                    name = row[0].jsonPrimitive.content,
                    nameArabic = row[1].jsonPrimitive.content,
                    countryCode = row[2].jsonPrimitive.content,
                    latitude = row[3].jsonPrimitive.double,
                    longitude = row[4].jsonPrimitive.double,
                    timezoneId = row[5].jsonPrimitive.content,
                    population = row[6].jsonPrimitive.int,
                )
            }

        /** Folds case, Latin accents and Arabic diacritics/letter variants so "Nizwá" and "nizwa" match. */
        fun fold(text: String): String =
            ArabicText.normalize(Normalizer.normalize(text, Normalizer.Form.NFD).replace(MARKS, ""))
                .replace(PUNCTUATION, "")
                .lowercase(Locale.ROOT)

        fun countryName(countryCode: String, locale: Locale): String =
            runCatching { Locale.Builder().setRegion(countryCode).build().getDisplayCountry(locale) }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: countryCode

        fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dPhi = Math.toRadians(lat2 - lat1)
            val dLambda = Math.toRadians(lon2 - lon1)
            val a = sin(dPhi / 2) * sin(dPhi / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLambda / 2) * sin(dLambda / 2)
            return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
        }
    }
}
