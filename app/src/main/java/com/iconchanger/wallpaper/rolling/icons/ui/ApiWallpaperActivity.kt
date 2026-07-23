package com.iconchanger.wallpaper.rolling.icons.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iconchanger.wallpaper.rolling.icons.R
import com.iconchanger.wallpaper.rolling.icons.adapter.ApiWallpaperAdapter
import com.iconchanger.wallpaper.rolling.icons.adapter.CategoryAdapter
import com.iconchanger.wallpaper.rolling.icons.data.PreferenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale

data class ApiWallpaperItem(
    val id: String,
    val category: String,
    val name: String,
    val originalImageUrl: String,
    val thumbnailImageUrl: String,
    val views: Long
)

class ApiWallpaperActivity : BaseActivity() {

    private lateinit var preferenceRepository: PreferenceRepository
    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var apiWallpaperRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    private val allWallpapers = ArrayList<ApiWallpaperItem>()
    private val displayedWallpapers = ArrayList<ApiWallpaperItem>()
    private val categoryList = ArrayList<String>()

    private var wallpaperAdapter: ApiWallpaperAdapter? = null
    private var categoryAdapter: CategoryAdapter? = null

    private var selectedCategory = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_wallpaper)

        preferenceRepository = PreferenceRepository(this)

        categoryRecyclerView = findViewById(R.id.categoryRecyclerView)
        apiWallpaperRecyclerView = findViewById(R.id.apiWallpaperRecyclerView)
        progressBar = findViewById(R.id.progressBar)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // 1. Setup Category horizontal layout
        categoryRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        categoryAdapter = CategoryAdapter(categoryList) { category ->
            selectedCategory = category
            filterWallpapersByCategory()
        }
        categoryRecyclerView.adapter = categoryAdapter

        // 2. Setup Wallpaper grid layout
        apiWallpaperRecyclerView.layoutManager = GridLayoutManager(this, 3)
        wallpaperAdapter = ApiWallpaperAdapter(displayedWallpapers) { item ->
            // Immediate navigation upon clicking any item
            scope.launch {
                withContext(Dispatchers.IO) {
                    preferenceRepository.setBgImagePath(item.originalImageUrl)
                    preferenceRepository.setBgType(2)
                }
                val intent = Intent(this@ApiWallpaperActivity, CustomizeActivity::class.java).apply {
                    putExtra("mode", "wallpaper")
                }
                startActivity(intent)
                finish()
            }
        }
        apiWallpaperRecyclerView.adapter = wallpaperAdapter

        // Fetch wallpapers from API
        fetchWallpapersFromApi()
    }

    private fun fetchWallpapersFromApi() {
        progressBar.visibility = View.VISIBLE
        scope.launch(Dispatchers.IO) {
            val fetchedItems = ArrayList<ApiWallpaperItem>()
            try {
                val apiUrl = "https://api.1teps.com/wallapi/images?image_type=BG&category=all&pageNumber=200"
                val jsonText = URL(apiUrl).readText()
                val jsonObject = JSONObject(jsonText)
                val imagesArray = jsonObject.optJSONArray("images")

                if (imagesArray != null) {
                    for (i in 0 until imagesArray.length()) {
                        val obj = imagesArray.getJSONObject(i)
                        fetchedItems.add(
                            ApiWallpaperItem(
                                id = obj.optString("id"),
                                category = obj.optString("category"),
                                name = obj.optString("name"),
                                originalImageUrl = obj.optString("original_image_url"),
                                thumbnailImageUrl = obj.optString("thumbnail_image_url"),
                                views = obj.optLong("views")
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Extract unique normalized categories
            val extractedCategories = LinkedHashSet<String>()
            fetchedItems.forEach { item ->
                if (item.category.isNotEmpty()) {
                    extractedCategories.add(normalizeCategory(item.category))
                }
            }

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE

                allWallpapers.clear()
                allWallpapers.addAll(fetchedItems)

                categoryList.clear()
                categoryList.addAll(extractedCategories)
                if (categoryList.isNotEmpty()) {
                    selectedCategory = categoryList[0]
                }
                categoryAdapter?.notifyDataSetChanged()

                filterWallpapersByCategory()
            }
        }
    }

    private fun normalizeCategory(cat: String): String {
        val clean = cat.trim().replaceFirstChar { it.uppercase(Locale.ROOT) }
        return when (clean.lowercase(Locale.ROOT)) {
            "sport", "sports" -> "Sports"
            else -> clean
        }
    }

    private fun filterWallpapersByCategory() {
        displayedWallpapers.clear()
        displayedWallpapers.addAll(
            allWallpapers.filter { normalizeCategory(it.category).equals(selectedCategory, ignoreCase = true) }
        )
        wallpaperAdapter?.notifyDataSetChanged()
    }
}
