package com.jaydev.stepcounter.locker

import android.app.*
import android.content.*
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class LockerService : Service() {
    private lateinit var store: LockerStore
    private var registered = false
    private val handler = Handler(Looper.getMainLooper())
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            handler.removeCallbacksAndMessages(null)
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    store.autoStatus("화면 켜짐 감지 · 잠금 상태 확인 중")
                    attemptDisplay(0)
                }
            }
        }
    }

    private fun attemptDisplay(check: Int) {
        if (!store.snapshot().enabled || !getSystemService(PowerManager::class.java).isInteractive) return
        if (!Settings.canDrawOverlays(this)) {
            store.autoStatus("다른 앱 위에 표시 권한이 꺼져 있습니다")
            return
        }
        if (!getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
            if (check < 5) handler.postDelayed({ attemptDisplay(check + 1) }, 500)
            else store.autoStatus("잠금 상태가 감지되지 않았습니다 · 기기의 즉시 잠금 설정을 확인해주세요")
            return
        }
        val token = System.currentTimeMillis()
        store.autoStatus("잠금 감지 · 화면 실행 요청")
        try {
            startActivity(Intent(this, LockerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra(EXTRA_AUTO_TOKEN, token))
            handler.postDelayed({
                if (!store.wasAutoShown(token)) store.autoStatus("실행 요청 후 화면 표시가 확인되지 않았습니다 · 상태 알림을 눌러 열어주세요")
            }, 2_000)
        } catch (_: RuntimeException) {
            store.autoStatus("자동 실행 요청이 거부되었습니다 · 상태 알림을 눌러 열어주세요")
        }
    }

    override fun onCreate() {
        super.onCreate()
        store = LockerStore(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || !store.snapshot().enabled || !Settings.canDrawOverlays(this)) {
            store.enabled(false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val channelId = "lock-screen-mode"
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(channelId, "잠금화면 표시 모드", NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(this, 1, Intent(this, LockerActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 2, Intent(this, LockerService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("한걸음 잠금화면 모드")
            .setContentText("화면을 켜면 표시 · 눌러서 열기").setContentIntent(open)
            .setOngoing(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .addAction(0, "모드 끄기", stop).build()
        startForeground(41, notification)
        if (!registered) {
            ContextCompat.registerReceiver(this, screenReceiver, IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }, ContextCompat.RECEIVER_NOT_EXPORTED)
            registered = true
        }
        store.autoStatus("자동 표시 준비 완료 · 화면을 껐다 켜주세요")
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        store.autoStatus("자동 표시 서비스 종료됨 · 앱에서 모드를 다시 켜주세요")
        if (registered) unregisterReceiver(screenReceiver)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object {
        const val ACTION_STOP = "com.jaydev.stepcounter.STOP_LOCKER"
        const val EXTRA_AUTO_TOKEN = "auto_launch_token"
    }
}
