package com.byagowi.persiancalendar.service

import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import com.byagowi.persiancalendar.entities.Theme
import com.byagowi.persiancalendar.ui.athan.PatternDrawable
import com.byagowi.persiancalendar.utils.logException

class PersianCalendarWallpaperService : WallpaperService() {
    override fun onCreateEngine() = object : Engine() {
        private var patternDrawable = PatternDrawable()
        private val drawRunner = Runnable { draw() }
        private val handler = Handler(Looper.getMainLooper()).also { it.post(drawRunner) }
        private var visible = true
        private val bounds = Rect()
        override fun onVisibilityChanged(visible: Boolean) {
            val context = this@PersianCalendarWallpaperService
            val isNightMode = Theme.isNightMode(context)
            val accentColor = if (Theme.isDynamicColorAvailable()) context.getColor(
                if (isNightMode) android.R.color.system_accent1_200
                else android.R.color.system_accent1_400
            ) else null
            patternDrawable = PatternDrawable(
                preferredTintColor = accentColor,
                darkBaseColor = Theme.isNightMode(context)
            )
            this.visible = visible
            if (visible) handler.post(drawRunner)
            else handler.removeCallbacks(drawRunner)
        }

        private var rotationDegree = 0f
        private fun draw() {
            val surfaceHolder = surfaceHolder
            rotationDegree += .05f
            runCatching {
                val result = surfaceHolder.lockCanvas()
                result.getClipBounds(bounds)
                patternDrawable.bounds = bounds
                patternDrawable.rotationDegree = rotationDegree
                patternDrawable.draw(result)
                result
            }.onSuccess(surfaceHolder::unlockCanvasAndPost).onFailure(logException)
            handler.removeCallbacks(drawRunner)
            if (visible) handler.postDelayed(drawRunner, 1000 / 10)
        }
    }
}
