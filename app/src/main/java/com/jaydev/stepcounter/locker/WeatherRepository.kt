package com.jaydev.stepcounter.locker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.gson.JsonParser
import java.io.IOException
import java.net.URL
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

data class WeatherSnapshot(val temperature: Double, val code: Int, val observedAt: Long, val fetchedAt: Long, val isDay: Boolean? = null, val city: String? = null, val cityLookupAttempted: Boolean = false) {
    fun description(): String = "${temperature.roundToInt()}° · ${weatherDescription(code)}"
    fun stale(now: Long) = now - fetchedAt > 2 * 60 * 60 * 1000L || now < fetchedAt
}

fun weatherDescription(code: Int): String = when (code) {
    0 -> "맑음"
    1, 2 -> "구름 조금"
    3 -> "흐림"
    45, 48 -> "안개"
    51, 53, 55, 56, 57 -> "이슬비"
    61, 63, 65, 66, 67, 80, 81, 82 -> "비"
    71, 73, 75, 77, 85, 86 -> "눈"
    95, 96, 99 -> "뇌우"
    else -> "날씨 확인 중"
}

internal fun parseWeather(json: String, fetchedAt: Long): WeatherSnapshot {
    val current = JsonParser.parseString(json).asJsonObject.getAsJsonObject("current")
    val temperature = current.get("temperature_2m").asDouble
    require(temperature.isFinite())
    return WeatherSnapshot(temperature, current.get("weather_code").asInt, current.get("time").asLong * 1000, fetchedAt,
        current.get("is_day")?.takeUnless { it.isJsonNull }?.asInt?.let { it == 1 })
}

class WeatherRepository(private val context: Context) {
    // Location is requested only by a visible activity, never by the foreground service.
    suspend fun loadCurrentLocation(): WeatherSnapshot {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            throw SecurityException("위치 권한이 필요합니다")
        val location = withTimeout(25_000) { currentLocation() }
        // Coarse, rounded coordinates only; no health payload or identifiers are sent.
        val lat = String.format(Locale.US, "%.2f", location.latitude)
        val lon = String.format(Locale.US, "%.2f", location.longitude)
        val weather = withContext(Dispatchers.IO) {
            val connection = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code,is_day&timeformat=unixtime&forecast_days=1").openConnection() as HttpsURLConnection
            try {
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                if (connection.responseCode != 200) throw IOException("날씨 조회 실패")
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                parseWeather(json, System.currentTimeMillis())
            } finally { connection.disconnect() }
        }
        // City boundaries need the original OS location. Rounding is only for the weather API.
        return weather.copy(city = CityResolver(context).resolve(location.latitude, location.longitude), cityLookupAttempted = true)
    }

    @Suppress("MissingPermission", "DEPRECATION", "OVERRIDE_DEPRECATION")
    private suspend fun currentLocation(): Location = suspendCancellableCoroutine { continuation ->
        val manager = context.getSystemService(LocationManager::class.java)
        val provider = if (Build.VERSION.SDK_INT >= 31 && manager.getProviders(true).contains(LocationManager.FUSED_PROVIDER))
            LocationManager.FUSED_PROVIDER else LocationManager.NETWORK_PROVIDER
        if (!manager.isProviderEnabled(provider)) {
            continuation.resumeWithException(IOException("위치 서비스가 꺼져 있습니다"))
            return@suspendCancellableCoroutine
        }
        if (Build.VERSION.SDK_INT >= 30) {
            val cancellation = CancellationSignal()
            continuation.invokeOnCancellation { cancellation.cancel() }
            manager.getCurrentLocation(provider, cancellation, context.mainExecutor) { location ->
                if (continuation.isActive) {
                    if (location != null) continuation.resume(location)
                    else continuation.resumeWithException(IOException("현재 위치를 확인하지 못했습니다"))
                }
            }
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(location)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            continuation.invokeOnCancellation { manager.removeUpdates(listener) }
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        }
    }
}
