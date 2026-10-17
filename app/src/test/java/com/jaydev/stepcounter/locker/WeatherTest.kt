package com.jaydev.stepcounter.locker

import org.junit.Assert.*
import org.junit.Test

class WeatherTest {
    @Test fun parsesObservationTimeAndNegativeTemperature() {
        val weather = parseWeather("""{"current":{"temperature_2m":-3.6,"weather_code":73,"time":1700000000}}""", 1700000010000)
        assertEquals("-4° · 눈", weather.description())
        assertEquals(1700000000000L, weather.observedAt)
        assertNull(weather.isDay)
        assertFalse(weather.stale(1700000010000))
        assertTrue(weather.stale(1700000010000 + 2 * 60 * 60 * 1000L + 1))
    }
    @Test fun parsesNightAndDayForWeatherIcons() {
        val night = parseWeather("""{"current":{"temperature_2m":18,"weather_code":0,"time":1700000000,"is_day":0}}""", 1)
        val day = parseWeather("""{"current":{"temperature_2m":18,"weather_code":0,"time":1700000000,"is_day":1}}""", 1)
        assertEquals(false, night.isDay)
        assertEquals(true, day.isDay)
    }
    @Test fun mapsRainThunderAndUnknownWithoutInventingClearWeather() {
        assertEquals("비", weatherDescription(82))
        assertEquals("뇌우", weatherDescription(99))
        assertEquals("날씨 확인 중", weatherDescription(500))
    }
    @Test(expected = RuntimeException::class) fun rejectsMissingTemperature() {
        parseWeather("""{"current":{"weather_code":0,"time":1700000000}}""", 1)
    }
}
