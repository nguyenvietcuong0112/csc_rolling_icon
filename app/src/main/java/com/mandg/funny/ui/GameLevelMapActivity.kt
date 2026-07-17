package com.mandg.funny.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Xml
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.mandg.funny.R
import com.mandg.funny.data.GameDatabaseHelper
import com.mandg.funny.game.widget.LevelMapView
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

class GameLevelMapActivity : AppCompatActivity() {

    private lateinit var viewPagerMap: ViewPager2
    private lateinit var txtCoinCount: TextView
    private lateinit var dbHelper: GameDatabaseHelper
    private val mapPages = ArrayList<LevelMapView.MapPage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_level_map)

        dbHelper = GameDatabaseHelper(this)

        viewPagerMap = findViewById(R.id.viewPagerMap)
        txtCoinCount = findViewById(R.id.txtCoinCount)

        findViewById<ImageView>(R.id.btnExitMap).setOnClickListener {
            finish()
        }

        findViewById<ImageView>(R.id.btnCoinAdd).setOnClickListener {
            // Award coins (e.g. ad video reward simulation)
            addCoins(100)
        }

        updateCoinsUI()
        loadLevelMapData()
    }

    override fun onResume() {
        super.onResume()
        updateCoinsUI()
        // Reload level progress status (stars and locked state)
        loadLevelMapData()
    }

    private fun updateCoinsUI() {
        val sp = getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        val coins = sp.getInt("coins", 1000)
        txtCoinCount.text = coins.toString()
    }

    private fun addCoins(amount: Int) {
        val sp = getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        val currentCoins = sp.getInt("coins", 1000)
        sp.edit().putInt("coins", currentCoins + amount).apply()
        updateCoinsUI()
    }

    private fun loadLevelMapData() {
        val allLevels = parseLevelsFromXml()
        if (allLevels.isEmpty()) return

        // 1. Query level progress database
        var lastPassedLevel = 0
        val levelNodes = allLevels.map { levelData ->
            val stars = dbHelper.getLevelStars(levelData.levelNum)
            if (stars > 0) {
                lastPassedLevel = lastPassedLevel.coerceAtLeast(levelData.levelNum)
            }
            LevelMapView.LevelNode(
                levelNum = levelData.levelNum,
                posX = levelData.posX,
                posY = levelData.posY,
                stars = stars,
                status = if (stars > 0) LevelMapView.LevelStatus.Passed else LevelMapView.LevelStatus.Unpassed
            )
        }

        // 2. Set next level to playable (Playing status)
        val playableLevel = lastPassedLevel + 1
        for (node in levelNodes) {
            if (node.levelNum == playableLevel) {
                node.status = LevelMapView.LevelStatus.Playing
            } else if (node.levelNum == 1 && lastPassedLevel == 0) {
                node.status = LevelMapView.LevelStatus.Playing
            }
        }

        // 3. Group by mapId
        val groupedByMap = levelNodes.groupBy { level ->
            allLevels.find { it.levelNum == level.levelNum }?.mapId ?: 1
        }

        mapPages.clear()
        val bgResIds = intArrayOf(
            R.drawable.game_level_bg_00,
            R.drawable.game_level_bg_01,
            R.drawable.game_level_bg_02,
            R.drawable.game_level_bg_03,
            R.drawable.game_level_bg_04,
            R.drawable.game_level_bg_05,
            R.drawable.game_level_bg_06,
            R.drawable.game_level_bg_07,
            R.drawable.game_level_bg_08
        )

        for ((mapId, levels) in groupedByMap) {
            val bgIdx = (mapId - 1) % bgResIds.size
            mapPages.add(
                LevelMapView.MapPage(
                    mapId = mapId,
                    bgResId = bgResIds[bgIdx],
                    levels = levels.sortedBy { it.levelNum }
                )
            )
        }

        // Setup adapter
        viewPagerMap.adapter = MapPageAdapter(mapPages) { levelNum ->
            val intent = Intent(this, GamePlayActivity::class.java).apply {
                putExtra("level_num", levelNum)
            }
            startActivity(intent)
        }

        // Swipe viewPager to the page containing the current playable level
        val currentPlayPage = mapPages.indexOfFirst { page ->
            page.levels.any { it.status == LevelMapView.LevelStatus.Playing }
        }
        if (currentPlayPage != -1) {
            viewPagerMap.setCurrentItem(currentPlayPage, false)
        }
    }

    // Level xml parsing data structure
    private data class XmlLevelData(
        val levelNum: Int,
        val mapId: Int,
        val posX: Float,
        val posY: Float
    )

    private fun parseLevelsFromXml(): List<XmlLevelData> {
        val list = ArrayList<XmlLevelData>()
        var inputStream: InputStream? = null
        try {
            inputStream = assets.open("game/game_level_normal.xml")
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            var eventType = parser.eventType

            var levelNum = 0
            var mapId = 1
            var posX = 0f
            var posY = 0f

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val tagName = parser.name
                    if (tagName == "level") {
                        levelNum = parser.getAttributeValue(null, "level").toInt()
                    } else if (tagName == "map") {
                        mapId = parser.getAttributeValue(null, "id").toInt()
                        posX = parser.getAttributeValue(null, "posX").toFloat()
                        posY = parser.getAttributeValue(null, "posY").toFloat()
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.name == "level") {
                        list.add(XmlLevelData(levelNum, mapId, posX, posY))
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
        }
        return list
    }

    // ViewPager2 Page Adapter
    private inner class MapPageAdapter(
        private val pages: List<LevelMapView.MapPage>,
        private val onLevelSelected: (Int) -> Unit
    ) : RecyclerView.Adapter<MapPageAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val levelMapView: LevelMapView = view.findViewById(R.id.levelMapView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_map_page, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val page = pages[position]
            holder.levelMapView.setupLayout(page)
            holder.levelMapView.setOnLevelClickListener(object : LevelMapView.OnLevelClickListener {
                override fun onLevelClick(levelNum: Int) {
                    onLevelSelected(levelNum)
                }
            })
        }

        override fun getItemCount(): Int = pages.size
    }
}
