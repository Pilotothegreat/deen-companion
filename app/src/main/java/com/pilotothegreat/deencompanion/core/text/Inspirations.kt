package com.pilotothegreat.deencompanion.core.text

import java.time.LocalDate
import java.util.Locale

data class Inspiration(
    val arabic: String,
    val english: String,
    val sourceArabic: String,
    val sourceEnglish: String,
) {
    fun text(locale: Locale): String = if (locale.language == "ar") arabic else english
    fun source(locale: Locale): String = if (locale.language == "ar") sourceArabic else sourceEnglish
}

/** One verse or hadith per day, shared by the home screen and the widget. */
object Inspirations {
    val all = listOf(
        Inspiration("فَإِنَّ مَعَ الْعُسْرِ يُسْرًا", "So verily, with hardship, there is ease.", "الشرح: ٥", "Quran 94:5"),
        Inspiration("إِنَّ اللَّهَ مَعَ الصَّابِرِينَ", "Indeed, Allah is with the patient.", "البقرة: ١٥٣", "Quran 2:153"),
        Inspiration("وَوَجَدَكَ ضَالًّا فَهَدَىٰ", "And He found you lost and guided you.", "الضحى: ٧", "Quran 93:7"),
        Inspiration("ادْعُونِي أَسْتَجِبْ لَكُمْ", "Call upon Me; I will answer you.", "غافر: ٦٠", "Quran 40:60"),
        Inspiration("وَرَحْمَتِي وَسِعَتْ كُلَّ شَيْءٍ", "My mercy encompasses all things.", "الأعراف: ١٥٦", "Quran 7:156"),
        Inspiration("فَاذْكُرُونِي أَذْكُرْكُمْ", "Remember Me; I will remember you.", "البقرة: ١٥٢", "Quran 2:152"),
        Inspiration("لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا", "Allah does not burden a soul beyond that it can bear.", "البقرة: ٢٨٦", "Quran 2:286"),
        Inspiration("إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ", "Actions are but by intentions.", "متفق عليه", "Bukhari & Muslim"),
        Inspiration("خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ", "The best of you are those who learn the Quran and teach it.", "صحيح البخاري", "Sahih al-Bukhari"),
        Inspiration("الْكَلِمَةُ الطَّيِّبَةُ صَدَقَةٌ", "A good word is charity.", "متفق عليه", "Bukhari & Muslim"),
    )

    fun forDate(date: LocalDate): Inspiration = all[(date.dayOfYear - 1) % all.size]
}
