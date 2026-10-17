package com.jaydev.stepcounter.locker

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

internal fun cityLabel(locality: String?, district: String?, region: String?): String? {
    val city = locality?.trim()?.takeIf { it.isNotEmpty() }
        ?: district?.trim()?.takeIf { it.isNotEmpty() }
    return listOfNotNull(region?.trim()?.takeIf { it.isNotEmpty() }, city)
        .distinct().joinToString(" ").takeIf { it.isNotEmpty() }?.take(80)
}

internal class CityResolver(private val context: Context) {
    // A failed reverse lookup must not discard a successful weather response or reuse a city
    // from an older, potentially different location. Never save coordinates or street addresses.
    @Suppress("DEPRECATION")
    suspend fun resolve(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        return try {
            val geocoder = Geocoder(context, Locale.KOREAN)
            val addresses = if (Build.VERSION.SDK_INT >= 33) {
                withTimeoutOrNull(6_000) {
                    suspendCancellableCoroutine<List<Address>?> { continuation ->
                        geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (continuation.isActive) continuation.resume(addresses)
                            }
                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) continuation.resume(null)
                            }
                        })
                    }
                }
            } else {
                // Legacy platform geocoding is blocking; keep it off the UI thread.
                withContext(Dispatchers.IO) { geocoder.getFromLocation(latitude, longitude, 1) }
            }
            addresses?.firstOrNull()?.let { cityLabel(it.locality, it.subAdminArea, it.adminArea) }
        } catch (_: IOException) { null }
        catch (_: IllegalArgumentException) { null }
    }
}
