package com.pilotothegreat.deencompanion.util

import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.*

class PrayerTimeCalculator {

    enum class CalculationMethod(
        val fajrAngle: Double,
        val ishaAngle: Double,
        val isIshaInterval: Boolean = false,
        val ishaIntervalMins: Long = 0,
        val dhuhrBufferMins: Int = 0
    ) {
        MWL(18.0, 17.0, false, 0, 1),
        ISNA(15.0, 15.0, false, 0, 1),
        EGYPT(19.5, 17.5, false, 0, 0),
        MAKKAH(18.5, 90.0, true, 90, 1), // Umm Al-Qura (90 mins after Maghrib)
        KARACHI(18.0, 18.0, false, 0, 0),
        JAFARI(16.0, 14.0, false, 0, 0),
        TEHRAN(17.7, 14.0, false, 0, 0)
    }

    enum class AsrSchool(val shadowFactor: Double) {
        STANDARD(1.0), // Shafi, Maliki, Hanbali
        HANAFI(2.0)
    }

    data class PrayerTimes(
        val fajr: LocalTime,
        val sunrise: LocalTime,
        val dhuhr: LocalTime,
        val asr: LocalTime,
        val maghrib: LocalTime,
        val isha: LocalTime
    )

    companion object {
        fun calculate(
            date: LocalDate,
            latitude: Double,
            longitude: Double,
            timezoneOffsetHours: Double,
            method: CalculationMethod = CalculationMethod.MWL,
            asrSchool: AsrSchool = AsrSchool.STANDARD,
            hijriMonth: Int? = null
        ): PrayerTimes {
            val year = date.year
            val month = date.monthValue
            val day = date.dayOfMonth

            // Julian Date
            var y = year
            var m = month
            if (m <= 2) {
                y -= 1
                m += 12
            }

            val a = y / 100
            val b = 2 - a + (a / 4)
            val jd = (365.25 * (y + 4716)).toInt() + (30.6001 * (m + 1)).toInt() + day + b - 1524.5

            val d = jd - 2451545.0

            // Solar Coordinates
            var g = 357.529 + 0.98560028 * d
            var q = 280.459 + 0.98564736 * d
            g = fixAngle(g)
            q = fixAngle(q)

            val L = fixAngle(q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2 * g)))
            val e = 23.439 - 0.00000036 * d

            val dd = Math.toDegrees(asin(sin(Math.toRadians(L)) * sin(Math.toRadians(e))))
            val ra = Math.toDegrees(atan2(cos(Math.toRadians(e)) * sin(Math.toRadians(L)), cos(Math.toRadians(L)))) / 15.0
            val raFixed = fixHour(ra)

            val eqt = q / 15.0 - raFixed

            // Mid Day (Dhuhr)
            val dhuhrLocal = 12.0 + timezoneOffsetHours - (longitude / 15.0) - eqt
            val dhuhrBuffer = method.dhuhrBufferMins / 60.0
            val dhuhrTime = doubleToTime(dhuhrLocal + dhuhrBuffer) // Precautionary buffer for Zawal if applicable

            // Sunrise and Sunset
            val sunriseHA = hourAngle(-0.833, latitude, dd)
            val sunriseLocal = dhuhrLocal - (sunriseHA / 15.0)
            val sunsetLocal = dhuhrLocal + (sunriseHA / 15.0)

            val sunriseTime = doubleToTime(sunriseLocal)
            val maghribTime = doubleToTime(sunsetLocal + (2.0 / 60.0)) // +2 minutes precautionary buffer to ensure sun disk has completely set

            val nightLength = (24.0 - 2.0 * (sunriseHA / 15.0)).coerceIn(4.0, 20.0)

            // Fajr
            val fajrAngle = method.fajrAngle
            val isFajrImpossible = isAngleImpossible(-fajrAngle, latitude, dd)
            val useFajrAdjustment = abs(latitude) > 48.0 || isFajrImpossible
            val fajrTime = if (useFajrAdjustment) {
                val fajrPortion = fajrAngle / 60.0
                val fajrOffset = fajrPortion * nightLength
                doubleToTime(sunriseLocal - fajrOffset)
            } else {
                val fajrHA = hourAngle(-fajrAngle, latitude, dd)
                doubleToTime(dhuhrLocal - (fajrHA / 15.0))
            }

            // Isha
            val ishaTime = if (method.isIshaInterval) {
                val actualHijriMonth = hijriMonth ?: try {
                    java.time.chrono.HijrahDate.from(date).get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
                } catch (e: Exception) {
                    1
                }
                val intervalMins = if (method == CalculationMethod.MAKKAH && actualHijriMonth == 9) 120L else method.ishaIntervalMins
                doubleToTime(sunsetLocal + (2.0 / 60.0) + (intervalMins / 60.0)) // Offset Isha according to the buffered Maghrib time
            } else {
                val ishaAngle = method.ishaAngle
                val isIshaImpossible = isAngleImpossible(-ishaAngle, latitude, dd)
                val useIshaAdjustment = abs(latitude) > 48.0 || isIshaImpossible
                if (useIshaAdjustment) {
                    val ishaPortion = ishaAngle / 60.0
                    val ishaOffset = ishaPortion * nightLength
                    doubleToTime(sunsetLocal + ishaOffset)
                } else {
                    val ishaHA = hourAngle(-ishaAngle, latitude, dd)
                    doubleToTime(dhuhrLocal + (ishaHA / 15.0))
                }
            }

            // Asr
            val absDiff = abs(latitude - dd)
            val altitudeAsr = Math.toDegrees(atan(1.0 / (tan(Math.toRadians(absDiff)) + asrSchool.shadowFactor)))
            val asrHA = hourAngle(altitudeAsr, latitude, dd)
            val asrTime = doubleToTime(dhuhrLocal + (asrHA / 15.0))

            return PrayerTimes(
                fajr = fajrTime,
                sunrise = sunriseTime,
                dhuhr = dhuhrTime,
                asr = asrTime,
                maghrib = maghribTime,
                isha = ishaTime
            )
        }

        private fun hourAngle(angleOrAltitude: Double, lat: Double, decl: Double): Double {
            val altitudeRad = Math.toRadians(angleOrAltitude)
            val latRad = Math.toRadians(lat)
            val declRad = Math.toRadians(decl)

            val cosHA = (sin(altitudeRad) - sin(latRad) * sin(declRad)) / (cos(latRad) * cos(declRad))
            if (cosHA < -1.0) return 180.0
            if (cosHA > 1.0) return 0.0
            return Math.toDegrees(acos(cosHA))
        }

        private fun isAngleImpossible(angleOrAltitude: Double, lat: Double, decl: Double): Boolean {
            val altitudeRad = Math.toRadians(angleOrAltitude)
            val latRad = Math.toRadians(lat)
            val declRad = Math.toRadians(decl)
            val cosHA = (sin(altitudeRad) - sin(latRad) * sin(declRad)) / (cos(latRad) * cos(declRad))
            return cosHA < -1.0 || cosHA > 1.0
        }

        private fun doubleToTime(value: Double): LocalTime {
            var valFixed = fixHour(value)
            val hours = valFixed.toInt()
            valFixed = (valFixed - hours) * 60.0
            var minutes = Math.round(valFixed).toInt()

            var finalHours = hours
            var finalMinutes = minutes
            if (finalMinutes >= 60) {
                finalHours = (finalHours + 1) % 24
                finalMinutes -= 60
            }
            if (finalHours < 0) {
                finalHours += 24
            }
            return LocalTime.of(finalHours % 24, finalMinutes)
        }

        private fun fixAngle(angle: Double): Double {
            var a = angle % 360.0
            if (a < 0) a += 360.0
            return a
        }

        private fun fixHour(hour: Double): Double {
            var h = hour % 24.0
            if (h < 0) h += 24.0
            return h
        }
    }
}

