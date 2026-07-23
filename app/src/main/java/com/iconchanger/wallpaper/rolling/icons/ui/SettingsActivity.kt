package com.iconchanger.wallpaper.rolling.icons.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.iconchanger.wallpaper.rolling.icons.BuildConfig
import androidx.lifecycle.lifecycleScope
import com.iconchanger.wallpaper.rolling.icons.R
import com.iconchanger.wallpaper.rolling.icons.data.PreferenceRepository
import kotlinx.coroutines.launch

class SettingsActivity : BaseActivity() {

    private lateinit var preferenceRepository: PreferenceRepository

    private lateinit var btnBack: ImageView
    private lateinit var btnSetDefaultLauncher: View
    private lateinit var switchMakeLauncher: SwitchCompat
    private lateinit var btnSizeSmall: TextView
    private lateinit var btnSizeMiddle: TextView
    private lateinit var btnSizeLarge: TextView
    private lateinit var switchFloatButton: SwitchCompat
    private lateinit var switchWallpaperTouch: SwitchCompat
    private lateinit var tvSettingsVersionValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        preferenceRepository = PreferenceRepository(this)

        btnBack = findViewById(R.id.btnBack)
        btnSetDefaultLauncher = findViewById(R.id.btnSetDefaultLauncher)
        switchMakeLauncher = findViewById(R.id.switchMakeLauncher)
        btnSizeSmall = findViewById(R.id.btnSizeSmall)
        btnSizeMiddle = findViewById(R.id.btnSizeMiddle)
        btnSizeLarge = findViewById(R.id.btnSizeLarge)
        switchFloatButton = findViewById(R.id.switchFloatButton)
        switchWallpaperTouch = findViewById(R.id.switchWallpaperTouch)
        tvSettingsVersionValue = findViewById(R.id.tv_settings_version_value)

        btnBack.setOnClickListener { finish() }

        // 1. Make it as launcher action
        val openLauncherSettingsAction = {
            Toast.makeText(this, getString(R.string.toast_select_launcher), Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
        }

        tvSettingsVersionValue.text = "v${BuildConfig.VERSION_NAME}"

        btnSetDefaultLauncher.setOnClickListener { openLauncherSettingsAction() }
        switchMakeLauncher.setOnClickListener { openLauncherSettingsAction() }

        // 2. Icon size Segmented control
        btnSizeSmall.setOnClickListener { updateIconSize(0.6f) }
        btnSizeMiddle.setOnClickListener { updateIconSize(1.0f) }
        btnSizeLarge.setOnClickListener { updateIconSize(1.4f) }

        // 3. Enable float button
        switchFloatButton.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                preferenceRepository.setFloatButtonEnabled(isChecked)
            }
        }

        // 4. Enable wallpaper touch
        switchWallpaperTouch.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                preferenceRepository.setWallpaperTouchEnabled(isChecked)
            }
        }

        // Support Information Actions
        findViewById<View>(R.id.btnLanguage)?.setOnClickListener {
            Toast.makeText(this, "Language selection coming soon", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btnPrivacyPolicy)?.setOnClickListener {
            openWebPage("https://google.com")
        }

        findViewById<View>(R.id.btnAboutUs)?.setOnClickListener {
            Toast.makeText(this, getString(R.string.toast_about_app), Toast.LENGTH_LONG).show()
        }

        loadSettingsFromPrefs()
    }

    override fun onResume() {
        super.onResume()
        switchMakeLauncher.isChecked = isDefaultLauncher()
    }

    private fun loadSettingsFromPrefs() {
        lifecycleScope.launch {
            // Load Icon size state
            val size = preferenceRepository.getIconSize()
            updateSizeButtonsUI(size)

            // Load Launcher state
            switchMakeLauncher.isChecked = isDefaultLauncher()

            // Load Float Button
            switchFloatButton.isChecked = preferenceRepository.isFloatButtonEnabled()

            // Load Wallpaper Touch
            switchWallpaperTouch.isChecked = preferenceRepository.isWallpaperTouchEnabled()
        }
    }

    private fun updateIconSize(size: Float) {
        updateSizeButtonsUI(size)
        lifecycleScope.launch {
            preferenceRepository.setIconSize(size)
        }
    }

    private fun updateSizeButtonsUI(size: Float) {
        val isSmall = size < 0.8f
        val isMiddle = size in 0.8f..1.2f

        if (isSmall) {
            setSelectedSegmentButton(btnSizeSmall)
            setUnselectedSegmentButton(btnSizeMiddle)
            setUnselectedSegmentButton(btnSizeLarge)
        } else if (isMiddle) {
            setUnselectedSegmentButton(btnSizeSmall)
            setSelectedSegmentButton(btnSizeMiddle)
            setUnselectedSegmentButton(btnSizeLarge)
        } else {
            setUnselectedSegmentButton(btnSizeSmall)
            setUnselectedSegmentButton(btnSizeMiddle)
            setSelectedSegmentButton(btnSizeLarge)
        }
    }

    private fun setSelectedSegmentButton(textView: TextView) {
        textView.background = ContextCompat.getDrawable(this, R.drawable.bg_purple_gradient_btn)
        textView.setTextColor(Color.WHITE)
    }

    private fun setUnselectedSegmentButton(textView: TextView) {
        textView.background = null
        textView.setTextColor(Color.parseColor("#1A1A1A"))
    }

    private fun isDefaultLauncher(): Boolean {
        val localIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = packageManager.resolveActivity(localIntent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    private fun openWebPage(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_no_browser), Toast.LENGTH_SHORT).show()
        }
    }
}
