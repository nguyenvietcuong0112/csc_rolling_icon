package com.mandg.funny.game.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.mandg.funny.R

class ScoreStarsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var totalStars = 3
    private var activeStarsIndex = -1 // -1 means 0 stars, 0 means 1 star, 1 means 2 stars, 2 means 3 stars

    private val starOnBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.game_star_on)
    private val starOffBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.game_star_off)
    private val tempRect = Rect()

    fun setOnStarNum(num: Int) {
        activeStarsIndex = num - 1
        invalidate()
    }

    fun setStarNumber(num: Int) {
        totalStars = num
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width
        val h = height

        // Calculate scaled star width based on height to maintain aspect ratio
        val starWidth = (starOffBitmap.width * h) / starOffBitmap.height
        val spacing = (w - (starWidth * totalStars)) / (totalStars + 1)

        for (i in 0 until totalStars) {
            val startX = (i + 1) * spacing + i * starWidth
            tempRect.set(startX, 0, startX + starWidth, h)

            val bitmap = if (i <= activeStarsIndex) starOnBitmap else starOffBitmap
            canvas.drawBitmap(bitmap, null, tempRect, null)
        }
    }
}
