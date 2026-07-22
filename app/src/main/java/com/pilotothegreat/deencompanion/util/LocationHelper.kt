package com.pilotothegreat.deencompanion.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
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
    suspend fun getDeviceLocation(context: Context, useIpFallback: Boolean = true): LocationData? = withContext(Dispatchers.IO) {
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

            if (bestLocation == null) {
                bestLocation = withTimeoutOrNull(10000L) {
                    requestFreshLocation(locationManager)
                }
            }

            if (bestLocation != null) {
                var city = "GPS: ${"%.2f".format(bestLocation.latitude)}, ${"%.2f".format(bestLocation.longitude)}"
                try {
                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                    @Suppress("DEPRECATION")
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

        // Only attempt IP-based fallback if the user has allowed it
        if (useIpFallback) {
            return@withContext fetchIpLocation()
        }

        return@withContext null
    }


    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(locationManager: LocationManager): Location? = suspendCancellableCoroutine { continuation ->
        val providers = locationManager.getProviders(true)
        val provider = when {
            providers.contains(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            providers.contains(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            providers.isNotEmpty() -> providers[0]
            else -> null
        }

        if (provider == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            locationManager.requestLocationUpdates(
                provider,
                0L,
                0f,
                listener,
                android.os.Looper.getMainLooper()
            )
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }

        continuation.invokeOnCancellation {
            try {
                locationManager.removeUpdates(listener)
            } catch (e: Exception) {}
        }
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
            val url = URL("https://ip-api.com/json/")
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

