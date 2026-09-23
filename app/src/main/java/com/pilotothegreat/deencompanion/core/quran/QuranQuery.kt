package com.pilotothegreat.deencompanion.core.quran

import androidx.annotation.StringRes
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.text.ArabicText
import com.pilotothegreat.deencompanion.core.text.Numerals

/**
 * A passage the Quran is quoted by rather than by its number.
 *
 * Someone looking for Ayat al-Kursi types "ayat al kursi", not "2:255", and the words of the ayah are
 * not in the name — no amount of text search finds it. Each one lists the spellings people actually
 * write, in both scripts, folded the way [QuranQuery] folds the query.
 */
enum class NamedPassage(
    @get:StringRes val label: Int,
    val surah: Int,
    val ayah: Int,
    /** The last ayah of the passage, for the ones that are more than one. */
    val lastAyah: Int = ayah,
    vararg val aliases: String,
) {
    AYAT_AL_KURSI(
        R.string.passage_ayat_al_kursi, 2, 255,
        aliases = arrayOf("ayat al kursi", "ayatul kursi", "ayat ul kursi", "aytul kursi", "ayat kursi", "kursi", "throne verse", "verse of the throne", "ايه الكرسي", "ايت الكرسي", "الكرسي"),
    ),
    KHAWATIM_AL_BAQARAH(
        R.string.passage_khawatim_al_baqarah, 2, 285, 286,
        aliases = arrayOf("khawatim al baqarah", "khawatim albaqarah", "last two verses of al baqarah", "last two ayahs of al baqarah", "amanar rasul", "amana al rasul", "خواتيم البقره", "خواتيم سوره البقره", "امن الرسول", "اخر ايتين من البقره"),
    ),
    AYAT_AN_NUR(
        R.string.passage_ayat_an_nur, 24, 35,
        aliases = arrayOf("ayat an nur", "ayat al nur", "verse of light", "light verse", "ايه النور", "ايت النور"),
    ),
    AYAT_AD_DAYN(
        R.string.passage_ayat_ad_dayn, 2, 282,
        aliases = arrayOf("ayat ad dayn", "ayat al dayn", "verse of debt", "longest verse", "ايه الدين", "ايت الدين", "اطول ايه"),
    ),
    MU_AWWIDHATAYN(
        R.string.passage_mu_awwidhatayn, 113, 1, 6,
        aliases = arrayOf("muawwidhatayn", "mu awwidhatayn", "muawwidhatain", "the two refuges", "المعوذتين", "المعوذتان"),
    ),
    THREE_QULS(
        R.string.passage_three_quls, 112, 1, 6,
        aliases = arrayOf("three quls", "the three quls", "3 quls", "quls", "المعوذات", "القلاقل"),
    ),
    ;

    val isSingleAyah: Boolean get() = lastAyah == ayah
}

/**
 * The other names people write surahs by.
 *
 * The mushaf's own transliteration is one spelling of many: Ya-Sin is typed "yaseen", al-Mulk is known
 * as Tabarak, al-Ikhlas as at-Tawhid. Keys are folded the way [QuranQuery.fold] folds a query, with the
 * spaces taken out.
 */
object SurahNames {
    val ALIASES: Map<String, Int> = mapOf(
        "fatiha" to 1, "alfatiha" to 1, "ummalkitab" to 1, "امالكتاب" to 1, "السبعالمثاني" to 1,
        "baqara" to 2, "albaqara" to 2, "bakara" to 2, "albakara" to 2,
        "imran" to 3, "aleimran" to 3, "alimran" to 3, "آلعمران" to 3, "الimran" to 3,
        "maida" to 5, "almaida" to 5,
        "anfal" to 8, "tawba" to 9, "bara'a" to 9, "براءه" to 9,
        "isra" to 17, "alisra" to 17, "baniisrail" to 17, "بنياسراىيل" to 17, "بنياسرائيل" to 17,
        "kahaf" to 18, "taha" to 20, "طه" to 20,
        "rum" to 30, "room" to 30, "alrum" to 30,
        "sajda" to 32, "yaseen" to 36, "yasin" to 36, "yasseen" to 36, "ياسين" to 36,
        "dukhan" to 44, "rahmaan" to 55, "الرحمان" to 55,
        "waqia" to 56, "alwaqia" to 56, "waqiah" to 56,
        "hashr" to 59, "jumua" to 62, "aljumua" to 62,
        "mulk" to 67, "tabarak" to 67, "تبارك" to 67,
        "qalam" to 68, "noon" to 68,
        "insan" to 76, "dahr" to 76, "الدهر" to 76,
        "duha" to 93, "alduha" to 93,
        "bayyina" to 98, "zalzala" to 99, "fil" to 105, "alfil" to 105,
        "quraysh" to 106, "quraish" to 106, "kawthar" to 108, "alkawthar" to 108,
        "nasr" to 110, "masad" to 111, "lahab" to 111, "اللهب" to 111,
        "tawhid" to 112, "altawhid" to 112, "التوحيد" to 112, "samad" to 112, "الصمد" to 112,
    )
}

/** A place in the mushaf that a query names outright, rather than something to look for in the text. */
sealed interface QuranDestination {
    /** One ayah, and the passage it is the opening of when the query asked for one by name. */
    data class Ayah(val surah: Int, val ayah: Int, val passage: NamedPassage? = null) : QuranDestination

    data class SurahStart(val surah: Int) : QuranDestination

    data class Page(val number: Int) : QuranDestination

    data class Juz(val number: Int) : QuranDestination
}

/**
 * What a query turned out to mean: the places it names, and whatever is left of it to look for.
 *
 * Both halves are used. "the verse about patience" names nothing and is searched whole; "ayat al kursi"
 * names an ayah and is also searched, because a search that throws away the words it recognised cannot
 * show anything else that matches them.
 */
data class ParsedQuery(val destinations: List<QuranDestination>, val text: String)

/**
 * Reads a line typed into the Quran search field.
 *
 * It handles the three ways people ask for an ayah — by name ("ayat al kursi"), by reference ("2:255",
 * "surah 2 ayah 255", "al baqarah 255", "juz 30", "page 604"), and in a sentence ("show me the verse
 * about patience") — and leaves the rest as plain text for the search that was always there.
 */
object QuranQuery {

    /** The most destinations a query can produce; beyond a handful the list stops being a shortcut. */
    private const val MAX_DESTINATIONS = 4

    /**
     * Words that only frame the question. Stripping them is what lets a sentence be searched: the ayah
     * is about patience, it does not contain the words "show me the verse about".
     */
    private val FILLER = setOf(
        "show", "me", "the", "a", "an", "please", "find", "search", "look", "for", "read", "open", "go",
        "to", "where", "is", "are", "what", "which", "about", "on", "in", "of", "from", "quran", "qur'an",
        "koran", "number", "no", "verse", "verses", "ayah", "ayat", "aya", "ayahs", "surah", "surat",
        "sura", "chapter", "page", "juz", "juzz", "para",
        "اقرا", "ابحث", "افتح", "اين", "ما", "هي", "عن", "في", "من", "القران", "رقم", "ايه", "ايات",
        "الايه", "سوره", "السوره", "جزء", "الجزء", "صفحه", "الصفحه", "اريد", "لي",
    )

    /** The words that introduce a number, so "juz 30" is a juz and "page 30" is a page. */
    private val JUZ_WORDS = setOf("juz", "juzz", "para", "جزء", "الجزء")
    private val PAGE_WORDS = setOf("page", "صفحه", "الصفحه", "ص")
    private val SURAH_WORDS = setOf("surah", "surat", "sura", "chapter", "سوره", "السوره")
    private val AYAH_WORDS = setOf("ayah", "ayat", "aya", "verse", "ايه", "الايه", "ايات")

    private val REFERENCE = Regex("""(\d{1,3})\s*[:.\-/]\s*(\d{1,3})""")
    private val NUMBER = Regex("""\d{1,3}""")

    /**
     * @param verseCount how many ayahs a surah has, for checking a reference points at one
     * @param surahNamed the surah a name or a common spelling of one belongs to, or null
     */
    fun parse(
        raw: String,
        verseCount: (Int) -> Int,
        surahNamed: (String) -> Int?,
    ): ParsedQuery {
        val folded = fold(raw)
        if (folded.isEmpty()) return ParsedQuery(emptyList(), raw.trim())
        val destinations = LinkedHashSet<QuranDestination>()

        namedPassages(folded).forEach { destinations += QuranDestination.Ayah(it.surah, it.ayah, it) }
        references(folded, verseCount).forEach { destinations += it }
        byName(folded, verseCount, surahNamed)?.let { destinations += it }
        numbered(folded).forEach { destinations += it }
        bareNumber(folded).forEach { destinations += it }

        return ParsedQuery(destinations.take(MAX_DESTINATIONS), textOf(raw, folded))
    }

    /** Lowercased, in ASCII digits, without Arabic diacritics or the punctuation between words. */
    fun fold(raw: String): String {
        val ascii = Numerals.toAscii(raw)
        val normalized = ArabicText.normalize(ascii)
        return buildString(normalized.length) {
            for (c in normalized) {
                when {
                    c.isLetterOrDigit() -> append(c)
                    // A reference keeps its separator; everything else becomes a space.
                    c == ':' || c == '/' || c == '.' || c == '-' -> append(c)
                    else -> append(' ')
                }
            }
        }.trim().replace(Regex("\\s+"), " ")
    }

    /** A passage whose name the query carries, or which it has begun to spell out. */
    private fun namedPassages(folded: String): List<NamedPassage> = NamedPassage.entries.filter { passage ->
        passage.aliases.any { alias ->
            val name = fold(alias)
            folded.contains(name) || (folded.length >= PREFIX_FROM && name.startsWith(folded))
        }
    }

    /** "2:255", and "surah 2 ayah 255" written out. */
    private fun references(folded: String, verseCount: (Int) -> Int): List<QuranDestination> {
        val found = mutableListOf<QuranDestination>()
        REFERENCE.findAll(folded).forEach { match ->
            val surah = match.groupValues[1].toInt()
            val ayah = match.groupValues[2].toInt()
            if (surah in 1..114 && ayah in 1..verseCount(surah)) found += QuranDestination.Ayah(surah, ayah)
        }
        val words = folded.split(' ')
        words.forEachIndexed { index, word ->
            if (word !in SURAH_WORDS) return@forEachIndexed
            val surah = words.getOrNull(index + 1)?.toIntOrNull()?.takeIf { it in 1..114 } ?: return@forEachIndexed
            // "surah 2 ayah 255", with or without the word for ayah between them.
            val rest = words.drop(index + 2)
            val ayah = rest.firstOrNull { it.toIntOrNull() != null }?.toIntOrNull()
                ?.takeIf { rest.first() in AYAH_WORDS || rest.first().toIntOrNull() != null }
                ?.takeIf { it in 1..verseCount(surah) }
            found += if (ayah != null) QuranDestination.Ayah(surah, ayah) else QuranDestination.SurahStart(surah)
        }
        return found
    }

    /** "al baqarah", and "al baqarah 255" or "البقرة ٢٥٥". */
    private fun byName(folded: String, verseCount: (Int) -> Int, surahNamed: (String) -> Int?): QuranDestination? {
        val trailing = NUMBER.find(folded.substringAfterLast(' '))?.value?.toIntOrNull()
        val withoutNumber = folded.replace(NUMBER, " ").replace(Regex("\\s+"), " ").trim()
        val name = withoutNumber.split(' ').filterNot { it in FILLER || it.isEmpty() }.joinToString(" ")
        val surah = surahNamed(name.ifEmpty { withoutNumber }) ?: return null
        val ayah = trailing?.takeIf { it in 1..verseCount(surah) }
        return if (ayah != null) QuranDestination.Ayah(surah, ayah) else QuranDestination.SurahStart(surah)
    }

    /** "juz 30" and "page 604", in either language. */
    private fun numbered(folded: String): List<QuranDestination> {
        val words = folded.split(' ')
        val found = mutableListOf<QuranDestination>()
        words.forEachIndexed { index, word ->
            val number = words.getOrNull(index + 1)?.toIntOrNull() ?: return@forEachIndexed
            when {
                word in JUZ_WORDS && number in 1..30 -> found += QuranDestination.Juz(number)
                word in PAGE_WORDS && number in 1..PAGE_COUNT -> found += QuranDestination.Page(number)
            }
        }
        return found
    }

    /** A number on its own is a surah, and then a page: "36" is Ya-Sin before it is a page of al-Anfal. */
    private fun bareNumber(folded: String): List<QuranDestination> {
        val number = folded.toIntOrNull() ?: return emptyList()
        return listOfNotNull(
            QuranDestination.SurahStart(number).takeIf { number in 1..114 },
            QuranDestination.Page(number).takeIf { number in 1..PAGE_COUNT },
        )
    }

    /**
     * What is left to look for in the text.
     *
     * The framing words go, and if that empties the query it is searched as it was typed: someone who
     * types "the" means "the", however unhelpful the result.
     */
    private fun textOf(raw: String, folded: String): String {
        val kept = folded.split(' ').filterNot { it in FILLER || it.isBlank() }
        val text = kept.joinToString(" ")
        return if (text.length >= MIN_TEXT) text else raw.trim()
    }

    /** Below this many characters a query is too short to be treated as the start of a passage's name. */
    private const val PREFIX_FROM = 3

    /** A residue shorter than this is a leftover, not a search. */
    private const val MIN_TEXT = 2

    private const val PAGE_COUNT = KhatmaPlan.PAGE_COUNT
}
