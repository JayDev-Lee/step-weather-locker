package com.jaydev.stepcounter.locker

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaydev.stepcounter.data.Presentation
import com.jaydev.stepcounter.ui.ActivityCircle

private val lockerColors = darkColorScheme(
    primary = Color(0xffa8ee79), onPrimary = Color(0xff16340c),
    background = Color(0xff0d120f), onBackground = Color(0xfff2f8f0),
    surface = Color(0xff151c17), onSurface = Color(0xfff2f8f0),
    onSurfaceVariant = Color(0xffb6c3b2), secondaryContainer = Color(0xff273823),
    onSecondaryContainer = Color(0xffa8ee79),
)

@Composable
fun LockerScreen(
    state: LockerUiState, activity: Presentation, background: Bitmap?,
    onRefresh: () -> Unit, onClose: () -> Unit,
) {
    MaterialTheme(colorScheme = lockerColors) {
        CompositionLocalProvider(LocalContentColor provides lockerColors.onSurface) {
        Box(Modifier.fillMaxSize().background(lockerColors.background)) {
            if (background != null) Image(background.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.35f), Color.Black.copy(alpha=.65f)))))
            Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal=24.dp, vertical=20.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                Text(state.clock, color=Color.White, fontSize=68.sp, fontWeight=FontWeight.Light, lineHeight=78.sp, modifier=Modifier.padding(top=12.dp))
                Text(state.date, color=Color.White, fontSize=16.sp)
                if (state.settings.weatherEnabled) {
                    Text(state.locationText, color=Color(0xffdfebd9), fontSize=14.sp, modifier=Modifier.padding(top=16.dp))
                    Row(Modifier.padding(top=6.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        state.settings.weather?.let { WeatherIcon(it, Modifier.size(36.dp)) }
                        Text(state.weatherText, color=Color.White, fontSize=20.sp)
                    }
                    if (state.weatherTime.isNotEmpty()) Text(state.weatherTime, color=Color(0xffdfebd9), fontSize=11.sp)
                }
                Spacer(Modifier.height(24.dp))
                ActivityCircle(activity, Modifier.widthIn(max=340.dp).fillMaxWidth())
                Row(Modifier.widthIn(max=360.dp).fillMaxWidth().padding(top=16.dp).background(lockerColors.surface,RoundedCornerShape(22.dp)).padding(14.dp), verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (activity.demo) "예시 데이터" else "삼성 헬스", color=lockerColors.onSurface, fontSize=12.sp)
                        Text(activity.status, color=lockerColors.onSurfaceVariant, fontSize=11.sp)
                    }
                    FilledTonalButton(onClick=onRefresh, enabled=!activity.syncing && !state.busy) { Text(if (activity.syncing || state.busy) "갱신 중…" else "새로고침", fontSize=12.sp) }
                }
                state.message?.let { Text(it, color=Color.White, fontSize=12.sp, modifier=Modifier.padding(top=10.dp)) }
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick=onClose, modifier=Modifier.heightIn(min=48.dp)) { Text("기본 잠금화면으로", color=Color.White) }
                Text("지문 · PIN 잠금 유지", color=Color(0xffdfebd9), fontSize=11.sp, modifier=Modifier.padding(top=8.dp))
                if (state.settings.weatherEnabled) Text("Weather data: Open-Meteo · CC BY 4.0", color=Color(0xffdfebd9), fontSize=10.sp, modifier=Modifier.padding(top=12.dp))
            }
        }
        }
    }
}

@Composable
fun LockerSettingsScreen(
    state: LockerUiState, background: Bitmap?, onEnabled: (Boolean) -> Unit,
    onPhoto: () -> Unit, onReset: () -> Unit, onWeather: (Boolean) -> Unit,
    onRefreshWeather: () -> Unit, onPreview: () -> Unit, onBack: () -> Unit,
    onLocationSettings: () -> Unit,
) {
    MaterialTheme(colorScheme = lockerColors) {
        Surface(Modifier.fillMaxSize(), color=lockerColors.background) {
            Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
                TextButton(onClick=onBack) { Text("돌아가기") }
                Text("나만의 잠금화면", fontSize=28.sp, fontWeight=FontWeight.Bold)
                Text("화면을 켜면 걸음 수와 시계·날씨를 보여줘요.", color=lockerColors.onSurfaceVariant, fontSize=14.sp)
                Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                    Text("잠금화면 자동 표시", Modifier.weight(1f))
                    Switch(checked=state.settings.enabled, onCheckedChange=onEnabled)
                }
                Text("켜면 잠금 상태에서도 걸음 수·활동 정보가 표시돼요. 다른 앱 위에 표시 권한과 상태 알림을 사용합니다. 알림에서 언제든 끌 수 있어요.", fontSize=12.sp, color=lockerColors.onSurfaceVariant)
                Text("기기 재시작·앱 강제 종료 후에는 여기서 다시 켜주세요. One UI가 자동 실행을 제한하면 상태 알림을 눌러 열 수 있어요.", fontSize=12.sp, color=lockerColors.onSurfaceVariant)
                Text("자동 표시 상태", fontSize=13.sp, fontWeight=FontWeight.SemiBold)
                Text(state.settings.autoStatus, fontSize=12.sp, color=lockerColors.primary)
                HorizontalDivider()
                Text("배경 사진", fontWeight=FontWeight.SemiBold)
                if (background != null) Image(background.asImageBitmap(), "선택한 배경", Modifier.fillMaxWidth().height(150.dp), contentScale=ContentScale.Crop)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick=onPhoto,enabled=!state.busy) { Text("사진 선택") }
                    TextButton(onClick=onReset,enabled=!state.busy && background!=null) { Text("기본 배경") }
                }
                Text("사진은 앱 안에 보관하며, 잠금화면 글자가 잘 보이도록 어둡게 표시해요.", fontSize=12.sp, color=lockerColors.onSurfaceVariant)
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                    Text("현재 위치 날씨", Modifier.weight(1f))
                    Switch(checked=state.settings.weatherEnabled,onCheckedChange=onWeather, enabled=!state.busy)
                }
                Text("도시명은 기기의 위치를 주소로 변환해 확인합니다. ‘정확한 위치’를 허용하면 시 경계에서 다른 도시로 표시되는 현상을 줄일 수 있어요. 날씨 서비스에는 반올림한 좌표만 보내고, 앱에는 좌표를 저장하지 않아요.", fontSize=12.sp, color=lockerColors.onSurfaceVariant)
                TextButton(onClick=onLocationSettings) { Text("위치 권한 · 정확도 설정") }
                Text("권한 → 위치 → 앱 사용 중 허용 · 정확한 위치 사용", fontSize=12.sp, color=lockerColors.onSurfaceVariant)
                if (state.settings.weatherEnabled) {
                    Text(state.locationText, fontSize=14.sp, color=lockerColors.onSurfaceVariant)
                    Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        state.settings.weather?.let { WeatherIcon(it, Modifier.size(32.dp)) }
                        Text(state.weatherText)
                    }
                    Text(state.weatherTime, fontSize=12.sp, color=lockerColors.onSurfaceVariant)
                    OutlinedButton(onClick=onRefreshWeather, enabled=!state.busy) { Text(if (state.busy) "날씨 확인 중…" else "현재 위치로 날씨 갱신") }
                    Text("화면이 열릴 때 30분 이상 지난 날씨를 갱신해요. 조회 실패 시 기존 값과 기준 시각을 표시합니다.\nWeather data: Open-Meteo · CC BY 4.0", fontSize=12.sp, color=lockerColors.onSurfaceVariant)
                }
                state.message?.let { Text(it, color=lockerColors.primary, fontSize=13.sp) }
                Button(onClick=onPreview, modifier=Modifier.fillMaxWidth().heightIn(min=50.dp)) { Text("잠금화면 미리보기") }
            }
        }
    }
}
