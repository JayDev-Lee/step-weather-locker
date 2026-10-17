package com.jaydev.stepcounter.locker

import android.content.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaydev.stepcounter.ui.DashboardViewModel

class LockerActivity : ComponentActivity() {
    private val locker: LockerViewModel by viewModels()
    private val health: DashboardViewModel by viewModels()
    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) { finishAndRemoveTask() }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        // Do not wake or keep the display on, dismiss keyguard, or bypass device authentication.
        enableEdgeToEdge()
        ContextCompat.registerReceiver(this, dismissReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        setContent {
            val state by locker.state.collectAsStateWithLifecycle()
            val background by locker.background.collectAsStateWithLifecycle()
            val activity by health.state.collectAsStateWithLifecycle()
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                health.resume()
                locker.refreshWeather()
            }
            LockerScreen(state, activity, background,
                onRefresh = { health.refresh(); locker.refreshWeather(force = true) },
                onClose = { finishAndRemoveTask() },
            )
        }
    }
    override fun onDestroy() {
        unregisterReceiver(dismissReceiver)
        super.onDestroy()
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
    override fun onResume() {
        super.onResume()
        val token = intent.getLongExtra(LockerService.EXTRA_AUTO_TOKEN, 0)
        if (token != 0L) LockerStore(this).autoShown(token)
    }
}
