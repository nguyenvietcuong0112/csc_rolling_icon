package com.mandg.funny.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Xml
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.mandg.funny.R
import com.mandg.funny.data.AppRepository
import com.mandg.funny.data.GameDatabaseHelper
import com.mandg.funny.data.IconLoader
import com.mandg.funny.game.widget.BlocksLayout
import com.mandg.funny.game.widget.ScoreProgressBar
import com.mandg.funny.game.widget.ScoreStarsView
import com.mandg.funny.game.widget.TargetLayout
import com.mandg.funny.render.GameRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

class GamePlayActivity : AndroidApplication(), BlocksLayout.GameListener {

    val selectedGameApps = ArrayList<com.mandg.funny.model.AppInfo>()

    private lateinit var blocksLayout: BlocksLayout
    private lateinit var scoreProgressBar: ScoreProgressBar
    private lateinit var scoreStarsView: ScoreStarsView
    private lateinit var targetLayout: TargetLayout
    private lateinit var txtScore: TextView
    private lateinit var txtSteps: TextView
    private lateinit var txtLevelTitle: TextView

    // Overlay Views
    private lateinit var layoutGameOverlay: FrameLayout
    private lateinit var txtOverlayTitle: TextView
    private lateinit var txtOverlayScore: TextView
    private lateinit var layoutWinStars: LinearLayout
    private lateinit var layoutCoinReward: LinearLayout
    private lateinit var btnOverlayPrimary: Button
    private lateinit var btnOverlaySecondary: Button

    private lateinit var dbHelper: GameDatabaseHelper
    private val coroutineScope = MainScope()

    private var currentLevelNum = 1
    private var levelScoreGoal = 1000
    private var levelStepsLimit = 20
    private var currentScore = 0
    private var currentSteps = 20

    private val levelTargets = ArrayList<TargetLayout.Target>()
    private var isGameFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dbHelper = GameDatabaseHelper(this)
        currentLevelNum = intent.getIntExtra("level_num", 1)

        // 1. Initialize libGDX Box2D view for rolling background icons
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            useGL30 = false
        }
        val gdxView = initializeForView(GameRenderer(this), config)

        setContentView(R.layout.activity_game_play)

        // Add gdxView to container
        findViewById<FrameLayout>(R.id.gdxContainer).addView(gdxView)

        // 2. Bind layout Views
        blocksLayout = findViewById(R.id.blocksLayout)
        scoreProgressBar = findViewById(R.id.scoreProgressBar)
        scoreStarsView = findViewById(R.id.scoreStarsView)
        targetLayout = findViewById(R.id.targetLayout)
        txtScore = findViewById(R.id.txtScore)
        txtSteps = findViewById(R.id.txtSteps)
        txtLevelTitle = findViewById(R.id.txtLevelTitle)

        // Overlay popups
        layoutGameOverlay = findViewById(R.id.layoutGameOverlay)
        txtOverlayTitle = findViewById(R.id.txtOverlayTitle)
        txtOverlayScore = findViewById(R.id.txtOverlayScore)
        layoutWinStars = findViewById(R.id.layoutWinStars)
        layoutCoinReward = findViewById(R.id.layoutCoinReward)
        btnOverlayPrimary = findViewById(R.id.btnOverlayPrimary)
        btnOverlaySecondary = findViewById(R.id.btnOverlaySecondary)

        txtLevelTitle.text = "MÀN $currentLevelNum"

        findViewById<ImageView>(R.id.btnPauseGame).setOnClickListener {
            finish()
        }

        blocksLayout.setGameListener(this)

        loadLevelConfigAndStart()
    }

    private fun loadLevelConfigAndStart() {
        coroutineScope.launch {
            // 1. Parse level configurations from assets XML
            val config = parseLevelConfig(currentLevelNum)
            if (config == null) {
                Toast.makeText(this@GamePlayActivity, "Không tìm thấy dữ liệu màn chơi!", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            levelScoreGoal = config.scoreGoal
            levelStepsLimit = config.stepsLimit
            currentScore = 0
            currentSteps = levelStepsLimit
            isGameFinished = false

            // Update UI initial stats
            txtScore.text = "0"
            txtSteps.text = currentSteps.toString()
            scoreProgressBar.setMaxValue(levelScoreGoal)
            scoreProgressBar.setProgress(0)
            scoreStarsView.setOnStarNum(0)

            // 2. Load the user's selected app icons
            val appRepo = AppRepository(this@GamePlayActivity)
            val loader = IconLoader(this@GamePlayActivity)
            val selectedApps = appRepo.getSelectedApps()

            selectedGameApps.clear()
            for (i in 0 until 5) {
                if (i < selectedApps.size) {
                    selectedGameApps.add(selectedApps[i])
                }
            }

            val appIcons = HashMap<Int, Bitmap>()
            // Default built-in icons in case selected app icons list is empty
            val defaultResIds = intArrayOf(
                android.R.drawable.sym_def_app_icon,
                android.R.drawable.ic_dialog_info,
                android.R.drawable.ic_dialog_email,
                android.R.drawable.ic_dialog_map,
                android.R.drawable.ic_menu_camera
            )

            for (i in 0 until 5) {
                val type = i + 1
                val packageName = if (i < selectedApps.size) selectedApps[i].packageName else ""
                val bmp = loader.loadAppIcon(packageName)
                    ?: BitmapFactory.decodeResource(resources, defaultResIds[i])!!
                appIcons[type] = bmp
            }

            // 3. Map targets list
            levelTargets.clear()
            for (xmlTarget in config.targetTypes) {
                levelTargets.add(
                    TargetLayout.Target(
                        type = xmlTarget.type,
                        count = xmlTarget.count,
                        bitmap = appIcons[xmlTarget.type]
                    )
                )
            }

            // Setup custom views
            targetLayout.setupTargets(levelTargets)
            blocksLayout.configureLevel(
                cols = config.columns,
                rws = config.rows,
                scoreGoal = levelScoreGoal,
                stepsLimit = levelStepsLimit,
                allowedTypes = config.blockTypes,
                skips = config.skips,
                appIcons = appIcons
            )
        }
    }

    override fun onScoreIncremented(score: Int) {
        currentScore += score
        txtScore.text = currentScore.toString()
        scoreProgressBar.setProgress(currentScore)

        // Calculate stars
        val stars = when {
            currentScore >= levelScoreGoal * 1.5 -> 3
            currentScore >= levelScoreGoal * 1.2 -> 2
            currentScore >= levelScoreGoal -> 1
            else -> 0
        }
        scoreStarsView.setOnStarNum(stars)

        checkGameStatus()
    }

    override fun onBlockDestroyed(type: Int, count: Int) {
        targetLayout.updateTargetCount(type, count)
        checkGameStatus()
    }

    override fun onMoveMade() {
        currentSteps--
        txtSteps.text = currentSteps.toString()
        checkGameStatus()
    }

    override fun onGameFinished(isWon: Boolean) {
        // Not used, handled in checkGameStatus()
    }

    private fun checkGameStatus() {
        if (isGameFinished) return

        val allTargetsReached = targetLayout.areAllTargetsReached()
        val scoreReached = currentScore >= levelScoreGoal

        if (allTargetsReached && scoreReached) {
            // Level Won!
            isGameFinished = true
            showWinOverlay()
        } else if (currentSteps <= 0) {
            // Level Lost (Out of moves)
            isGameFinished = true
            showLoseOverlay()
        }
    }

    private fun showWinOverlay() {
        val stars = when {
            currentScore >= levelScoreGoal * 1.5 -> 3
            currentScore >= levelScoreGoal * 1.2 -> 2
            else -> 1
        }

        // Save progress to database
        dbHelper.saveLevelProgress(currentLevelNum, currentScore, stars)

        // Reward coins
        val sp = getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        val currentCoins = sp.getInt("coins", 1000)
        sp.edit().putInt("coins", currentCoins + 50).apply()

        // Configure Win Overlay UI
        txtOverlayTitle.text = "THẮNG CUỘC!"
        txtOverlayTitle.setBackgroundResource(R.drawable.game_panel_title_bg)
        txtOverlayScore.text = "Điểm đạt được: $currentScore"
        layoutCoinReward.visibility = View.VISIBLE
        layoutWinStars.visibility = View.VISIBLE

        // Configure stars images
        findViewById<ImageView>(R.id.imgStar1).setImageResource(if (stars >= 1) R.drawable.game_star_on else R.drawable.game_star_off)
        findViewById<ImageView>(R.id.imgStar2).setImageResource(if (stars >= 2) R.drawable.game_star_on else R.drawable.game_star_off)
        findViewById<ImageView>(R.id.imgStar3).setImageResource(if (stars >= 3) R.drawable.game_star_on else R.drawable.game_star_off)

        btnOverlayPrimary.text = "MÀN TIẾP THEO"
        btnOverlayPrimary.setOnClickListener {
            // Advance to next level
            currentLevelNum++
            txtLevelTitle.text = "MÀN $currentLevelNum"
            layoutGameOverlay.visibility = View.GONE
            loadLevelConfigAndStart()
        }

        btnOverlaySecondary.setOnClickListener {
            finish()
        }

        layoutGameOverlay.visibility = View.VISIBLE
    }

    private fun showLoseOverlay() {
        // Configure Lose Overlay UI
        txtOverlayTitle.text = "THẤT BẠI!"
        txtOverlayTitle.setBackgroundResource(R.drawable.game_panel_title_bg_gray)
        txtOverlayScore.text = "Bạn đã hết lượt di chuyển!"
        layoutCoinReward.visibility = View.GONE
        layoutWinStars.visibility = View.GONE

        btnOverlayPrimary.text = "CHƠI LẠI"
        btnOverlayPrimary.setOnClickListener {
            layoutGameOverlay.visibility = View.GONE
            loadLevelConfigAndStart()
        }

        btnOverlaySecondary.setOnClickListener {
            finish()
        }

        layoutGameOverlay.visibility = View.VISIBLE
    }

    // Config data classes for parser
    private data class XmlTargetType(val type: Int, val count: Int)
    private data class ParsedLevelConfig(
        val levelNum: Int,
        val scoreGoal: Int,
        val stepsLimit: Int,
        val blockTypes: List<Int>,
        val targetTypes: List<XmlTargetType>,
        val columns: Int,
        val rows: Int,
        val skips: Set<Pair<Int, Int>>
    )

    private fun parseLevelConfig(levelNum: Int): ParsedLevelConfig? {
        var inputStream: InputStream? = null
        try {
            inputStream = assets.open("game/game_level_normal.xml")
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            var eventType = parser.eventType

            var currentParsedNum = 0
            var scoreGoal = 1000
            var stepsLimit = 20
            val blockTypes = ArrayList<Int>()
            val targetTypes = ArrayList<XmlTargetType>()
            var columns = 9
            var rows = 9
            val skips = HashSet<Pair<Int, Int>>()

            var inTargetLevel = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val tagName = parser.name
                    if (tagName == "level") {
                        val num = parser.getAttributeValue(null, "level").toInt()
                        if (num == levelNum) {
                            currentParsedNum = num
                            scoreGoal = parser.getAttributeValue(null, "score").toInt()
                            stepsLimit = parser.getAttributeValue(null, "step").toInt()
                            inTargetLevel = true
                        }
                    } else if (inTargetLevel) {
                        when (tagName) {
                            "blockType" -> {
                                val text = parser.nextText().trim()
                                if (text.isNotEmpty()) {
                                    blockTypes.addAll(text.split(",").map { it.toInt() })
                                }
                            }
                            "targetType" -> {
                                val number = parser.getAttributeValue(null, "number").toInt()
                                val type = parser.getAttributeValue(null, "type").toInt()
                                targetTypes.add(XmlTargetType(type, number))
                            }
                            "rowColumn" -> {
                                columns = parser.getAttributeValue(null, "column").toInt()
                                rows = parser.getAttributeValue(null, "row").toInt()
                            }
                            "skip" -> {
                                val text = parser.nextText().trim()
                                if (text.isNotEmpty()) {
                                    val coords = text.split(",").map { it.toInt() }
                                    for (i in coords.indices step 2) {
                                        if (i + 1 < coords.size) {
                                            skips.add(Pair(coords[i], coords[i + 1]))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.name == "level" && inTargetLevel) {
                        return ParsedLevelConfig(
                            levelNum = currentParsedNum,
                            scoreGoal = scoreGoal,
                            stepsLimit = stepsLimit,
                            blockTypes = blockTypes,
                            targetTypes = targetTypes,
                            columns = columns,
                            rows = rows,
                            skips = skips
                        )
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
        }
        return null
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        blocksLayout.release()
        super.onDestroy()
    }
}
