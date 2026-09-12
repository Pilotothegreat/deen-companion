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
import com.pilotothegreat.deencompanion.data.settings.LocationSource
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
    private val cities: CityIndex,
) {
    enum class Result { UPDATED, PERMISSION_MISSING, UNAVAILABLE }

    private data class Resolved(
        val latitude: Double,
        val longitude: Double,
        val city: String?,
        val countryCode: String?,
        val timezoneId: String,
        val source: LocationSource,
    )

    private data class Place(val city: String?, val countryCode: String?)

    @Volatile
    private var lastOpenCheck = 0L

    fun hasPermission(): Boolean = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    /**
     * Resolves the device location, falling back to IP geolocation only when the user allows it,
     * and saves it in one write. This also switches a manually chosen city back to automatic.
     */
    /**
     * Speed reported by the last fix, when the provider gave one. The app never asks for location in
     * order to learn this; it only reads what a fix it took anyway happens to carry.
     */
    @Volatile var lastFixSpeed: Float? = null
        private set

    suspend fun refresh(): Result {
        val allowIp = settings.current().useIpLocationFallback
        val fix = if (hasPermission()) deviceLocation() else null
        lastFixSpeed = fix?.takeIf { it.hasSpeed() }?.speed
        val resolved = when {
            fix != null -> {
                val place = describe(fix.latitude, fix.longitude)
                Resolved(fix.latitude, fix.longitude, place.city, place.countryCode, TimeZone.getDefault().id, LocationSource.DEVICE)
            }
            allowIp -> ipLocation()
            else -> null
        } ?: return if (hasPermission()) Result.UNAVAILABLE else Result.PERMISSION_MISSING

        settings.setLocation(
            resolved.latitude, resolved.longitude, resolved.city, resolved.timezoneId, resolved.countryCode, resolved.source,
        )
        return Result.UPDATED
    }

    /** Saves a city picked from the list; it stays until the device location is used again. */
    suspend fun chooseCity(city: City) {
        settings.setLocation(
            city.latitude, city.longitude, city.label(appLocale()), city.timezoneId, city.countryCode, LocationSource.MANUAL,
        )
    }

    /**
     * On app start and resume: refreshes a missing or day-old automatic location, or one the
     * device has since moved away from (a cheap last-known check, no GPS fix). Throttled.
     */
    suspend fun onAppOpened() {
        val now = System.currentTimeMillis()
        if (now - lastOpenCheck < THIRTY_MINUTES) return
        lastOpenCheck = now

        val current = settings.current()
        val location = current.location
        if (location.source == LocationSource.MANUAL) return
        if (location.isDefault || now - location.updatedAt > ONE_DAY) {
            if (hasPermission() || current.useIpLocationFallback) refresh()
            return
        }
        if (!hasPermission()) return
        val last = lastKnownLocation() ?: return
        if (CityIndex.distanceKm(last.latitude, last.longitude, location.latitude, location.longitude) > TRAVEL_KM) refresh()
    }

    /** Keeps an automatic location on the device's time zone after it changes, e.g. after landing abroad. */
    suspend fun onTimezoneChanged() {
        val location = settings.current().location
        if (location.source != LocationSource.MANUAL && !location.isDefault) settings.setTimezone(TimeZone.getDefault().id)
    }

    /** Re-resolves the saved city name in the current app language. */
    suspend fun relocalizeCity() {
        val location = settings.current().location
        if (location.isDefault) return
        val name = if (location.source == LocationSource.MANUAL) {
            cities.nearest(location.latitude, location.longitude, withinKm = 5.0)?.label(appLocale())
        } else {
            describe(location.latitude, location.longitude).city
        }
        name?.let { settings.setCityName(it) }
    }

    /** City name and country from the geocoder, or from the nearest bundled city when it fails or is missing. */
    private suspend fun describe(latitude: Double, longitude: Double): Place {
        val geocoded = geocode(latitude, longitude)
        if (geocoded?.city != null && geocoded.countryCode != null) return geocoded
        val nearest = cities.nearest(latitude, longitude)
        return Place(
            city = geocoded?.city ?: nearest?.label(appLocale()),
            countryCode = geocoded?.countryCode ?: nearest?.countryCode,
        )
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(): Location? {
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = runCatching { manager.getProviders(true) }.getOrDefault(emptyList())
        return providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    private suspend fun deviceLocation(): Location? {
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = runCatching { manager.getProviders(true) }.getOrDefault(emptyList())
        val lastKnown = lastKnownLocation()
        lastKnown?.takeIf { System.currentTimeMillis() - it.time < TWO_HOURS }?.let { return it }

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
        return fresh ?: lastKnown
    }

    private suspend fun geocode(latitude: Double, longitude: Double): Place? {
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
            val separator = if (appLocale().language == "ar") "، " else ", "
            val city = listOfNotNull(it.locality ?: it.subAdminArea ?: it.adminArea, it.countryName)
                .distinct()
                .joinToString(separator)
                .ifBlank { null }
            Place(city, it.countryCode)
        }
    }

    private suspend fun ipLocation(): Resolved? {
        for (provider in IP_PROVIDERS) {
            try {
                val json = JSONObject(Http.getText(provider.url, timeoutMs = 5_000))
                val latitude = json.getDouble(provider.latitude)
                val longitude = json.getDouble(provider.longitude)
                val place = describe(latitude, longitude)
                val city = place.city ?: listOf(json.optString(provider.city), json.optString(provider.country))
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
                    .ifBlank { null }
                val country = place.countryCode ?: json.optString(provider.countryCode).ifBlank { null }
                val timezone = json.optString(provider.timezone).ifBlank { TimeZone.getDefault().id }
                return Resolved(latitude, longitude, city, country, timezone, LocationSource.IP)
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
        val countryCode: String,
        val timezone: String,
    )

    private companion object {
        const val TWO_HOURS = 2 * 60 * 60 * 1000L
        const val THIRTY_MINUTES = 30 * 60 * 1000L
        const val ONE_DAY = 24 * 60 * 60 * 1000L
        /** Moving further than this from the saved location counts as travel. */
        const val TRAVEL_KM = 25.0
        val IP_PROVIDERS = listOf(
            IpProvider("https://ipapi.co/json/", "latitude", "longitude", "city", "country_name", "country_code", "timezone"),
            IpProvider("https://freeipapi.com/api/json", "latitude", "longitude", "cityName", "countryName", "countryCode", "timeZone"),
        )
    }
}
