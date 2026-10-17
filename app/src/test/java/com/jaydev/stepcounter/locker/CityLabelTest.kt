package com.jaydev.stepcounter.locker

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class CityLabelTest {
    @Test fun displaysCityWithRegionWithoutRepeatingNames() {
        assertEquals("경기도 수원시", cityLabel("수원시", "팔달구", "경기도"))
        assertEquals("서울특별시", cityLabel("서울특별시", "중구", "서울특별시"))
    }
    @Test fun fallsBackToAvailableAdministrativeAreaWithoutInventingCity() {
        assertEquals("서울특별시 중구", cityLabel(null, "중구", "서울특별시"))
        assertEquals("부산광역시", cityLabel(" ", null, " 부산광역시 "))
        assertNull(cityLabel(null, " ", null))
    }
    @Test fun oldWeatherCacheWithoutCityRemainsReadable() {
        val old = Gson().fromJson("""{"temperature":18,"code":0,"observedAt":1,"fetchedAt":1}""", WeatherSnapshot::class.java)
        assertNull(old.city)
        assertEquals("18° · 맑음", old.description())
    }
}
