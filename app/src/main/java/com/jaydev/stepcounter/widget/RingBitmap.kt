package com.jaydev.stepcounter.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/** Glance uses RemoteViews: draw only the ring; all text remains accessible Glance text. */
fun ringBitmap(progress: Float, dark: Boolean): Bitmap {
    val bitmap = Bitmap.createBitmap(480, 480, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = if (dark) 0xff151c17.toInt() else 0xfff5f8f2.toInt()
    canvas.drawCircle(240f, 240f, 238f, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 17f
    paint.color = if (dark) 0xff2b3c2c.toInt() else 0xffe0e9dc.toInt()
    val bounds = RectF(16f, 16f, 464f, 464f)
    canvas.drawOval(bounds, paint)
    if (progress > 0) {
        paint.color = if (dark) 0xffa8ee79.toInt() else 0xff327f30.toInt()
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawArc(bounds, -90f, progress.coerceIn(0f, 1f) * 360, false, paint)
    }
    return bitmap
}
