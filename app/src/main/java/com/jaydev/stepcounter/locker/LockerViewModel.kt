package com.jaydev.stepcounter.locker

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.text.format.DateFormat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LockerUiState(
    val settings: LockerSettings = LockerSettings(),
    val clock: String = "",
    val date: String = "",
    val weatherText: String = "날씨를 설정해주세요",
    val weatherTime: String = "",
    val busy: Boolean = false,
    val message: String? = null,
    val locationText: String = "현재 위치",
)
private data class LockerOperation(val busy: Boolean = false, val message: String? = null)

class LockerViewModel(application: Application) : AndroidViewModel(application) {
    val store = LockerStore(application)
    private val operation = MutableStateFlow(LockerOperation())
    private val ticker = flow { while (true) { emit(System.currentTimeMillis()); delay(1_000) } }
    val state = combine(store.state, ticker, operation) { settings, now, op ->
        val weather = settings.weather.takeIf { settings.weatherEnabled }
        val localTime = weather?.let { Instant.ofEpochMilli(it.observedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MM.dd HH:mm")) }
        LockerUiState(settings,
            DateFormat.getTimeFormat(application).format(Date(now)),
            Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)),
            if (!settings.weatherEnabled) "날씨 꺼짐" else weather?.let { (if (it.stale(now)) "이전 날씨 · " else "") + it.description() } ?: "현재 위치 날씨를 불러와주세요",
            localTime?.let { "$it 기준" }.orEmpty(), op.busy, op.message,
            locationText = weather?.city?.takeIf { it.isNotBlank() } ?: "현재 위치 · 도시 확인 불가",
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LockerUiState())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val background = store.state.map { it.backgroundPath }.distinctUntilChanged().mapLatest { path ->
        withContext(Dispatchers.IO) { path?.let { BitmapFactory.decodeFile(it) } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun message(value: String) { operation.update { it.copy(message = value) } }

    fun refreshWeather(force: Boolean = false) {
        val settings = store.snapshot()
        if (!settings.weatherEnabled || operation.value.busy) return
        val cached = settings.weather
        if (!force && cached != null && cached.cityLookupAttempted && System.currentTimeMillis() - cached.fetchedAt in 0 until 30 * 60 * 1000L) return
        operation.value = LockerOperation(busy = true)
        viewModelScope.launch {
            try {
                val weather = WeatherRepository(getApplication()).loadCurrentLocation()
                // A request that outlives an explicit disable must not restore cached location data.
                if (store.snapshot().weatherEnabled) store.weather(weather)
                operation.value = LockerOperation()
            } catch (_: TimeoutCancellationException) {
                operation.value = LockerOperation(message = "위치 확인이 늦습니다. 위치 설정을 확인한 뒤 다시 시도해주세요")
            } catch (error: CancellationException) { throw error }
            catch (_: SecurityException) {
                operation.value = LockerOperation(message = "설정에서 대략적인 위치 권한을 허용해주세요")
            } catch (_: Exception) {
                operation.value = LockerOperation(message = "날씨 조회 실패 · 인터넷과 위치 서비스를 확인해주세요")
            } finally { operation.update { it.copy(busy = false) } }
        }
    }

    fun enableWeather(value: Boolean) {
        store.weatherEnabled(value)
        if (value) refreshWeather(force = true)
    }

    fun background(uri: Uri?) {
        if (operation.value.busy) return
        viewModelScope.launch {
            operation.value = LockerOperation(busy = true)
            try {
                val old = store.snapshot().backgroundPath
                val path = withContext(Dispatchers.IO) {
                    if (uri == null) null else {
                        val source = ImageDecoder.createSource(getApplication<Application>().contentResolver, uri)
                        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                            val sample = ((maxOf(info.size.width, info.size.height) + 1919) / 1920).coerceAtLeast(1)
                            decoder.setTargetSampleSize(sample)
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        }
                        val file = File(getApplication<Application>().filesDir, "locker-background-${System.nanoTime()}.jpg")
                        try {
                            file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
                            file.absolutePath
                        } catch (error: Exception) { file.delete(); throw error }
                        finally { bitmap.recycle() }
                    }
                }
                store.background(path)
                withContext(Dispatchers.IO) { old?.let { File(it).delete() } }
                operation.value = LockerOperation(message = if (path == null) "기본 배경으로 변경했습니다" else "배경 사진을 변경했습니다")
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { operation.value = LockerOperation(message = "사진을 불러오지 못했습니다. 다른 사진을 선택해주세요") }
            finally { operation.update { it.copy(busy = false) } }
        }
    }
}
