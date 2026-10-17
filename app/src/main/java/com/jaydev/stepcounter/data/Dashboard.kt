package com.jaydev.stepcounter.data

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

const val STEP_GOAL = 10_000L

data class DailyActivity(
    val date: LocalDate,
    val zone: String,
    val steps: Long?,
    val calories: Double?,
    val distanceMeters: Double?,
    val syncedAt: Long,
)

data class Dashboard(
    val activity: DailyActivity? = null,
    val syncStartedAt: Long = 0,
    val message: String? = null,
    val previewSteps: Long? = null,
) {
    fun presentation(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): Presentation {
        val today = now.atZone(zone).toLocalDate()
        val demo = previewSteps
        val current = activity?.takeIf { it.date == today && it.zone == zone.id }
        val steps = demo ?: current?.steps
        val working = syncStartedAt > 0 && now.toEpochMilli() - syncStartedAt in 0..120_000
        val stale = current != null && now.toEpochMilli() - current.syncedAt > 45 * 60_000L
        val time = activity?.let { Instant.ofEpochMilli(it.syncedAt).atZone(zone).format(DateTimeFormatter.ofPattern("MM/dd HH:mm")) }
        return Presentation(
            steps = steps,
            calories = if (demo != null) 246.0 else current?.calories,
            distanceMeters = if (demo != null) 5800.0 else current?.distanceMeters,
            progress = ((steps ?: 0L).coerceAtLeast(0).toDouble() / STEP_GOAL).coerceIn(0.0, 1.0).toFloat(),
            syncing = working,
            demo = demo != null,
            status = when {
                working -> "동기화 중…"
                demo != null -> "디자인 미리보기 · 예시 데이터"
                message != null -> message
                activity != null && current == null -> "오늘 데이터 갱신 필요"
                stale -> "오래된 데이터 · 새로고침 필요"
                time != null -> "$time 갱신"
                else -> "앱에서 삼성 헬스를 연결하세요"
            },
            lastSync = time,
        )
    }
}

data class Presentation(
    val steps: Long?,
    val calories: Double?,
    val distanceMeters: Double?,
    val progress: Float,
    val syncing: Boolean,
    val demo: Boolean,
    val status: String,
    val lastSync: String?,
) {
    val stepText: String get() = steps?.let { NumberFormat.getIntegerInstance(Locale.US).format(it) } ?: "—"
    val calorieText: String get() = calories?.let { String.format(Locale.US, "%,.0f", it) } ?: "—"
    val distanceText: String get() = distanceMeters?.let { String.format(Locale.US, "%,.1f", it / 1000) } ?: "—"
    val goalText: String get() = if ((steps ?: 0) >= STEP_GOAL) "10,000보 목표 달성" else "목표 10,000보"
}
