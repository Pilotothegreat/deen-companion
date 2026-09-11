package com.pilotothegreat.deencompanion.core.text

import java.time.LocalDate

/** An ayah by surah and number. */
data class AyahRef(val surah: Int, val ayah: Int)

/**
 * A short, self-contained ayah for each day, shared by the Today card and the widget. The text
 * comes from the bundled Quran, so only references live here.
 */
object DailyVerse {
    val all: List<AyahRef> = """
        2:3 2:45 2:110 2:115 2:152 2:153 2:156 2:163 2:186 2:201 2:208 2:269
        3:8 3:17 3:26 3:31 3:92 3:104 3:134 3:139 3:185 3:190 3:193 3:200
        4:28 4:110 6:162 7:23 7:55 7:56 7:199 7:205 8:2 9:51 9:129 10:57 10:62
        11:6 11:115 12:86 12:87 13:28 14:7 14:24 14:41 15:99
        16:18 16:53 16:90 16:97 16:125 16:128 17:9 17:24 17:80 17:82 18:10 18:46 18:109
        20:25 20:46 20:82 20:114 20:124 20:130 21:35 21:87 21:107 23:1 23:118
        25:58 25:63 25:74 26:80 27:62 28:24 29:2 29:45 29:69 30:21 31:17 31:18
        33:21 33:41 33:56 35:2 35:15 39:10 39:36 39:53 40:44 40:60 41:30 41:34 42:19
        47:7 49:10 49:13 50:16 51:56 52:48 53:39 55:13 55:60 59:18 59:19 62:10
        64:11 64:16 65:3 67:2 70:5 71:10 73:8 85:14 87:14 93:3 93:5 93:7 94:5 94:6 94:8
        96:1 99:7 103:3 108:1 110:3 112:1
    """.trim().split(Regex("\\s+")).map { ref -> ref.split(':').let { AyahRef(it[0].toInt(), it[1].toInt()) } }

    /** Coprime with the list size, so consecutive days jump around the list and still visit every ayah. */
    internal const val STRIDE = 53L

    fun forDate(date: LocalDate): AyahRef = all[Math.floorMod(date.toEpochDay() * STRIDE, all.size.toLong()).toInt()]
}
