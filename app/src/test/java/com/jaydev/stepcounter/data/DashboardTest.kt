package com.jaydev.stepcounter.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class DashboardTest {
    private val seoul=ZoneId.of("Asia/Seoul")
    private val now=Instant.parse("2026-10-17T05:32:00Z")
    private fun activity(date: String="2026-10-17",steps: Long=12345)=DailyActivity(LocalDate.parse(date),seoul.id,steps,246.0,5800.0,now.toEpochMilli())

    @Test fun `ring caps at target while number keeps all digits`() {
        val p=Dashboard(activity(steps=123456)).presentation(now,seoul)
        assertEquals("123,456",p.stepText)
        assertEquals(1f,p.progress,0f)
    }
    @Test fun `new day hides yesterday metrics without inventing zero`() {
        val p=Dashboard(activity("2026-10-16")).presentation(now,seoul)
        assertNull(p.steps);assertNull(p.calories);assertNull(p.distanceMeters)
        assertEquals("—",p.stepText)
        assertTrue(p.status.contains("오늘 데이터"))
    }
    @Test fun `timezone change invalidates daily aggregate`() {
        val p=Dashboard(activity()).presentation(now,ZoneId.of("Europe/London"))
        assertNull(p.steps)
    }
    @Test fun `failed refresh retains last successful values and timestamp`() {
        val p=Dashboard(activity(),message="연동 실패").presentation(now,seoul)
        assertEquals(12345L,p.steps)
        assertEquals("연동 실패",p.status)
        assertEquals("10/17 14:32",p.lastSync)
    }
    @Test fun `missing optional activity data is unavailable rather than zero`() {
        val p=Dashboard(activity().copy(calories=null,distanceMeters=null)).presentation(now,seoul)
        assertEquals("—",p.calorieText);assertEquals("—",p.distanceText)
    }
    @Test fun `abandoned sync does not leave a permanent spinner`() {
        val p=Dashboard(activity(),syncStartedAt=now.minusSeconds(121).toEpochMilli()).presentation(now,seoul)
        assertFalse(p.syncing)
    }
    @Test fun `stale data is labelled`() {
        val p=Dashboard(activity().copy(syncedAt=now.minusSeconds(3600).toEpochMilli())).presentation(now,seoul)
        assertTrue(p.status.contains("오래된 데이터"))
    }
    @Test fun `preview is explicit and does not pretend to have a sync timestamp`() {
        val p=Dashboard(previewSteps=8432).presentation(now,seoul)
        assertTrue(p.demo); assertTrue(p.status.contains("예시 데이터"));assertNull(p.lastSync)
        assertEquals(.8432f,p.progress,.00001f)
    }
    @Test fun `large Long value renders fully without integer overflow`() {
        val p=Dashboard(activity(steps=Long.MAX_VALUE)).presentation(now,seoul)
        assertEquals("9,223,372,036,854,775,807",p.stepText)
        assertEquals(1f,p.progress,0f)
    }
}
