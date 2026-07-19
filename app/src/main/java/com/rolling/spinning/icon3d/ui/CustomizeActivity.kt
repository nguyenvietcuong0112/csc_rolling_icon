package com.rolling.spinning.icon3d.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.rolling.spinning.icon3d.R
import com.rolling.spinning.icon3d.data.PreferenceRepository
import com.rolling.spinning.icon3d.wallpaper.RollingWallpaperService
import kotlinx.coroutines.launch

class CustomizeActivity : BaseActivity() {

    private lateinit var preferenceRepository: PreferenceRepository

    private lateinit var btnBack: ImageView
    private lateinit var cardLiveWallpaper: MaterialCardView
    private lateinit var cardHomeLauncher: MaterialCardView

    private lateinit var btnSizeSmall: TextView
    private lateinit var btnSizeMiddle: TextView
    private lateinit var btnSizeLarge: TextView
    private lateinit var switchWallpaperTouch: SwitchCompat
    private lateinit var switchFloatButton: SwitchCompat
    private lateinit var btnPreviewApply: View

    private var selectedMode = "wallpaper" // "wallpaper" or "launcher"
    private var selectedSize = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customize)

        preferenceRepository = PreferenceRepository(this)

        // Binding views
        btnBack = findViewById(R.id.btnBack)
        cardLiveWallpaper = findViewById(R.id.cardLiveWallpaper)
        cardHomeLauncher = findViewById(R.id.cardHomeLauncher)

        btnSizeSmall = findViewById(R.id.btnSizeSmall)
        btnSizeMiddle = findViewById(R.id.btnSizeMiddle)
        btnSizeLarge = findViewById(R.id.btnSizeLarge)
        switchWallpaperTouch = findViewById(R.id.switchWallpaperTouch)
        switchFloatButton = findViewById(R.id.switchFloatButton)
        btnPreviewApply = findViewById(R.id.btnPreviewApply)

        // Back button click listener
        btnBack.setOnClickListener { finish() }

        // Setup Mode Selector Cards Clicks
        cardLiveWallpaper.setOnClickListener {
            selectedMode = "wallpaper"
            updateModeSelectionUI()
        }
        cardHomeLauncher.setOnClickListener {
            selectedMode = "launcher"
            updateModeSelectionUI()
        }

        // Setup Icon Size Clicks
        btnSizeSmall.setOnClickListener { updateIconSize(0.7f) }
        btnSizeMiddle.setOnClickListener { updateIconSize(1.0f) }
        btnSizeLarge.setOnClickListener { updateIconSize(1.3f) }

        // Load config from preferences
        loadConfigFromPrefs()

        // Preview & Apply Action Button
        btnPreviewApply.setOnClickListener {
            saveConfigAndNavigate()
        }
    }

    private fun loadConfigFromPrefs() {
        lifecycleScope.launch {
            // 1. Icon Size selection
            selectedSize = preferenceRepository.getIconSize()
            updateSizeButtonsUI(selectedSize)

            // 2. Switches state
            switchWallpaperTouch.isChecked = preferenceRepository.isWallpaperTouchEnabled()
            switchFloatButton.isChecked = preferenceRepository.isFloatButtonEnabled()

            // 3. Default to wallpaper mode selection UI
            selectedMode = "wallpaper"
            updateModeSelectionUI()
        }
    }

    private fun updateModeSelectionUI() {
        val terracottaColor = 0xFFC85C32.toInt()
        val darkBrownColor = 0xFF2E1C0C.toInt()

        if (selectedMode == "wallpaper") {
            cardLiveWallpaper.strokeColor = terracottaColor
            cardLiveWallpaper.strokeWidth = dpToPx(3.0f)

            cardHomeLauncher.strokeColor = darkBrownColor
            cardHomeLauncher.strokeWidth = dpToPx(1.5f)
        } else {
            cardLiveWallpaper.strokeColor = darkBrownColor
            cardLiveWallpaper.strokeWidth = dpToPx(1.5f)

            cardHomeLauncher.strokeColor = terracottaColor
            cardHomeLauncher.strokeWidth = dpToPx(3.0f)
        }
    }

    private fun updateIconSize(size: Float) {
        selectedSize = size
        updateSizeButtonsUI(size)
    }

    private fun updateSizeButtonsUI(size: Float) {
        val activeBg = ContextCompat.getDrawable(this, R.drawable.bg_vintage_button_secondary)
        val inactiveBg = null
        btnSizeSmall.background = if (size < 0.8f) activeBg else inactiveBg
        btnSizeMiddle.background = if (size in 0.8f..1.2f) activeBg else inactiveBg
        btnSizeLarge.background = if (size > 1.2f) activeBg else inactiveBg
    }

    private fun saveConfigAndNavigate() {
        lifecycleScope.launch {
            // Save settings parameters
            preferenceRepository.setIconSize(selectedSize)
            preferenceRepository.setWallpaperTouchEnabled(switchWallpaperTouch.isChecked)
            preferenceRepository.setFloatButtonEnabled(switchFloatButton.isChecked)

            if (selectedMode == "wallpaper") {
                // Navigate to system Live Wallpaper chooser preview
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(this@CustomizeActivity, RollingWallpaperService::class.java)
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    startActivity(intent)
                    Toast.makeText(this@CustomizeActivity, getString(R.string.toast_apply_wallpaper_tip), Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    val chooserIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        startActivity(chooserIntent)
                    } catch (ex: Exception) {
                        Toast.makeText(this@CustomizeActivity, getString(R.string.toast_unsupported_wallpaper), Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Navigate to interactive LauncherActivity preview screen
                val intent = Intent(this@CustomizeActivity, LauncherActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
            finish()
        }
    }

    private fun dpToPx(dp: Float): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
