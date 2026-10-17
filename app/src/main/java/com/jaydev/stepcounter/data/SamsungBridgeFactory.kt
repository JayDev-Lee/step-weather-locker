package com.jaydev.stepcounter.data

import android.app.Activity
import android.content.Context
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.error.ResolvablePlatformException
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

fun createSamsungBridge(context: Context): HealthBridge = SamsungHealthBridge(context)

private class SamsungHealthBridge(context: Context) : HealthBridge {
    // Keep service creation inside the guarded request so SDK setup errors become useful UI state.
    private val store by lazy { HealthDataService.getStore(context) }
    private val stepsPermission = Permission.of(DataTypes.STEPS, AccessType.READ)
    private val summaryPermission = Permission.of(DataTypes.ACTIVITY_SUMMARY, AccessType.READ)
    private val permissions = setOf(stepsPermission, summaryPermission)

    override suspend fun requestPermissions(activity: Activity) = sdkRequest(activity) {
        val current = store.getGrantedPermissions(permissions)
        val granted = if (current.containsAll(permissions)) current else store.requestPermissions(permissions, activity)
        if (stepsPermission !in granted) throw HealthAccessException("걸음 수 읽기 권한을 허용해주세요")
    }

    override suspend fun readToday(date: LocalDate, zone: ZoneId): DailyActivity = sdkRequest {
        val granted = store.getGrantedPermissions(permissions)
        if (stepsPermission !in granted) throw HealthAccessException("앱에서 걸음 수 읽기 권한을 허용해주세요")
        val end = LocalDateTime.now(zone)
        if (end.toLocalDate() != date) throw HealthAccessException("날짜가 변경되었습니다. 다시 새로고침해주세요", false)
        val filter = LocalTimeFilter.of(date.atStartOfDay(), end)
        val stepsRequest = DataType.StepsType.TOTAL.requestBuilder.setLocalTimeFilter(filter).build()
        val steps = store.aggregateData(stepsRequest).dataList.mapNotNull { it.value }.sum()
        var calories: Double? = null
        var distance: Double? = null
        if (summaryPermission in granted) {
            val caloriesRequest = DataType.ActivitySummaryType.TOTAL_ACTIVE_CALORIES_BURNED.requestBuilder
                .setLocalTimeFilter(filter).build()
            val distanceRequest = DataType.ActivitySummaryType.TOTAL_DISTANCE.requestBuilder
                .setLocalTimeFilter(filter).build()
            calories = store.aggregateData(caloriesRequest).dataList.mapNotNull { it.value?.toDouble() }
                .takeIf { it.isNotEmpty() }?.sum()
            distance = store.aggregateData(distanceRequest).dataList.mapNotNull { it.value?.toDouble() }
                .takeIf { it.isNotEmpty() }?.sum()
        }
        DailyActivity(date, zone.id, steps, calories, distance, System.currentTimeMillis())
    }

    private suspend fun <T> sdkRequest(activity: Activity? = null, block: suspend () -> T): T = try {
        block()
    } catch (error: HealthDataException) {
        currentCoroutineContext().ensureActive()
        // Resolution UI (install/update/initialize) is offered only after an explicit Connect tap.
        if (activity != null && !activity.isFinishing && !activity.isDestroyed &&
            error is ResolvablePlatformException && error.hasResolution) {
            error.resolve(activity)
        }
        throw samsungHealthFailure(error.errorCode)
    }
}
