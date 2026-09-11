package com.pilotothegreat.deencompanion.ui.common

import android.content.Context
import android.text.format.DateFormat
import androidx.annotation.StringRes
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.prayer.AsrSchool
import com.pilotothegreat.deencompanion.core.prayer.CalculationMethod
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.tasbih.Dhikr
import com.pilotothegreat.deencompanion.core.text.Numerals
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.util.Locale

@get:StringRes
val Prayer.nameRes: Int
    get() = when (this) {
        Prayer.FAJR -> R.string.fajr
        Prayer.SUNRISE -> R.string.sunrise
        Prayer.DHUHR -> R.string.dhuhr
        Prayer.ASR -> R.string.asr
        Prayer.MAGHRIB -> R.string.maghrib
        Prayer.ISHA -> R.string.isha
    }

@get:StringRes
val Dhikr.labelRes: Int
    get() = when (this) {
        Dhikr.SUBHAN_ALLAH -> R.string.tasbih_dhikr_subhanallah
        Dhikr.ALHAMDULILLAH -> R.string.tasbih_dhikr_alhamdulillah
        Dhikr.ALLAHU_AKBAR -> R.string.tasbih_dhikr_allahuakbar
        Dhikr.LA_ILAHA_ILLALLAH -> R.string.tasbih_dhikr_lailahaillallah
    }

@get:StringRes
val CalculationMethod.labelRes: Int
    get() = when (this) {
        CalculationMethod.OMAN -> R.string.calc_oman
        CalculationMethod.MWL -> R.string.calc_mwl
        CalculationMethod.ISNA -> R.string.calc_isna
        CalculationMethod.EGYPT -> R.string.calc_egypt
        CalculationMethod.MAKKAH -> R.string.calc_makkah
        CalculationMethod.KARACHI -> R.string.calc_karachi
        CalculationMethod.JAFARI -> R.string.calc_jafari
        CalculationMethod.TEHRAN -> R.string.calc_tehran
    }

@get:StringRes
val AsrSchool.labelRes: Int
    get() = when (this) {
        AsrSchool.STANDARD -> R.string.asr_standard
        AsrSchool.HANAFI -> R.string.asr_hanafi
    }

/** Locale-aware formatting shared by screens, widgets and notifications. */
object Formatters {

    /** Clock time honoring the device's 12/24-hour preference and the locale's digits. */
    fun time(context: Context, time: LocalTime, locale: Locale): String {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        return DateTimeFormatter.ofPattern(pattern, locale).withDecimalStyle(DecimalStyle.of(locale)).format(time)
    }

    /** Countdown such as "2:05:09" or "5:09". */
    fun countdown(duration: Duration, locale: Locale): String {
        val total = duration.seconds.coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        val text = if (h > 0) "%d:%02d:%02d".format(Locale.ROOT, h, m, s) else "%d:%02d".format(Locale.ROOT, m, s)
        return Numerals.localize(text, locale)
    }

    fun number(value: Int, locale: Locale): String = Numerals.format(value, locale)
}
