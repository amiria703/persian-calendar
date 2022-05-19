package com.byagowi.persiancalendar.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.service.dreams.DreamService
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatImageView
import androidx.customview.widget.ViewDragHelper
import com.byagowi.persiancalendar.R
import com.byagowi.persiancalendar.entities.Theme
import com.byagowi.persiancalendar.ui.athan.PatternDrawable
import kotlin.math.min
import kotlin.random.Random

class PersianCalendarDreamService : DreamService() {

    private val valueAnimator = ValueAnimator.ofFloat(0f, 360f).also {
        it.duration = 360000L
        it.interpolator = LinearInterpolator()
        it.repeatMode = ValueAnimator.RESTART
        it.repeatCount = ValueAnimator.INFINITE
    }

    private val audioTrack = run {
        val sampleRate = 22050 // Hz (maximum frequency is 7902.13Hz (B8))
        val numSamples = sampleRate * 10
        val buffer = (0..numSamples).runningFold(.0) { lastOut, _ ->
            // Brown noise https://github.com/zacharydenton/noise.js/blob/master/noise.js#L45
            (((Random.nextDouble() * 2 - 1) * .02 + lastOut) / 1.02)
        }.map { (it * Short.MAX_VALUE).toInt().toShort() }.toShortArray()
        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT, buffer.size, AudioTrack.MODE_STATIC
        )
        audioTrack.write(buffer, 0, buffer.size)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioTrack.setLoopPoints(0, audioTrack.bufferSizeInFrames, -1)
        }
        audioTrack
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = true
        isFullscreen = true

        val backgroundView = View(this).also {
            val isNightMode = Theme.isNightMode(this)
            val accentColor = if (Theme.isDynamicColorAvailable()) getColor(
                if (isNightMode) android.R.color.system_accent1_200
                else android.R.color.system_accent1_400
            ) else null
            val pattern = PatternDrawable(
                preferredTintColor = accentColor,
                darkBaseColor = Theme.isNightMode(this)
            )
            it.background = pattern
            valueAnimator.addUpdateListener {
                pattern.rotationDegree = valueAnimator.animatedValue as? Float ?: 0f
                pattern.invalidateSelf()
            }
            it.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) wakeUp()
                else finish()
            }
        }

        val button = AppCompatImageView(ContextThemeWrapper(this, R.style.LightTheme)).also {
            it.setImageResource(R.drawable.ic_play)
            var play = false
            it.setOnClickListener { _ ->
                play = !play
                it.setImageResource(if (play) R.drawable.ic_stop else R.drawable.ic_play)
                if (play) audioTrack.play() else audioTrack.pause()
            }
        }

        // Make the play/stop button movable using ViewDragHelper and ViewGroup
        val screen = object : ViewGroup(this) {
            init {
                addView(backgroundView)
                addView(button)
            }

            override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {}
            override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
                super.onSizeChanged(w, h, oldw, oldh)
                backgroundView.layout(0, 0, w, h)
                button.layout(0, 0, min(w, h) / 5, min(w, h) / 5)
            }

            // Make play button of the screen movable
            private val callback = object : ViewDragHelper.Callback() {
                override fun tryCaptureView(child: View, pointerId: Int) = child == button
                override fun onViewPositionChanged(
                    changedView: View, left: Int, top: Int, dx: Int, dy: Int
                ) = invalidate()
                override fun getViewHorizontalDragRange(child: View): Int = width
                override fun getViewVerticalDragRange(child: View): Int = height
                override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int =
                    left.coerceIn(0, width - child.width)
                override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int =
                    top.coerceIn(0, height - child.height)
                override fun onViewCaptured(capturedChild: View, activePointerId: Int) =
                    bringChildToFront(capturedChild)
            }
            private val dragHelper = ViewDragHelper.create(this, callback)

            override fun onInterceptTouchEvent(event: MotionEvent): Boolean =
                dragHelper.shouldInterceptTouchEvent(event)

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouchEvent(event: MotionEvent): Boolean {
                dragHelper.processTouchEvent(event)
                return true
            }
        }

        setContentView(screen)

        listOf(valueAnimator::start, valueAnimator::reverse).random()()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (audioTrack.state == AudioTrack.STATE_INITIALIZED) audioTrack.stop()
        valueAnimator.removeAllUpdateListeners()
        valueAnimator.cancel()
    }
}
