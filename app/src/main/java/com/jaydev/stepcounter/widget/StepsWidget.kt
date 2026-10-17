package com.jaydev.stepcounter.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jaydev.stepcounter.MainActivity
import com.jaydev.stepcounter.R
import com.jaydev.stepcounter.data.DashboardStore
import com.jaydev.stepcounter.data.Presentation
import com.jaydev.stepcounter.sync.SyncScheduler
import kotlinx.coroutines.flow.first
import kotlin.math.min

open class StepsWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(200.dp, 260.dp), DpSize(240.dp, 310.dp), DpSize(300.dp, 370.dp), DpSize(360.dp, 430.dp)))
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = DashboardStore(context)
        val initial = store.state.first()
        provideContent {
            // A Glance session can outlive update(). Observe changes instead of capturing
            // a single snapshot outside provideContent, which leaves an active widget stale.
            val dashboard by store.state.collectAsState(initial = initial)
            val snapshot = dashboard.presentation()
            val dark = LocalContext.current.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            val bitmap = remember(snapshot.progress, dark) { ringBitmap(snapshot.progress, dark) }
            val size = LocalSize.current
            val diameter = min(size.width.value, size.height.value - 72).coerceAtLeast(160f).dp
            val text = if (dark) Color(0xfff2f8f0) else Color(0xff15251a)
            val muted = if (dark) Color(0xffacbba7) else Color(0xff526451)
            val accent = if (dark) Color(0xffa8ee79) else Color(0xff327f30)
            Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
                Box(GlanceModifier.size(diameter).clickable(actionStartActivity(Intent(context, MainActivity::class.java))), contentAlignment = Alignment.Center) {
                    Image(ImageProvider(bitmap), contentDescription = null, modifier = GlanceModifier.fillMaxSize())
                    Column(GlanceModifier.width(diameter * .75f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Label("오늘 걸음", 14f, muted)
                        val numberSize = min(diameter.value * .20f, diameter.value * .70f / (snapshot.stepText.length * .64f)).coerceAtLeast(8f)
                        Label(snapshot.stepText, numberSize, text, true)
                        Label("걸음", 15f, text)
                        Spacer(GlanceModifier.height(7.dp))
                        Label(snapshot.goalText, 12f, accent)
                        Spacer(GlanceModifier.height(12.dp))
                        Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Label("활동 칼로리", 11f, muted)
                                Label("${snapshot.calorieText} kcal", 16f, text, true)
                            }
                            Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Label("이동 거리", 11f, muted)
                                Label("${snapshot.distanceText} km", 16f, text, true)
                            }
                        }
                    }
                }
                Spacer(GlanceModifier.height(8.dp))
                Row(GlanceModifier.width(diameter).height(60.dp).background(ImageProvider(R.drawable.widget_footer)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(GlanceModifier.defaultWeight().clickable(actionStartActivity(Intent(context, MainActivity::class.java)))) {
                        Label(if (snapshot.demo) "예시 데이터" else "삼성 헬스", 11f, text)
                        Label(snapshot.status, 11f, muted, maxLines = 2)
                    }
                    Button(
                        text = if (snapshot.syncing) "갱신 중…" else "새로고침",
                        onClick = actionRunCallback<RefreshAction>(),
                        enabled = !snapshot.syncing,
                        modifier = GlanceModifier.height(48.dp),
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = ColorProvider(if (dark) Color(0xff273823) else Color(0xffe4eddf)),
                            contentColor = ColorProvider(accent),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun Label(value: String, size: Float, color: Color, bold: Boolean = false, maxLines: Int = 1) {
    Text(value, style = TextStyle(color = ColorProvider(color), fontSize = size.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center), maxLines = maxLines)
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        SyncScheduler.schedule(context)
        SyncScheduler.refresh(context)
    }
}

open class StepsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StepsWidget()
    override fun onUpdate(context: Context, appWidgetManager: android.appwidget.AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        SyncScheduler.schedule(context)
        SyncScheduler.refresh(context)
    }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        val manager = android.appwidget.AppWidgetManager.getInstance(context)
        val other = if (this is LargeStepsWidgetReceiver) StepsWidgetReceiver::class.java else LargeStepsWidgetReceiver::class.java
        if (manager.getAppWidgetIds(android.content.ComponentName(context, other)).isEmpty()) SyncScheduler.cancel(context)
    }
}
class LargeStepsWidget : StepsWidget()
class LargeStepsWidgetReceiver : StepsWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LargeStepsWidget()
}

suspend fun refreshWidgets(context: Context) {
    StepsWidget().updateAll(context)
    LargeStepsWidget().updateAll(context)
}
