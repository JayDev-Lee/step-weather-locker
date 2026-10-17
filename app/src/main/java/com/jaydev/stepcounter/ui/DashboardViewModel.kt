package com.jaydev.stepcounter.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaydev.stepcounter.BuildConfig
import com.jaydev.stepcounter.data.*
import com.jaydev.stepcounter.sync.SyncScheduler
import com.jaydev.stepcounter.widget.refreshWidgets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val store = DashboardStore(application)
    private val tick = flow { while (true) { emit(System.currentTimeMillis()); delay(30_000) } }
    val state = combine(store.state, tick) { snapshot, _ -> snapshot.presentation() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Dashboard().presentation())

    fun resume() {
        if (BuildConfig.SAMSUNG_SDK_INCLUDED) SyncScheduler.schedule(getApplication())
        SyncScheduler.refresh(getApplication())
    }
    fun refresh() { SyncScheduler.refresh(getApplication()) }
    fun connect(activity: Activity) {
        viewModelScope.launch {
            try {
                healthBridge(getApplication()).requestPermissions(activity)
                store.preview(null)
                SyncScheduler.schedule(getApplication())
                SyncScheduler.refresh(getApplication())
            } catch (error: CancellationException) { throw error }
            catch (error: HealthAccessException) { store.failed(error.userMessage) }
            catch (error: Exception) { store.failed("삼성 헬스 설치·초기 설정 및 앱의 읽기 권한을 확인해주세요") }
        }
    }
    fun preview(steps: Long?) {
        if (!BuildConfig.DEBUG) return
        viewModelScope.launch {
            store.preview(steps)
            refreshWidgets(getApplication())
            if (steps == null) refresh()
        }
    }
}
