package com.mandg.funny.game.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import com.mandg.funny.R
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.cos
import kotlin.math.sin

class BlocksLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Representation of a block in the grid
    data class Block(
        var type: Int, // 1-5: apps, 6: vertical bomb, 7: horizontal bomb, 8: area bomb
        var bitmap: Bitmap?,
        var col: Int,
        var row: Int,
        var currentX: Float = 0f,
        var currentY: Float = 0f,
        var targetX: Float = 0f,
        var targetY: Float = 0f,
        var isFalling: Boolean = false,
        var rotation: Float = 0f
    )

    data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var color: Int,
        var size: Float,
        var alpha: Int = 255,
        var life: Float = 1.0f // 0.0 to 1.0
    )

    data class FloatingText(
        val text: String,
        var x: Float,
        var y: Float,
        var startY: Float,
        var alpha: Int = 255,
        val startTime: Long = SystemClock.uptimeMillis()
    )

    private var columns = 9
    private var rows = 9
    private var targetScore = 1000
    private var maxSteps = 20

    private var cellSize = 0
    private var blockDrawSize = 0
    private var startX = 0f
    private var startY = 0f

    private var grid = Array(9) { Array<Block?>(9) { null } }
    private var skippedCells = HashSet<Pair<Int, Int>>() // Coordinates of empty/skip grid cells
    private var blockBitmaps = HashMap<Int, Bitmap>() // Block type -> Bitmap icon

    private val particles = ArrayList<Particle>()
    private val floatingTexts = ArrayList<FloatingText>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val random = Random()
    private var soundPool: SoundPool? = null
    private var sndBreak = 0
    private var sndBomb = 0
    private var sndClick = 0

    // Bomb icon resources
    private val bombVerticalBmp: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.game_boom_vertical)
    private val bombHorizontalBmp: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.game_boom_horizontal)
    private val bombAroundBmp: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.game_boom_around)

    interface GameListener {
        fun onScoreIncremented(score: Int)
        fun onBlockDestroyed(type: Int, count: Int)
        fun onMoveMade()
        fun onGameFinished(isWon: Boolean)
    }

    private var gameListener: GameListener? = null

    fun setGameListener(listener: GameListener) {
        this.gameListener = listener
    }

    init {
        initAudio()
    }

    private fun initAudio() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(attrs)
            .build()

        // Fallback standard resource loading or silent if not found
        try {
            sndBreak = soundPool?.load(context, R.raw.sound_game_eat, 1) ?: 0
            sndBomb = soundPool?.load(context, R.raw.sound_game_boom, 1) ?: 0
            sndClick = soundPool?.load(context, R.raw.sound_click, 1) ?: 0
        } catch (e: Exception) {
            // Raw files might not exist, use default loads
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }

    // Set grid configuration and icons
    fun configureLevel(
        cols: Int,
        rws: Int,
        scoreGoal: Int,
        stepsLimit: Int,
        allowedTypes: List<Int>,
        skips: Set<Pair<Int, Int>>,
        appIcons: Map<Int, Bitmap>
    ) {
        columns = cols
        rows = rws
        targetScore = scoreGoal
        maxSteps = stepsLimit
        skippedCells.clear()
        skippedCells.addAll(skips)

        grid = Array(columns) { Array<Block?>(rows) { null } }

        // Map icons
        blockBitmaps.clear()
        for (type in allowedTypes) {
            appIcons[type]?.let {
                blockBitmaps[type] = Bitmap.createScaledBitmap(it, 128, 128, true)
            }
        }

        // Trigger size calculation and grid generation on next layout pass
        post {
            calculateDimensions()
            generateGrid(allowedTypes)
            invalidate()
        }
    }

    private fun calculateDimensions() {
        if (width == 0 || height == 0) return
        cellSize = width / columns
        blockDrawSize = (cellSize * 0.9f).toInt()
        startX = (width - columns * cellSize) / 2f
        startY = (height - rows * cellSize) / 2f
    }

    private fun generateGrid(allowedTypes: List<Int>) {
        if (allowedTypes.isEmpty()) return
        for (c in 0 until columns) {
            for (r in 0 until rows) {
                if (skippedCells.contains(Pair(c, r))) {
                    grid[c][r] = null
                    continue
                }
                val type = allowedTypes[random.nextInt(allowedTypes.size)]
                val px = startX + c * cellSize
                val py = startY + r * cellSize
                grid[c][r] = Block(
                    type = type,
                    bitmap = blockBitmaps[type],
                    col = c,
                    row = r,
                    currentX = px,
                    currentY = py - height, // Fall from top
                    targetX = px,
                    targetY = py,
                    isFalling = true
                )
            }
        }
        animateFallingBlocks()
    }

    private fun animateFallingBlocks() {
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            interpolator = LinearInterpolator()
            addUpdateListener {
                val fraction = it.animatedValue as Float
                for (c in 0 until columns) {
                    for (r in 0 until rows) {
                        val block = grid[c][r] ?: continue
                        if (block.isFalling) {
                            block.currentY = block.targetY - (1f - fraction) * height
                            if (fraction >= 1.0f) {
                                block.currentY = block.targetY
                                block.isFalling = false
                            }
                        }
                    }
                }
                invalidate()
            }
        }
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cellSize == 0) return

        // 1. Draw Grid Blocks
        for (c in 0 until columns) {
            for (r in 0 until rows) {
                val block = grid[c][r] ?: continue
                val px = block.currentX
                val py = block.currentY

                canvas.save()
                canvas.translate(px, py)
                if (block.rotation != 0f) {
                    canvas.rotate(block.rotation, cellSize / 2f, cellSize / 2f)
                }

                val offset = (cellSize - blockDrawSize) / 2
                val dstRect = Rect(offset, offset, offset + blockDrawSize, offset + blockDrawSize)

                val bmp = block.bitmap
                if (bmp != null) {
                    canvas.drawBitmap(bmp, null, dstRect, paint)
                }
                canvas.restore()
            }
        }

        // 2. Draw Break/Explosion Particles
        val pIterator = particles.iterator()
        while (pIterator.hasNext()) {
            val p = pIterator.next()
            p.life -= 0.05f
            if (p.life <= 0) {
                pIterator.remove()
                continue
            }
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.5f // simple gravity
            p.alpha = (p.life * 255).toInt().coerceIn(0, 255)
            paint.color = p.color
            paint.alpha = p.alpha
            canvas.drawCircle(p.x, p.y, p.size, paint)
        }
        paint.alpha = 255

        // 3. Draw Floating Text Popups
        val tIterator = floatingTexts.iterator()
        while (tIterator.hasNext()) {
            val t = tIterator.next()
            val elapsed = SystemClock.uptimeMillis() - t.startTime
            if (elapsed > 800) {
                tIterator.remove()
                continue
            }
            val progress = elapsed / 800f
            t.y = t.startY - progress * cellSize
            t.alpha = ((1f - progress) * 255).toInt().coerceIn(0, 255)
            textPaint.alpha = t.alpha
            canvas.drawText(t.text, t.x, t.y, textPaint)
        }
        textPaint.alpha = 255

        if (particles.isNotEmpty() || floatingTexts.isNotEmpty()) {
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (cellSize == 0) return false
        val x = event.x
        val y = event.y

        if (event.action == MotionEvent.ACTION_DOWN) {
            val col = ((x - startX) / cellSize).toInt()
            val row = ((y - startY) / cellSize).toInt()

            if (col in 0 until columns && row in 0 until rows) {
                grid[col][row]?.let { block ->
                    handleBlockClick(block)
                }
            }
        }
        return true
    }

    private fun handleBlockClick(block: Block) {
        if (block.isFalling) return

        // 1. Play Sound
        if (block.type in 6..8) {
            soundPool?.play(sndBomb, 1f, 1f, 1, 0, 1f)
        } else {
            soundPool?.play(sndBreak, 1f, 1f, 1, 0, 1f)
        }

        // 2. Perform Block Removal and Cascade Falling
        val removed = ArrayList<Block>()
        if (block.type in 6..8) {
            // Blast Bomb
            performBombBlast(block, removed)
        } else {
            // Matching Group
            val matchGroup = ArrayList<Block>()
            findConnectedBlocks(block.col, block.row, block.type, matchGroup, Array(columns) { BooleanArray(rows) })
            if (matchGroup.size >= 2) {
                removed.addAll(matchGroup)
                for (b in matchGroup) {
                    grid[b.col][b.row] = null
                }
            } else {
                // Not enough matches, just shake/rotate the single block briefly
                shakeBlock(block)
                return
            }
        }

        if (removed.isNotEmpty()) {
            // Notify Score & Targets
            val countByType = removed.groupBy { it.type }.mapValues { it.value.size }
            var pointsEarned = 0
            for ((type, count) in countByType) {
                pointsEarned += count * 100
                gameListener?.onBlockDestroyed(type, count)
            }

            // Spawn floating text
            val centerX = startX + block.col * cellSize + cellSize / 2f
            val centerY = startY + block.row * cellSize + cellSize / 2f
            floatingTexts.add(FloatingText("+$pointsEarned", centerX, centerY, centerY))
            gameListener?.onScoreIncremented(pointsEarned)

            // Spawn Particles
            for (b in removed) {
                spawnParticles(
                    startX + b.col * cellSize + cellSize / 2f,
                    startY + b.row * cellSize + cellSize / 2f
                )
            }

            // Apply grid gravity (make blocks fall down)
            applyGridGravity()
            gameListener?.onMoveMade()
        }
    }

    private fun shakeBlock(block: Block) {
        val shake = ValueAnimator.ofFloat(0f, 15f, -15f, 15f, 0f).apply {
            duration = 300
            addUpdateListener {
                block.rotation = it.animatedValue as Float
                invalidate()
            }
        }
        shake.start()
    }

    private fun findConnectedBlocks(c: Int, r: Int, type: Int, result: ArrayList<Block>, visited: Array<BooleanArray>) {
        if (c !in 0 until columns || r !in 0 until rows) return
        if (visited[c][r] || grid[c][r] == null || grid[c][r]?.type != type || grid[c][r]?.isFalling == true) return

        visited[c][r] = true
        grid[c][r]?.let {
            result.add(it)
            findConnectedBlocks(c + 1, r, type, result, visited)
            findConnectedBlocks(c - 1, r, type, result, visited)
            findConnectedBlocks(c, r + 1, type, result, visited)
            findConnectedBlocks(c, r - 1, type, result, visited)
        }
    }

    private fun performBombBlast(bomb: Block, removed: ArrayList<Block>) {
        grid[bomb.col][bomb.row] = null
        removed.add(bomb)

        when (bomb.type) {
            6 -> { // Vertical blast (entire column)
                for (r in 0 until rows) {
                    grid[bomb.col][r]?.let {
                        if (!removed.contains(it)) {
                            removed.add(it)
                            grid[bomb.col][r] = null
                        }
                    }
                }
            }
            7 -> { // Horizontal blast (entire row)
                for (c in 0 until columns) {
                    grid[c][bomb.row]?.let {
                        if (!removed.contains(it)) {
                            removed.add(it)
                            grid[c][bomb.row] = null
                        }
                    }
                }
            }
            8 -> { // Area blast (3x3 surrounding)
                for (dc in -1..1) {
                    for (dr in -1..1) {
                        val nc = bomb.col + dc
                        val nr = bomb.row + dr
                        if (nc in 0 until columns && nr in 0 until rows) {
                            grid[nc][nr]?.let {
                                if (!removed.contains(it)) {
                                    removed.add(it)
                                    grid[nc][nr] = null
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun applyGridGravity() {
        val allowedTypes = blockBitmaps.keys.toList()
        if (allowedTypes.isEmpty()) return

        var maxNewFalls = 0

        for (c in 0 until columns) {
            var emptySlots = 0
            // Fall downwards
            for (r in rows - 1 downTo 0) {
                if (skippedCells.contains(Pair(c, r))) continue
                if (grid[c][r] == null) {
                    emptySlots++
                } else if (emptySlots > 0) {
                    // Shift block down
                    val block = grid[c][r]!!
                    grid[c][r + emptySlots] = block
                    grid[c][r] = null

                    block.row = r + emptySlots
                    block.targetY = startY + block.row * cellSize
                    block.isFalling = true
                }
            }

            // Refill column from top
            for (i in 0 until emptySlots) {
                val r = emptySlots - 1 - i
                if (skippedCells.contains(Pair(c, r))) continue

                val type = allowedTypes[random.nextInt(allowedTypes.size)]
                val px = startX + c * cellSize
                val py = startY + r * cellSize

                val newBlock = Block(
                    type = type,
                    bitmap = blockBitmaps[type],
                    col = c,
                    row = r,
                    currentY = py - height, // Fall from top of screen
                    targetY = py,
                    isFalling = true
                )
                grid[c][r] = newBlock
                maxNewFalls++
            }
        }

        // Trigger falling animation
        animateFallingBlocks()
        checkAndCreateBombs()
    }

    // Spawn vertical/horizontal/area bomb randomly if large match groups are broken
    private fun checkAndCreateBombs() {
        // Randomly spawn vertical/horizontal bombs in 10% chance to make game exciting
        if (random.nextInt(10) == 0) {
            val emptySlots = ArrayList<Pair<Int, Int>>()
            for (c in 0 until columns) {
                for (r in 0 until rows) {
                    if (!skippedCells.contains(Pair(c, r)) && grid[c][r] != null && grid[c][r]?.type !in 6..8) {
                        emptySlots.add(Pair(c, r))
                    }
                }
            }
            if (emptySlots.isNotEmpty()) {
                val (c, r) = emptySlots[random.nextInt(emptySlots.size)]
                val bombType = when (random.nextInt(3)) {
                    0 -> 6 // Vertical
                    1 -> 7 // Horizontal
                    else -> 8 // Around
                }
                val bombBmp = when (bombType) {
                    6 -> bombVerticalBmp
                    7 -> bombHorizontalBmp
                    else -> bombAroundBmp
                }
                grid[c][r]?.let {
                    it.type = bombType
                    it.bitmap = bombBmp
                    it.isFalling = false
                }
            }
        }
    }

    private fun spawnParticles(x: Float, y: Float) {
        val colors = intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN)
        val count = 10
        for (i in 0 until count) {
            val angle = random.nextFloat() * 2 * Math.PI
            val speed = random.nextFloat() * 10f + 5f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    color = colors[random.nextInt(colors.size)],
                    size = random.nextFloat() * 8f + 4f
                )
            )
        }
    }
}
