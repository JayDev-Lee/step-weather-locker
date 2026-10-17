package com.jaydev.stepcounter.data

import android.app.Activity
import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

interface HealthBridge {
    suspend fun requestPermissions(activity: Activity)
    suspend fun readToday(date: LocalDate, zone: ZoneId): DailyActivity
}

class HealthAccessException(val userMessage: String, val permanent: Boolean = true) : Exception(userMessage)

fun healthBridge(context: Context): HealthBridge {
    return createSamsungBridge(context.applicationContext)
}
