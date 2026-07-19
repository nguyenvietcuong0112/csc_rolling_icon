package com.rolling.spinning.icon3d.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rolling.spinning.icon3d.R
import com.rolling.spinning.icon3d.data.PreferenceRepository
import com.rolling.spinning.icon3d.wallpaper.RollingWallpaperService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WallpaperPickerActivity : BaseActivity() {

    private lateinit var preferenceRepository: PreferenceRepository
    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var recyclerView: RecyclerView
    private var wallpaperMode = "rolling" // "rolling" or "spinning"

    // Danh sách 9 hình nền trong dự án
    private val wallpapers = listOf(
        R.drawable.bg_wallpaper_00,
        R.drawable.bg_wallpaper_01,
        R.drawable.bg_wallpaper_02,
        R.drawable.bg_wallpaper_03,
        R.drawable.bg_wallpaper_04,
        R.drawable.bg_wallpaper_05,
        R.drawable.bg_wallpaper_06,
        R.drawable.bg_wallpaper_07,
        R.drawable.bg_wallpaper_08
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpaper_picker)

        preferenceRepository = PreferenceRepository(this)
        wallpaperMode = intent.getStringExtra("mode") ?: "rolling"

        recyclerView = findViewById(R.id.wallpaperRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = WallpaperAdapter()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private inner class WallpaperAdapter : RecyclerView.Adapter<WallpaperAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgView: ImageView = view.findViewById(R.id.imgWallpaper)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wallpaper_picker, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val resId = wallpapers[position]
            holder.imgView.setImageResource(resId)

            holder.itemView.setOnClickListener {
                val uriString = "android.resource://$packageName/$resId"

                scope.launch {
                    if (wallpaperMode == "spinning") {
                        preferenceRepository.setSpinningBgImagePath(uriString)
                        preferenceRepository.setWallpaperMode("spinning")
                    } else if (wallpaperMode == "shape_path") {
                        preferenceRepository.setShapeBgImagePath(uriString)
                        preferenceRepository.setWallpaperMode("shape_path")
                    } else {
                        preferenceRepository.setBgImagePath(uriString)
                        preferenceRepository.setBgType(2) // Bắt buộc dùng hình ảnh
                        preferenceRepository.setWallpaperMode("rolling")
                    }

                    val fromSettings = intent.getBooleanExtra("from_settings", false)
                    if (fromSettings) {
                        Toast.makeText(this@WallpaperPickerActivity, getString(R.string.toast_bg_updated), Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        // Khởi chạy màn hình thiết lập tùy chọn (CustomizeActivity) thay vì mở trình cài đặt hệ thống ngay
                        val intent = Intent(this@WallpaperPickerActivity, CustomizeActivity::class.java).apply {
                            putExtra("mode", wallpaperMode)
                        }
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }

        override fun getItemCount(): Int = wallpapers.size
    }
}
