package com.mandg.funny.game.widget

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.mandg.funny.R

class LevelMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Status enum for levels
    enum class LevelStatus {
        Passed,
        Playing,
        Unpassed
    }

    // Data class representing a level node on the map
    data class LevelNode(
        val levelNum: Int,
        val posX: Float, // Relative X (0.0 to 1.0)
        val posY: Float, // Relative Y (0.0 to 1.0)
        var status: LevelStatus = LevelStatus.Unpassed,
        var stars: Int = 0
    )

    // Data class representing a map page
    data class MapPage(
        val mapId: Int,
        val bgResId: Int,
        val levels: List<LevelNode>
    )

    private var currentMapPage: MapPage? = null
    private var bgBitmap: Bitmap? = null

    private val passedBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.game_level_map_passed)
    private val playingBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.game_level_map_playing)
    private val lockedBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.game_level_map_unpassed)
    private val starOnBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.game_level_map_star_on)
    private val starOffBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.game_level_map_star_off)

    private val nodeWidth = dpToPx(56)
    private val nodeHeight = dpToPx(50)

    private val tempRect = Rect()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = spToPx(14f)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private var selectedNode: LevelNode? = null
    private var isAnimating = false
    private var animationScale = 1.0f

    interface OnLevelClickListener {
        fun onLevelClick(levelNum: Int)
    }

    private var listener: OnLevelClickListener? = null

    fun setOnLevelClickListener(listener: OnLevelClickListener) {
        this.listener = listener
    }

    fun setupLayout(page: MapPage) {
        currentMapPage = page
        bgBitmap?.recycle()
        bgBitmap = BitmapFactory.decodeResource(resources, page.bgResId)
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val bg = bgBitmap
        if (bg != null && width > 0) {
            val scale = width.toFloat() / bg.width
            val height = (bg.height * scale).toInt()
            setMeasuredDimension(width, height)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val page = currentMapPage ?: return
        val bg = bgBitmap

        // 1. Draw Map Background
        if (bg != null) {
            tempRect.set(0, 0, width, height)
            canvas.drawBitmap(bg, null, tempRect, null)
        }

        // 2. Draw Level Nodes
        for (node in page.levels) {
            val cx = (width * node.posX).toInt()
            val cy = (height * node.posY).toInt()

            canvas.save()
            canvas.translate(cx.toFloat(), cy.toFloat())

            // If this node is being clicked/animated, apply scale
            if (node == selectedNode) {
                canvas.scale(animationScale, animationScale, nodeWidth / 2f, nodeHeight / 2f)
            }

            // Draw Node Icon
            val nodeBmp = when (node.status) {
                LevelStatus.Passed -> passedBitmap
                LevelStatus.Playing -> playingBitmap
                LevelStatus.Unpassed -> lockedBitmap
            }
            tempRect.set(0, 0, nodeWidth, nodeHeight)
            canvas.drawBitmap(nodeBmp, null, tempRect, null)

            // Draw Stars (3 stars above the node)
            val starW = dpToPx(16)
            val starH = dpToPx(16)
            for (i in 0 until 3) {
                val starBmp = if (i < node.stars) starOnBitmap else starOffBitmap
                val sx = when (i) {
                    0 -> 0
                    1 -> (nodeWidth - starW) / 2
                    else -> nodeWidth - starW
                }
                val sy = when (i) {
                    0 -> (nodeHeight * 0.45f).toInt()
                    1 -> (nodeHeight * 0.3f).toInt()
                    else -> (nodeHeight * 0.45f).toInt()
                }
                tempRect.set(sx, sy, sx + starW, sy + starH)
                canvas.drawBitmap(starBmp, null, tempRect, null)
            }

            // Draw Level Number Text
            canvas.drawText(
                node.levelNum.toString(),
                nodeWidth / 2f,
                nodeHeight * 0.72f,
                textPaint
            )

            canvas.restore()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x.toInt()
        val y = event.y.toInt()

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!isAnimating) {
                    selectedNode = findNodeAt(x, y)
                }
            }
            MotionEvent.ACTION_UP -> {
                val node = selectedNode
                if (node != null && !isAnimating && findNodeAt(x, y) == node) {
                    if (node.status != LevelStatus.Unpassed) {
                        animateClick(node)
                    } else {
                        selectedNode = null
                    }
                } else {
                    selectedNode = null
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                selectedNode = null
            }
        }
        return true
    }

    private fun findNodeAt(tx: Int, ty: Int): LevelNode? {
        val page = currentMapPage ?: return null
        for (node in page.levels) {
            val cx = (width * node.posX).toInt()
            val cy = (height * node.posY).toInt()
            if (tx >= cx && ty >= cy && tx <= cx + nodeWidth && ty <= cy + nodeHeight) {
                return node
            }
        }
        return null
    }

    private fun animateClick(node: LevelNode) {
        isAnimating = true
        val animator = ValueAnimator.ofFloat(1.0f, 1.15f, 1.0f).apply {
            duration = 200
            addUpdateListener {
                animationScale = it.animatedValue as Float
                invalidate()
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    isAnimating = false
                    selectedNode = null
                    listener?.onLevelClick(node.levelNum)
                }
                override fun onAnimationCancel(animation: Animator) {
                    isAnimating = false
                    selectedNode = null
                }
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }
        animator.start()
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
    private fun spToPx(sp: Float): Float = sp * resources.displayMetrics.scaledDensity
}
