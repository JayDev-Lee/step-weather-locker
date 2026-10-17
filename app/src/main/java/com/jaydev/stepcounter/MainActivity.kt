package com.jaydev.stepcounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.jaydev.stepcounter.ui.DashboardScreen
import com.jaydev.stepcounter.ui.DashboardViewModel
import com.jaydev.stepcounter.widget.StepsWidgetReceiver
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val model: DashboardViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.resume() }
            DashboardScreen(
                state = state,
                sdkIncluded = BuildConfig.SAMSUNG_SDK_INCLUDED,
                onConnect = { model.connect(this) },
                onRefresh = model::refresh,
                onLocker = { startActivity(android.content.Intent(this, com.jaydev.stepcounter.locker.LockerSettingsActivity::class.java)) },
                onPin = {
                    lifecycleScope.launch {
                        val requested = GlanceAppWidgetManager(this@MainActivity)
                            .requestPinGlanceAppWidget(StepsWidgetReceiver::class.java)
                        if (!requested) android.widget.Toast.makeText(this@MainActivity,
                            "홈 화면을 길게 눌러 위젯 목록에서 하루링을 추가해주세요", android.widget.Toast.LENGTH_LONG).show()
                    }
                },
            )
        }
    }
}
