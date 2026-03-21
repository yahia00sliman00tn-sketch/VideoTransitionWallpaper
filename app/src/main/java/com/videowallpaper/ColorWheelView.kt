package com.videowallpaper

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class ColorWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f
    private var selectedX = 0f
    private var selectedY = 0f
    private var brightness = 1f

    var onColorChanged: ((Int) -> Unit)? = null
    var selectedColor = Color.HSVToColor(floatArrayOf(270f, 1f, 1f))
        private set

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        radius = minOf(w, h) / 2f - 16f
        // ابدأ في المنتصف
        selectedX = centerX
        selectedY = centerY
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // ارسم عجلة الألوان
        val sweepGradient = SweepGradient(
            centerX, centerY,
            intArrayOf(
                Color.RED, Color.YELLOW, Color.GREEN,
                Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
            ),
            null
        )

        paint.shader = sweepGradient
        canvas.drawCircle(centerX, centerY, radius, paint)

        // أضف gradient من الأبيض للشفاف من المركز
        val radialGradient = RadialGradient(
            centerX, centerY, radius,
            Color.WHITE, Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        paint.shader = radialGradient
        canvas.drawCircle(centerX, centerY, radius, paint)

        // أضف تأثير السطوع
        paint.shader = null
        paint.color = Color.argb((255 * (1 - brightness)).toInt(), 0, 0, 0)
        canvas.drawCircle(centerX, centerY, radius, paint)

        // ارسم دائرة الاختيار
        paint.shader = null
        paint.color = selectedColor
        paint.style = Paint.Style.FILL
        canvas.drawCircle(selectedX, selectedY, 18f, paint)

        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawCircle(selectedX, selectedY, 18f, paint)
        paint.style = Paint.Style.FILL
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x - centerX
        val y = event.y - centerY
        val dist = sqrt(x * x + y * y)

        if (dist <= radius) {
            selectedX = event.x
            selectedY = event.y

            // احسب الـ hue من الزاوية
            var angle = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
            if (angle < 0) angle += 360f

            // احسب الـ saturation من المسافة
            val saturation = (dist / radius).coerceIn(0f, 1f)

            selectedColor = Color.HSVToColor(floatArrayOf(angle, saturation, brightness))
            onColorChanged?.invoke(selectedColor)
            invalidate()
        }
        return true
    }

    fun setBrightness(value: Float) {
        brightness = value.coerceIn(0f, 1f)
        // أعد حساب اللون
        val x = selectedX - centerX
        val y = selectedY - centerY
        val dist = sqrt(x * x + y * y)
        var angle = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
        if (angle < 0) angle += 360f
        val saturation = (dist / radius).coerceIn(0f, 1f)
        selectedColor = Color.HSVToColor(floatArrayOf(angle, saturation, brightness))
        onColorChanged?.invoke(selectedColor)
        invalidate()
    }
}
