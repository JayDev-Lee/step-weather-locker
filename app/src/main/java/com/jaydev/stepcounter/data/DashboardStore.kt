package com.jaydev.stepcounter.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dashboardDataStore by preferencesDataStore("health_dashboard")

class DashboardStore(context: Context) {
    private val dataStore = context.applicationContext.dashboardDataStore
    private object Keys {
        val date = stringPreferencesKey("date")
        val zone = stringPreferencesKey("zone")
        val steps = longPreferencesKey("steps")
        val calories = doublePreferencesKey("calories")
        val distance = doublePreferencesKey("distance")
        val success = longPreferencesKey("success")
        val started = longPreferencesKey("started")
        val message = stringPreferencesKey("message")
        val preview = longPreferencesKey("preview_steps")
    }
    val state = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { p ->
        val date = p[Keys.date]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        Dashboard(
            activity = date?.let { DailyActivity(it, p[Keys.zone].orEmpty(), p[Keys.steps], p[Keys.calories], p[Keys.distance], p[Keys.success] ?: 0) },
            syncStartedAt = p[Keys.started] ?: 0,
            message = p[Keys.message],
            // Old developer-preview preferences must never replace real data after upgrade.
        )
    }
    suspend fun started() { dataStore.edit { it[Keys.started] = System.currentTimeMillis(); it.remove(Keys.message) } }
    suspend fun succeeded(activity: DailyActivity) {
        dataStore.edit { p ->
            p[Keys.date] = activity.date.toString(); p[Keys.zone] = activity.zone
            activity.steps?.let { p[Keys.steps] = it } ?: p.remove(Keys.steps)
            activity.calories?.let { p[Keys.calories] = it } ?: p.remove(Keys.calories)
            activity.distanceMeters?.let { p[Keys.distance] = it } ?: p.remove(Keys.distance)
            p[Keys.success] = activity.syncedAt
            p.remove(Keys.message)
        }
    }
    suspend fun failed(message: String) { dataStore.edit { it[Keys.message] = message } }
    suspend fun finished() { dataStore.edit { it.remove(Keys.started) } }
    suspend fun preview(steps: Long?) { dataStore.edit { p -> if (steps == null) p.remove(Keys.preview) else p[Keys.preview] = steps } }
    suspend fun disconnect() { dataStore.edit { it.clear() } }
}
