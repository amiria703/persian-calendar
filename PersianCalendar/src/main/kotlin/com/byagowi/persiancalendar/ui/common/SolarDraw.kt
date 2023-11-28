package com.byagowi.persiancalendar.ui.common

import android.animation.ArgbEvaluator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.withRotation
import com.byagowi.persiancalendar.R
import com.byagowi.persiancalendar.ui.utils.getCompatDrawable
import io.github.cosinekitty.astronomy.Ecliptic
import io.github.cosinekitty.astronomy.Spherical
import kotlin.math.abs
import kotlin.math.cos

class SolarDraw(context: Context) {
    private val argbEvaluator = ArgbEvaluator()
    fun sunColor(progress: Float): Int =
        (argbEvaluator.evaluate(progress, 0xFFFFF9C4.toInt(), 0xFFFF9100.toInt()) as? Int) ?: 0

    fun sun(
        canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int? = null, small: Boolean = false,
        alpha: Int = 255
    ) {
        val drawable = if (small) smallSunDrawable else sunDrawable
        drawable.alpha = alpha
        drawable.setTintList(color?.let(ColorStateList::valueOf))
        drawable.setBounds((cx - r).toInt(), (cy - r).toInt(), (cx + r).toInt(), (cy + r).toInt())
        drawable.draw(canvas)
    }

    private val sunDrawable = context.getCompatDrawable(R.drawable.ic_sun)
    private val smallSunDrawable = context.getCompatDrawable(R.drawable.ic_sun_small)

    fun moon(
        canvas: Canvas, sun: Ecliptic, moon: Spherical, cx: Float, cy: Float, r: Float,
        angle: Float? = null, moonAltitude: Double? = null
    ) {
        val alpha =
            if (moonAltitude == null) 255 else (200 + moonAltitude.toInt() * 3).coerceIn(30, 255)
        moonShadowPaint.alpha = alpha
        moonDrawable.setBounds( // same as above
            (cx - r).toInt(), (cy - r).toInt(), (cx + r).toInt(), (cy + r).toInt()
        )
        moonDrawable.draw(canvas)
        moonDrawable.alpha = alpha
        val phase = (moon.lon - sun.elon).let { it + if (it < 0) 360 else 0 }
        canvas.withRotation(angle ?: if (phase < 180.0) 180f else 0f, cx, cy) {
            val sr = r * .97f
            val arcWidth = (cos(Math.toRadians(phase)) * sr).toFloat()
            moonRect.set(cx - sr, cy - sr, cx + sr, cy + sr)
            moonOval.set(cx - abs(arcWidth), cy - sr, cx + abs(arcWidth), cy + sr)
            ovalPath.rewind()
            ovalPath.arcTo(moonOval, 90f, if (arcWidth > 0) 180f else -180f)
            ovalPath.arcTo(moonRect, 270f, 180f)
            ovalPath.close()
            drawPath(ovalPath, moonShadowPaint)
        }
    }

    fun simpleMoon(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        moonDrawable.alpha = 255
        moonDrawable.setBounds(
            (cx - r).toInt(), (cy - r).toInt(), (cx + r).toInt(), (cy + r).toInt()
        )
        moonDrawable.draw(canvas)
    }

    private val moonDrawable = context.getCompatDrawable(R.drawable.ic_moon)
    private val ovalPath = Path()
    private val moonRect = RectF()
    private val moonOval = RectF()

    private val moonShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).also {
        it.color = 0x90000000.toInt()
        it.style = Paint.Style.FILL_AND_STROKE
    }

    private val earthDrawable = context.getCompatDrawable(R.drawable.ic_earth)

    fun earth(canvas: Canvas, cx: Float, cy: Float, r: Float, sunEcliptic: Ecliptic) {
        earthRect.set(cx - r, cy - r, cx + r, cy + r)
        earthDrawable.setBounds(
            (cx - r).toInt(), (cy - r).toInt(), (cx + r).toInt(), (cy + r).toInt()
        )
        earthDrawable.draw(canvas)
        earthRect.inset(r / 18, r / 18)
        val sunDegree = -sunEcliptic.elon.toFloat()
        canvas.drawArc(earthRect, sunDegree + 90f, 180f, true, earthShadowPaint)
    }

    private val earthRect = RectF()
    private val earthShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).also {
        it.color = 0x40000000
        it.style = Paint.Style.FILL_AND_STROKE
    }
}
