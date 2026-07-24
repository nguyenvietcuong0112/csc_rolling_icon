package com.iconchanger.wallpaper.rolling.icons.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iconchanger.wallpaper.rolling.icons.R
import com.iconchanger.wallpaper.rolling.icons.adapter.CategoryAdapter
import com.iconchanger.wallpaper.rolling.icons.adapter.CategoryItem
import com.iconchanger.wallpaper.rolling.icons.model.ApiWallpaperItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale

class ApiWallpaperActivity : BaseActivity() {

    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var progressBar: com.airbnb.lottie.LottieAnimationView
    private lateinit var layoutErrorOrEmpty: LinearLayout
    private lateinit var txtErrorMessage: TextView
    private lateinit var btnRetry: Button

    private val categoryList = ArrayList<CategoryItem>()
    private var categoryAdapter: CategoryAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_wallpaper)

        categoryRecyclerView = findViewById(R.id.categoryRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        layoutErrorOrEmpty = findViewById(R.id.layoutErrorOrEmpty)
        txtErrorMessage = findViewById(R.id.txtErrorMessage)
        btnRetry = findViewById(R.id.btnRetry)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnRetry.setOnClickListener {
            fetchCategoriesFromApi()
        }

        // Setup 2-Column Category Grid Layout with span lookup for Native Ad
        val gridLayoutManager = GridLayoutManager(this, 2)
        categoryAdapter = CategoryAdapter(categoryList) { categoryItem ->
            // Launch separate Detail Activity
            val intent = Intent(this@ApiWallpaperActivity, ApiWallpaperDetailActivity::class.java).apply {
                putExtra("category_name", categoryItem.name)
            }
            startActivity(intent)
        }

        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (categoryAdapter?.getItemViewType(position) == CategoryAdapter.VIEW_TYPE_AD) 2 else 1
            }
        }
        categoryRecyclerView.layoutManager = gridLayoutManager
        categoryRecyclerView.adapter = categoryAdapter

        // Fetch Categories from API
        fetchCategoriesFromApi()
    }

    private fun fetchCategoriesFromApi() {
        progressBar.visibility = View.VISIBLE
        progressBar.playAnimation()
        layoutErrorOrEmpty.visibility = View.GONE
        categoryRecyclerView.visibility = View.VISIBLE

        scope.launch(Dispatchers.IO) {
            val fetchedItems = ArrayList<ApiWallpaperItem>()
            var fetchError = false

            try {
                val apiUrl = "https://api.1teps.com/wallapi/images?image_type=BG&category=all&pageNumber=1675"
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
                fetchError = true
            }

            // Group items by normalized category and pick the 4th item (index 3) as cover image
            val categoryGroupMap = LinkedHashMap<String, ArrayList<ApiWallpaperItem>>()
            fetchedItems.forEach { item ->
                if (item.category.isNotEmpty()) {
                    val normalized = normalizeCategory(item.category)
                    val list = categoryGroupMap.getOrPut(normalized) { ArrayList() }
                    list.add(item)
                }
            }

            val extractedCategoryItems = categoryGroupMap.map { (name, items) ->
                val coverIndex = if (items.size >= 4) 3 else items.size - 1
                val coverUrl = items[coverIndex].thumbnailImageUrl
                CategoryItem(name, coverUrl)
            }

            withContext(Dispatchers.Main) {
                progressBar.cancelAnimation()
                progressBar.visibility = View.GONE

                if (fetchError || extractedCategoryItems.isEmpty()) {
                    categoryRecyclerView.visibility = View.GONE
                    layoutErrorOrEmpty.visibility = View.VISIBLE
                    txtErrorMessage.text = if (fetchError) {
                        "No internet connection. Please check your network and try again."
                    } else {
                        "No categories available at the moment."
                    }
                } else {
                    layoutErrorOrEmpty.visibility = View.GONE
                    categoryRecyclerView.visibility = View.VISIBLE

                    categoryList.clear()
                    categoryList.addAll(extractedCategoryItems)
                    categoryAdapter?.notifyDataSetChanged()
                }
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
}
