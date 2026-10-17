package com.jaydev.stepcounter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaydev.stepcounter.data.Dashboard
import com.jaydev.stepcounter.data.Presentation

@Composable
fun DashboardScreen(
    state: Presentation,
    sdkIncluded: Boolean,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
    onPin: () -> Unit,
    modifier: Modifier = Modifier,
    onLocker: () -> Unit = {},
) {
    val dark = isSystemInDarkTheme()
    val accent = if (dark) Color(0xffa8ee79) else Color(0xff327f30)
    val surface = if (dark) Color(0xff151c17) else Color(0xfff5f8f2)
    val foreground = if (dark) Color(0xfff2f8f0) else Color(0xff15251a)
    val colors = if (dark) darkColorScheme(primary=accent, background=Color(0xff0d120f), surface=surface, onSurface=foreground, onPrimary=Color(0xff16340c), secondaryContainer=Color(0xff273823), onSecondaryContainer=accent)
        else lightColorScheme(primary=accent, background=Color(0xfffdfefa), surface=surface, onSurface=foreground, onPrimary=Color.White, secondaryContainer=Color(0xffe4eddf), onSecondaryContainer=accent)
    MaterialTheme(colorScheme = colors) {
        Surface(modifier.fillMaxSize(), color=colors.background) {
            Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal=24.dp, vertical=20.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                Column(Modifier.fillMaxWidth().widthIn(max=480.dp)) {
                    Text("STEPCOUNTER", fontSize=11.sp, letterSpacing=2.sp, color=colors.onSurfaceVariant)
                    Text("오늘도 한 걸음.", fontSize=30.sp, fontWeight=FontWeight.Bold, modifier=Modifier.padding(top=8.dp, bottom=24.dp))
                }
                ActivityCircle(state, Modifier.widthIn(max=360.dp).fillMaxWidth())
                Row(Modifier.widthIn(max=360.dp).fillMaxWidth().padding(top=14.dp).background(surface,RoundedCornerShape(22.dp)).padding(14.dp), verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if(state.demo) "디자인 미리보기" else "삼성 헬스",fontSize=12.sp,fontWeight=FontWeight.Medium)
                        Text(state.status,fontSize=11.sp,color=colors.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick=onRefresh,enabled=!state.syncing) { Text(if(state.syncing) "갱신 중…" else "새로고침",fontSize=12.sp) }
                }
                if(state.lastSync!=null && !state.demo) Text("마지막 성공: ${state.lastSync}",fontSize=11.sp,color=colors.onSurfaceVariant,modifier=Modifier.padding(top=8.dp))
                Spacer(Modifier.height(22.dp))
                Button(onClick=onPin,modifier=Modifier.widthIn(max=360.dp).fillMaxWidth().heightIn(min=50.dp)) { Text("홈 화면에 위젯 추가") }
                OutlinedButton(onClick=onConnect,modifier=Modifier.widthIn(max=360.dp).fillMaxWidth().heightIn(min=48.dp)) { Text("삼성 헬스 연결 · 읽기 권한") }
                Text("자동 동기화 15분 · 절전 상태에서는 지연될 수 있어요",fontSize=11.sp,color=colors.onSurfaceVariant,modifier=Modifier.padding(top=10.dp))
                OutlinedButton(onClick=onLocker,modifier=Modifier.widthIn(max=360.dp).fillMaxWidth().heightIn(min=48.dp)) { Text("잠금화면 모드 · 배경과 날씨") }
                if (!sdkIncluded) Text("검수용 빌드 · 삼성 SDK가 아직 포함되지 않았습니다. 실제 건강 데이터는 읽지 않습니다.",fontSize=12.sp,color=colors.onSurfaceVariant,modifier=Modifier.widthIn(max=360.dp).fillMaxWidth().padding(top=16.dp))
            }
        }
    }
}

@Composable
fun ActivityCircle(state: Presentation, modifier: Modifier = Modifier) {
    val dark=isSystemInDarkTheme()
    val accent=MaterialTheme.colorScheme.primary
    val track=if(dark) Color(0xff2b3c2c) else Color(0xffe0e9dc)
    BoxWithConstraints(modifier.aspectRatio(1f).background(MaterialTheme.colorScheme.surface,CircleShape),contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize().semantics { contentDescription="하루 목표 10,000보 중 ${(state.progress*100).toInt()}% 달성" }) {
            val width=size.minDimension*.034f
            val inset=width/2+size.minDimension*.025f
            val arcSize=Size(size.width-inset*2,size.height-inset*2)
            drawArc(track,0f,360f,false,Offset(inset,inset),arcSize,style=Stroke(width))
            if(state.progress>0) drawArc(accent,-90f,360*state.progress,false,Offset(inset,inset),arcSize,style=Stroke(width,cap=StrokeCap.Round))
        }
        val numberSize=minOf(maxWidth.value*.20f,maxWidth.value*.70f/(state.stepText.length*.64f))
        Column(Modifier.fillMaxWidth(.75f),horizontalAlignment=Alignment.CenterHorizontally) {
            Text("오늘 걸음",fontSize=14.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(state.stepText,fontSize=numberSize.sp,lineHeight=(numberSize*1.08).sp,fontWeight=FontWeight.ExtraBold,letterSpacing=(-2).sp,maxLines=1)
            Text("걸음",fontSize=15.sp)
            Text(state.goalText,fontSize=12.sp,color=accent,modifier=Modifier.padding(top=8.dp))
            Row(Modifier.fillMaxWidth().padding(top=14.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
                Column(horizontalAlignment=Alignment.CenterHorizontally) { Text("활동 칼로리",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant); Text("${state.calorieText} kcal",fontSize=17.sp,fontWeight=FontWeight.SemiBold) }
                Column(horizontalAlignment=Alignment.CenterHorizontally) { Text("이동 거리",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant); Text("${state.distanceText} km",fontSize=17.sp,fontWeight=FontWeight.SemiBold) }
            }
        }
    }
}

@Preview(showBackground=true, locale="ko", widthDp=390, heightDp=940)
@Composable
private fun DashboardPreview() {
    DashboardScreen(Dashboard(previewSteps=12345).presentation(),true,{},{},{})
}
