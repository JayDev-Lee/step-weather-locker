package com.jaydev.stepcounter.locker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class LockerSettingsActivity : ComponentActivity() {
    private val model: LockerViewModel by viewModels()
    private val photo = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) model.background(uri)
    }
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startLocker() else model.message("알림을 허용하면 잠금화면 모드 상태와 끄기 버튼을 사용할 수 있어요")
    }
    private val location = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true || granted[Manifest.permission.ACCESS_FINE_LOCATION] == true)
            model.enableWeather(true)
        else model.message("날씨를 보려면 위치 권한을 허용해주세요")
    }
    private val locationSettings = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        model.refreshWeather(force = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            val background by model.background.collectAsStateWithLifecycle()
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.refreshWeather() }
            LockerSettingsScreen(state, background,
                onEnabled = ::toggleLocker,
                onPhoto = { photo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onReset = { model.background(null) },
                onWeather = { enabled ->
                    if (!enabled) model.enableWeather(false)
                    else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) model.enableWeather(true)
                    else location.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                },
                onRefreshWeather = { model.refreshWeather(force = true) },
                onLocationSettings = {
                    locationSettings.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                },
                onPreview = { startActivity(Intent(this, LockerActivity::class.java)) },
                onBack = ::finish,
            )
        }
    }

    private fun toggleLocker(enabled: Boolean) {
        if (!enabled) {
            model.store.enabled(false)
            stopService(Intent(this, LockerService::class.java))
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            model.message("‘다른 앱 위에 표시’를 허용한 뒤 돌아와 모드를 켜주세요")
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else startLocker()
    }

    private fun startLocker() {
        try {
            model.store.enabled(true)
            ContextCompat.startForegroundService(this, Intent(this, LockerService::class.java))
            model.message("잠금화면 모드를 켰습니다. 화면을 껐다 켜서 확인해주세요")
        } catch (_: RuntimeException) {
            model.store.enabled(false)
            model.message("모드를 시작하지 못했습니다. 표시 권한을 확인해주세요")
        }
    }
}
