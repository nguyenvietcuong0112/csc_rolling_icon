package com.iconchanger.wallpaper.rolling.icons.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.iconchanger.wallpaper.rolling.icons.R

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Nút Settings góc trên bên phải trên header
        findViewById<ImageView>(R.id.btnHeaderSettings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // 2. Click vào thẻ Rolling Icon hoặc nút Get Started của Rolling Icon
        val rollingClickAction = {
            val intent = Intent(this, RollingSelectionActivity::class.java)
            startActivity(intent)
        }
        findViewById<View>(R.id.cardRollingIcon).setOnClickListener { rollingClickAction() }
        findViewById<TextView>(R.id.btnGetStartedRolling).setOnClickListener { rollingClickAction() }

        // 3. Click vào thẻ Spinning Icon hoặc nút Get Started của Spinning Icon
        val spinningClickAction = {
            val intent = Intent(this, SpinningIconActivity::class.java)
            startActivity(intent)
        }
        findViewById<View>(R.id.cardSpinningIcon).setOnClickListener { spinningClickAction() }
        findViewById<TextView>(R.id.btnGetStartedSpinning).setOnClickListener { spinningClickAction() }

        // 4. Click vào thẻ Shape Path Icon
        findViewById<View>(R.id.cardShapePathIcon).setOnClickListener {
            val intent = Intent(this, ShapeSelectorActivity::class.java)
            startActivity(intent)
        }

        // 5. Click vào thẻ Emoji Icon
        findViewById<View>(R.id.cardEmojiIcon).setOnClickListener {
            val intent = Intent(this, RollingSelectionActivity::class.java).apply {
                putExtra("default_tab", 1)
                putExtra("single_mode", true)
            }
            startActivity(intent)
        }

        // 6. Click vào thẻ Photo Icon
        findViewById<View>(R.id.cardPhotoIcon).setOnClickListener {
            val intent = Intent(this, RollingSelectionActivity::class.java).apply {
                putExtra("default_tab", 2)
                putExtra("single_mode", true)
            }
            startActivity(intent)
        }

        // 7. Click vào thẻ Wallpaper -> Mở ApiWallpaperActivity (Online API Mode)
        findViewById<View>(R.id.cardWallpaper).setOnClickListener {
            val intent = Intent(this, ApiWallpaperActivity::class.java)
            startActivity(intent)
        }
    }
}

