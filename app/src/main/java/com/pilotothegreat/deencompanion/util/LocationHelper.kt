package com.pilotothegreat.deencompanion.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.TimeZone

object LocationHelper {

    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val cityName: String,
        val timezoneId: String
    )

    @SuppressLint("MissingPermission")
    suspend fun getDeviceLocation(context: Context): LocationData? = withContext(Dispatchers.IO) {
        val hasCoarsePerm = context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasCoarsePerm) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@withContext null
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null

            for (provider in providers) {
                val l = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            }

            if (bestLocation != null) {
                var city = "GPS: ${"%.2f".format(bestLocation.latitude)}, ${"%.2f".format(bestLocation.longitude)}"
                try {
                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                    val addresses = geocoder.getFromLocation(bestLocation.latitude, bestLocation.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val locality = address.locality ?: address.subAdminArea
                        val adminArea = address.adminArea
                        val country = address.countryName
                        city = when {
                            locality != null && country != null -> "$locality, $country"
                            locality != null -> locality
                            adminArea != null && country != null -> "$adminArea, $country"
                            country != null -> country
                            else -> city
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return@withContext LocationData(
                    latitude = bestLocation.latitude,
                    longitude = bestLocation.longitude,
                    cityName = city,
                    timezoneId = TimeZone.getDefault().id
                )
            }
        }
        return@withContext null
    }

    suspend fun fetchIpLocation(): LocationData? = withContext(Dispatchers.IO) {
        // Try Provider 1: ipapi.co
        try {
            val url = URL("https://ipapi.co/json/")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val lat = json.getDouble("latitude")
                val lon = json.getDouble("longitude")
                val city = json.optString("city", "Unknown City")
                val country = json.optString("country_name", "")
                val dispName = if (country.isNotEmpty()) "$city, $country" else city
                val tz = json.optString("timezone", TimeZone.getDefault().id)
                return@withContext LocationData(lat, lon, dispName, tz)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Try Provider 2: ip-api.com
        try {
            val url = URL("http://ip-api.com/json/")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                if (json.optString("status") == "success") {
                    val lat = json.getDouble("lat")
                    val lon = json.getDouble("lon")
                    val city = json.optString("city", "Unknown City")
                    val country = json.optString("country", "")
                    val dispName = if (country.isNotEmpty()) "$city, $country" else city
                    val tz = json.optString("timezone", TimeZone.getDefault().id)
                    return@withContext LocationData(lat, lon, dispName, tz)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Try Provider 3: freeipapi.com
        try {
            val url = URL("https://freeipapi.com/api/json")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val lat = json.getDouble("latitude")
                val lon = json.getDouble("longitude")
                val city = json.optString("cityName", "Unknown City")
                val country = json.optString("countryName", "")
                val dispName = if (country.isNotEmpty()) "$city, $country" else city
                val tz = json.optString("timeZone", TimeZone.getDefault().id)
                return@withContext LocationData(lat, lon, dispName, tz)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext null
    }
}

