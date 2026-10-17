package com.jaydev.stepcounter.locker

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import com.google.gson.Gson

data class LockerSettings(
    val enabled: Boolean = false,
    val backgroundPath: String? = null,
    val weatherEnabled: Boolean = false,
    val weather: WeatherSnapshot? = null,
    val autoStatus: String = "자동 표시 기록이 없습니다",
)

class LockerStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("locker", Context.MODE_PRIVATE)
    fun snapshot(): LockerSettings = LockerSettings(
        preferences.getBoolean("enabled", false), preferences.getString("background", null),
        preferences.getBoolean("weather_enabled", false),
        preferences.getString("weather", null)?.let { json ->
            try { Gson().fromJson(json, WeatherSnapshot::class.java) } catch (_: Exception) { null }
        },
        preferences.getString("auto_status", null) ?: "자동 표시 기록이 없습니다",
    )
    val state = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(snapshot()) }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(snapshot())
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    fun enabled(value: Boolean) { preferences.edit().putBoolean("enabled", value).apply() }
    fun background(path: String?) { preferences.edit().putString("background", path).apply() }
    fun weatherEnabled(value: Boolean) {
        preferences.edit().putBoolean("weather_enabled", value).apply()
        if (!value) preferences.edit().remove("weather").apply()
    }
    fun weather(value: WeatherSnapshot) { preferences.edit().putString("weather", Gson().toJson(value)).apply() }
    fun autoStatus(message: String) {
        val time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
        preferences.edit().putString("auto_status", "$time · $message").apply()
    }
    fun autoShown(token: Long) {
        preferences.edit().putLong("auto_shown", token).apply()
        autoStatus("잠금화면 표시 확인됨")
    }
    fun wasAutoShown(token: Long) = preferences.getLong("auto_shown", 0) == token
}
