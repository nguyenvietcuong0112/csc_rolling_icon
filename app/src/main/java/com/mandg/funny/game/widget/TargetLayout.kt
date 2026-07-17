package com.mandg.funny.game.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.mandg.funny.R
import com.mandg.funny.widget.StrokeTextView

class TargetLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    data class Target(
        val type: Int,
        var count: Int,
        val bitmap: Bitmap?
    )

    private val targetItems = ArrayList<TargetItemView>()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
    }

    fun setupTargets(targets: List<Target>) {
        removeAllViews()
        targetItems.clear()

        for (target in targets) {
            val itemView = TargetItemView(context)
            itemView.bindTarget(target)
            targetItems.add(itemView)

            val params = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(8)
                rightMargin = dpToPx(8)
            }
            addView(itemView, params)
        }
    }

    fun updateTargetCount(type: Int, destroyedCount: Int) {
        for (item in targetItems) {
            val target = item.target ?: continue
            if (target.type == type) {
                target.count = (target.count - destroyedCount).coerceAtLeast(0)
                item.updateCountUI()
            }
        }
    }

    fun areAllTargetsReached(): Boolean {
        for (item in targetItems) {
            val target = item.target ?: continue
            if (target.count > 0) return false
        }
        return true
    }

    // Inner custom view representing a target item block
    private inner class TargetItemView(context: Context) : FrameLayout(context) {
        val blockBg: ImageView = ImageView(context).apply {
            setBackgroundResource(R.drawable.game_block_bg)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val iconView: ImageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        val countText: StrokeTextView = StrokeTextView(context).apply {
            setTextColor(0xFFFFFFFF.toInt())
            setStrokeColor(0xFF000000.toInt())
            setStrokeWidth(4f)
            textSize = 12f
            gravity = Gravity.BOTTOM or Gravity.RIGHT
        }
        val checkedIcon: ImageView = ImageView(context).apply {
            setImageResource(R.drawable.game_target_checked)
            visibility = View.GONE
        }

        var target: Target? = null

        init {
            val size = dpToPx(48)
            addView(blockBg, LayoutParams(size, size).apply { gravity = Gravity.CENTER })
            addView(iconView, LayoutParams((size * 0.8f).toInt(), (size * 0.8f).toInt()).apply { gravity = Gravity.CENTER })

            val textCheckParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.RIGHT
                rightMargin = dpToPx(2)
                bottomMargin = dpToPx(2)
            }
            addView(countText, textCheckParams)
            addView(checkedIcon, textCheckParams)
        }

        fun bindTarget(t: Target) {
            target = t
            iconView.setImageBitmap(t.bitmap)
            updateCountUI()
        }

        fun updateCountUI() {
            val t = target ?: return
            if (t.count <= 0) {
                countText.visibility = View.GONE
                checkedIcon.visibility = View.VISIBLE
            } else {
                countText.text = t.count.toString()
                countText.visibility = View.VISIBLE
                checkedIcon.visibility = View.GONE
            }
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
