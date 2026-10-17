package com.jaydev.stepcounter.locker

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.cos
import kotlin.math.sin

// Decorative: the adjacent weather description provides the accessible label.
@Composable
fun WeatherIcon(weather: WeatherSnapshot, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        withTransform({ scale(size.width / 48f, size.height / 48f, pivot = Offset.Zero) }) {
            val sun = Color(0xffffd46b)
            val cloud = Color(0xffd5e5ef)
            val water = Color(0xff83c8ff)
            fun line(x: Float, y: Float, x2: Float, y2: Float, color: Color = water) {
                drawLine(color, Offset(x, y), Offset(x2, y2), strokeWidth=2.8f, cap=StrokeCap.Round)
            }
            fun clear(center: Offset, radius: Float) {
                if (weather.isDay == false) {
                    // A crescent silhouette; no background-colored cutout over photographs.
                    val moon = Path().apply {
                        moveTo(center.x + radius*.5f, center.y-radius)
                        cubicTo(center.x-radius*1.7f, center.y-radius, center.x-radius*1.7f, center.y+radius*1.5f, center.x+radius*.6f, center.y+radius)
                        cubicTo(center.x-radius*.5f, center.y+radius*.5f, center.x-radius*.8f, center.y-radius*.2f, center.x+radius*.5f, center.y-radius)
                        close()
                    }
                    drawPath(moon, sun)
                } else {
                    drawCircle(sun, radius, center)
                    for (i in 0..7) {
                        val angle = i * Math.PI / 4
                        val start = center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * (radius+3)
                        val end = center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * (radius+6)
                        drawLine(sun, start, end, 2.5f, StrokeCap.Round)
                    }
                }
            }
            when (weather.code) {
                0 -> clear(Offset(25f, 24f), 11f)
                1, 2 -> { clear(Offset(17f, 17f), 7f); cloudShape(cloud) }
                3 -> cloudShape(cloud)
                45, 48 -> {
                    cloudShape(cloud)
                    line(7f,35f,36f,35f,cloud); line(13f,41f,41f,41f,cloud)
                }
                51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> {
                    cloudShape(cloud)
                    line(15f,34f,12f,41f); line(26f,34f,23f,41f); line(37f,34f,34f,41f)
                }
                71, 73, 75, 77, 85, 86 -> {
                    cloudShape(cloud)
                    for (x in listOf(15f,33f)) {
                        line(x,34f,x,43f); line(x-4f,36f,x+4f,41f); line(x-4f,41f,x+4f,36f)
                    }
                }
                95, 96, 99 -> {
                    cloudShape(cloud)
                    drawPath(Path().apply {
                        moveTo(26f,29f); lineTo(18f,39f); lineTo(25f,39f)
                        lineTo(22f,47f); lineTo(34f,34f); lineTo(27f,34f); close()
                    }, sun)
                }
                else -> {
                    drawCircle(cloud, 15f, Offset(24f,24f), style=Stroke(2f))
                    line(18f,24f,30f,24f,cloud)
                }
            }
        }
    }
}

private fun DrawScope.cloudShape(color: Color) {
    drawPath(Path().apply {
        moveTo(13f,31f)
        cubicTo(1f,31f,1f,17f,13f,17f)
        cubicTo(15f,5f,33f,6f,35f,19f)
        cubicTo(47f,18f,48f,31f,37f,31f)
        close()
    }, color)
}
