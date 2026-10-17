package com.jaydev.stepcounter.sync

import android.content.Context
import androidx.work.*
import com.jaydev.stepcounter.data.*
import com.jaydev.stepcounter.widget.refreshWidgets
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

object SyncScheduler {
    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "samsung-health-periodic", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).build(),
        )
    }
    fun refresh(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "samsung-health-refresh", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<SyncWorker>().build(),
        )
    }
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("samsung-health-periodic")
        WorkManager.getInstance(context).cancelUniqueWork("samsung-health-refresh")
    }
}

class SyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        if (!syncMutex.tryLock()) return Result.success()
        val store = DashboardStore(applicationContext)
        try {
            if (store.state.first().previewSteps != null) {
                refreshWidgets(applicationContext)
                return Result.success()
            }
            store.started()
            refreshWidgets(applicationContext)
            val zone = ZoneId.systemDefault()
            val date = LocalDate.now(zone)
            val result = withTimeout(45_000) { healthBridge(applicationContext).readToday(date, zone) }
            if (LocalDate.now(zone) != date || ZoneId.systemDefault() != zone) {
                store.failed("날짜가 변경되었습니다. 다시 새로고침해주세요")
                return Result.retry()
            }
            store.succeeded(result)
            return Result.success()
        } catch (error: TimeoutCancellationException) {
            store.failed("응답이 늦습니다. 다시 새로고침해주세요")
            return retryOrFinish()
        } catch (error: CancellationException) {
            throw error
        } catch (error: HealthAccessException) {
            store.failed(error.userMessage)
            return if (error.permanent) Result.failure() else retryOrFinish()
        } catch (error: Exception) {
            // Never persist health payloads, exception text or credentials in UI/logs.
            store.failed("연동 실패 · 앱에서 삼성 헬스와 읽기 권한을 확인해주세요")
            return retryOrFinish()
        } finally {
            try {
                withContext(NonCancellable) {
                    store.finished()
                    refreshWidgets(applicationContext)
                }
            } finally { syncMutex.unlock() }
        }
    }
    private fun retryOrFinish() = if (runAttemptCount < 2) Result.retry() else Result.failure()
    companion object { private val syncMutex = Mutex() }
}
