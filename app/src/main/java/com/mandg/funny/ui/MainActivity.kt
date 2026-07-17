package com.mandg.funny.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mandg.funny.R

class MainActivity : AppCompatActivity() {

    private lateinit var btnWallpaperStop: ImageView
    private lateinit var btnLauncherStop: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnWallpaperStop = findViewById(R.id.main_window_wallpaper_stop)
        btnLauncherStop = findViewById(R.id.main_window_launcher_stop)

        // 1. Chọn ứng dụng (App Picker)
        findViewById<FrameLayout>(R.id.btnAppPicker).setOnClickListener {
            val intent = Intent(this, AppPickerActivity::class.java)
            startActivity(intent)
        }

        // 2. Chọn ảnh cá nhân (Photo Picker) -> Mở PhotoIconPickerActivity trực tiếp
        findViewById<FrameLayout>(R.id.btnPhotoPicker).setOnClickListener {
            val intent = Intent(this, PhotoIconPickerActivity::class.java)
            startActivity(intent)
        }

        // 3. Chọn Emoji -> Mở EmojiPickerActivity
        findViewById<FrameLayout>(R.id.btnEmojiPicker).setOnClickListener {
            val intent = Intent(this, EmojiPickerActivity::class.java)
            startActivity(intent)
        }

        // 4. Hình nền -> Kích hoạt Wallpaper
        findViewById<FrameLayout>(R.id.btnMainWallpaper).setOnClickListener {
            launchWallpaperPicker()
        }

        // Nút dừng hình nền động
        btnWallpaperStop.setOnClickListener {
            Toast.makeText(this, "Hãy chọn một hình nền tĩnh khác để dừng hình nền động!", Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                startActivity(Intent.createChooser(intent, "Chọn hình nền khác"))
            }
        }

        // 5. Trò chơi (Game) -> Kích hoạt màn hình bản đồ GameLevelMapActivity
        findViewById<FrameLayout>(R.id.btnMainGame).setOnClickListener {
            val intent = Intent(this, GameLevelMapActivity::class.java)
            startActivity(intent)
        }

        // 6. Cài đặt thông số vật lý
        findViewById<FrameLayout>(R.id.btnMainSettings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // 7. Bắt đầu Launcher (Màn hình chủ)
        findViewById<TextView>(R.id.btnMainStart).setOnClickListener {
            val intent = Intent(this, LauncherActivity::class.java)
            startActivity(intent)
        }

        // Nút dừng chế độ Launcher mặc định
        btnLauncherStop.setOnClickListener {
            Toast.makeText(this, "Hãy chọn trình khởi chạy hệ thống khác để tắt Launcher này!", Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStopOverlaysVisibility()
    }

    private fun updateStopOverlaysVisibility() {
        // Cập nhật trạng thái hiển thị của nút Dừng hình nền động
        if (isWallpaperActive()) {
            btnWallpaperStop.visibility = View.VISIBLE
        } else {
            btnWallpaperStop.visibility = View.GONE
        }

        // Cập nhật trạng thái hiển thị của nút Dừng Launcher mặc định
        if (isDefaultLauncher()) {
            btnLauncherStop.visibility = View.VISIBLE
        } else {
            btnLauncherStop.visibility = View.GONE
        }
    }

    private fun isWallpaperActive(): Boolean {
        val wpm = WallpaperManager.getInstance(this)
        val info = wpm.wallpaperInfo
        return info != null && info.packageName == packageName
    }

    private fun isDefaultLauncher(): Boolean {
        val localIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = packageManager.resolveActivity(localIntent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    private fun launchWallpaperPicker() {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@MainActivity, "com.mandg.funny.wallpaper.RollingWallpaperService")
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            val chooserIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                startActivity(chooserIntent)
            } catch (ex: Exception) {
                Toast.makeText(this, "Thiết bị không hỗ trợ live wallpaper!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
