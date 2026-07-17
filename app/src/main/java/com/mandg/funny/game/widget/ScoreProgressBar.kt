package com.mandg.funny.game.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.mandg.funny.R

class ScoreProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var maxValue = 100
    private var progress = 0

    private val bgDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.game_score_bg)
    private val progressDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.game_score_progress_bar)

    fun setMaxValue(value: Int) {
        maxValue = if (value <= 0) 100 else value
        invalidate()
    }

    fun setProgress(value: Int) {
        progress = if (value < 0) 0 else value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width
        val h = height

        // Draw background track
        bgDrawable?.setBounds(0, 0, w, h)
        bgDrawable?.draw(canvas)

        // Draw filled progress track
        val fillWidth = (progress.toFloat() / maxValue * w).toInt().coerceIn(0, w)
        if (fillWidth > 0) {
            progressDrawable?.setBounds(0, 0, fillWidth, h)
            progressDrawable?.draw(canvas)
        }
    }
}
