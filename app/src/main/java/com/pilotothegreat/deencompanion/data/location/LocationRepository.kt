package com.pilotothegreat.deencompanion.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.pilotothegreat.deencompanion.data.net.Http
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import timber.log.Timber
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume

class LocationRepository(
    private val context: Context,
    private val settings: SettingsRepository,
) {
    enum class Result { UPDATED, PERMISSION_MISSING, UNAVAILABLE }

    private data class Resolved(val latitude: Double, val longitude: Double, val city: String?, val timezoneId: String)

    fun hasPermission(): Boolean = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    /**
     * Resolves the device location, falling back to IP geolocation only when the user allows it,
     * and saves it in one write.
     */
    suspend fun refresh(): Result {
        val allowIp = settings.current().useIpLocationFallback
        val fix = if (hasPermission()) deviceLocation() else null
        val resolved = when {
            fix != null -> Resolved(fix.latitude, fix.longitude, geocode(fix.latitude, fix.longitude), TimeZone.getDefault().id)
            allowIp -> ipLocation()
            else -> null
        } ?: return if (hasPermission()) Result.UNAVAILABLE else Result.PERMISSION_MISSING

        settings.setLocation(resolved.latitude, resolved.longitude, resolved.city, resolved.timezoneId)
        return Result.UPDATED
    }

    /** Refreshes when never set or older than [maxAgeMillis]; returns null if still fresh. */
    suspend fun refreshIfStale(maxAgeMillis: Long = 24 * 60 * 60 * 1000L): Result? {
        val location = settings.current().location
        val age = System.currentTimeMillis() - location.updatedAt
        if (!location.isDefault && location.updatedAt > 0 && age < maxAgeMillis) return null
        return refresh()
    }

    /** Re-resolves the saved city name in the current app language. */
    suspend fun relocalizeCity() {
        val location = settings.current().location
        if (location.isDefault) return
        geocode(location.latitude, location.longitude)?.let { settings.setCityName(it) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun deviceLocation(): Location? {
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = runCatching { manager.getProviders(true) }.getOrDefault(emptyList())
        val lastKnown = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
        lastKnown.filter { System.currentTimeMillis() - it.time < TWO_HOURS }.maxByOrNull { it.time }?.let { return it }

        val provider = listOfNotNull(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) LocationManager.FUSED_PROVIDER else null,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
        ).firstOrNull { it in providers }

        val fresh = provider?.let {
            withTimeoutOrNull(15_000) {
                suspendCancellableCoroutine { continuation ->
                    val signal = CancellationSignal()
                    continuation.invokeOnCancellation { signal.cancel() }
                    LocationManagerCompat.getCurrentLocation(
                        manager, it, signal, ContextCompat.getMainExecutor(context),
                    ) { location -> if (continuation.isActive) continuation.resume(location) }
                }
            }
        }
        return fresh ?: lastKnown.maxByOrNull { it.time }
    }

    private suspend fun geocode(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, appLocale())
        val address: Address? = withTimeoutOrNull(10_000) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    })
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    runCatching { geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull() }.getOrNull()
                }
            }
        }
        return address?.let {
            listOfNotNull(it.locality ?: it.subAdminArea ?: it.adminArea, it.countryName)
                .distinct()
                .joinToString(", ")
                .ifBlank { null }
        }
    }

    private suspend fun ipLocation(): Resolved? {
        for (provider in IP_PROVIDERS) {
            try {
                val json = JSONObject(Http.getText(provider.url, timeoutMs = 5_000))
                val latitude = json.getDouble(provider.latitude)
                val longitude = json.getDouble(provider.longitude)
                val city = geocode(latitude, longitude) ?: listOf(json.optString(provider.city), json.optString(provider.country))
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
                    .ifBlank { null }
                val timezone = json.optString(provider.timezone).ifBlank { TimeZone.getDefault().id }
                return Resolved(latitude, longitude, city, timezone)
            } catch (e: Exception) {
                Timber.w(e, "IP location lookup failed: %s", provider.url)
            }
        }
        return null
    }

    private fun appLocale(): Locale = AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()

    private class IpProvider(
        val url: String,
        val latitude: String,
        val longitude: String,
        val city: String,
        val country: String,
        val timezone: String,
    )

    private companion object {
        const val TWO_HOURS = 2 * 60 * 60 * 1000L
        val IP_PROVIDERS = listOf(
            IpProvider("https://ipapi.co/json/", "latitude", "longitude", "city", "country_name", "timezone"),
            IpProvider("https://freeipapi.com/api/json", "latitude", "longitude", "cityName", "countryName", "timeZone"),
        )
    }
}
