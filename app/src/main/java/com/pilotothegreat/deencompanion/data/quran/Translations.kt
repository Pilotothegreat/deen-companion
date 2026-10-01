package com.pilotothegreat.deencompanion.data.quran

/** Where the Arabic text comes from, and the terms it comes under. */
data class TextSource(val name: String, val source: String, val terms: String)

/** A translation and the credit it must carry. */
data class TranslationInfo(
    val id: String,
    val name: String,
    val translator: String,
    val language: String,
    val license: String,
    val source: String,
    /** The exact wording the licence asks to be shown. */
    val attribution: String,
) {
    val isArabic: Boolean get() = language == "ar"
}

/**
 * The translations the app ships. Each one is redistributable on stated terms and is credited in
 * the reader and in About; an unattributed translation is a licensing problem, not a detail.
 */
object Translations {
    const val DEFAULT_ID = "clearquran"

    val CLEAR_QURAN = TranslationInfo(
        id = DEFAULT_ID,
        name = "The Clear Quran",
        translator = "Talal Itani",
        language = "en",
        license = "CC BY-ND 4.0",
        source = "https://clearquran.com",
        attribution = "Translation by Talal Itani, ClearQuran.com",
    )

    val all: List<TranslationInfo> = listOf(CLEAR_QURAN)

    fun byId(id: String?): TranslationInfo = all.firstOrNull { it.id == id } ?: CLEAR_QURAN

    /** The asset holding [id]'s verses, laid out surah by surah. */
    internal fun assetFor(id: String): String = "quran-tr-$id.json"
}
