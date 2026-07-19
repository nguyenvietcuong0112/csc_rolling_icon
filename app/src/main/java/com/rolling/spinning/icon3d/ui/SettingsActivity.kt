package com.rolling.spinning.icon3d.ui

import android.app.WallpaperManager
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.rolling.spinning.icon3d.R
import com.rolling.spinning.icon3d.data.PreferenceRepository
import kotlinx.coroutines.launch

class SettingsActivity : BaseActivity() {

    private lateinit var preferenceRepository: PreferenceRepository

    private lateinit var btnBack: ImageView
    private lateinit var btnSetDefaultLauncher: View
    private lateinit var btnSizeSmall: TextView
    private lateinit var btnSizeMiddle: TextView
    private lateinit var btnSizeLarge: TextView
    private lateinit var btnSelectBgImage: View
    private lateinit var txtBgSubtitle: TextView
    private lateinit var switchFloatButton: SwitchCompat
    private lateinit var switchWallpaperTouch: SwitchCompat

    // Khởi chạy bộ chọn ảnh cho Background Image
    private val bgImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Bỏ qua nếu không hỗ trợ
            }
            val uriString = it.toString()
            lifecycleScope.launch {
                preferenceRepository.setBgImagePath(uriString)
                preferenceRepository.setBgType(2) // 2: Loại ảnh nền là ảnh tuỳ chỉnh
                txtBgSubtitle.text = getString(R.string.bg_custom_image)
                Toast.makeText(this@SettingsActivity, getString(R.string.toast_bg_updated), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        preferenceRepository = PreferenceRepository(this)

        btnBack = findViewById(R.id.btnBack)
        btnSetDefaultLauncher = findViewById(R.id.btnSetDefaultLauncher)
        btnSizeSmall = findViewById(R.id.btnSizeSmall)
        btnSizeMiddle = findViewById(R.id.btnSizeMiddle)
        btnSizeLarge = findViewById(R.id.btnSizeLarge)
        btnSelectBgImage = findViewById(R.id.btnSelectBgImage)
        txtBgSubtitle = findViewById(R.id.txtBgSubtitle)
        switchFloatButton = findViewById(R.id.switchFloatButton)
        switchWallpaperTouch = findViewById(R.id.switchWallpaperTouch)

        btnBack.setOnClickListener { finish() }

        // 1. Make it as launcher
        btnSetDefaultLauncher.setOnClickListener {
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

        // 2. Icon size Segmented control
        btnSizeSmall.setOnClickListener { updateIconSize(0.7f) }
        btnSizeMiddle.setOnClickListener { updateIconSize(1.0f) }
        btnSizeLarge.setOnClickListener { updateIconSize(1.3f) }

        // 3. Select background image
        btnSelectBgImage.setOnClickListener {
            bgImageLauncher.launch("image/*")
        }

        // 4. Enable float button??
        switchFloatButton.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                preferenceRepository.setFloatButtonEnabled(isChecked)
            }
        }

        // 5. Enable wallpaper touch?
        switchWallpaperTouch.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                preferenceRepository.setWallpaperTouchEnabled(isChecked)
            }
        }

        // Action Buttons
        findViewById<View>(R.id.btnFeedback).setOnClickListener {
            Toast.makeText(this, getString(R.string.toast_feedback_coming), Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btnRateUs).setOnClickListener {
            Toast.makeText(this, getString(R.string.toast_thanks_rating), Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btnAboutUs).setOnClickListener {
            Toast.makeText(this, getString(R.string.toast_about_app), Toast.LENGTH_LONG).show()
        }

        findViewById<View>(R.id.btnPrivacyPolicy).setOnClickListener {
            openWebPage("")
        }

        findViewById<View>(R.id.btnServicePolicy).setOnClickListener {
            openWebPage("")
        }

        findViewById<View>(R.id.btnAboutAds).setOnClickListener {
            Toast.makeText(this, getString(R.string.toast_about_ads), Toast.LENGTH_LONG).show()
        }

        // Tải cấu hình từ Preferences
        loadSettingsFromPrefs()
    }



    private fun loadSettingsFromPrefs() {
        lifecycleScope.launch {
            // Load Icon size state
            val size = preferenceRepository.getIconSize()
            updateSizeButtonsUI(size)

            // Load Background Subtitle
            val bgType = preferenceRepository.getBgType()
            val bgPath = preferenceRepository.getBgImagePath()
            if (bgType == 2 && bgPath.isNotEmpty()) {
                txtBgSubtitle.text = getString(R.string.bg_custom_image)
            } else {
                txtBgSubtitle.text = getString(R.string.bg_default)
            }

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
        // Trạng thái nhỏ (small = 0.7f)
        if (size <= 0.8f) {
            setSelectedSegmentButton(btnSizeSmall)
            setUnselectedSegmentButton(btnSizeMiddle)
            setUnselectedSegmentButton(btnSizeLarge)
        }
        // Trạng thái trung bình (middle = 1.0f)
        else if (size <= 1.1f) {
            setUnselectedSegmentButton(btnSizeSmall)
            setSelectedSegmentButton(btnSizeMiddle)
            setUnselectedSegmentButton(btnSizeLarge)
        }
        // Trạng thái lớn (large = 1.3f)
        else {
            setUnselectedSegmentButton(btnSizeSmall)
            setUnselectedSegmentButton(btnSizeMiddle)
            setSelectedSegmentButton(btnSizeLarge)
        }
    }

    private fun setSelectedSegmentButton(textView: TextView) {
        textView.setBackgroundResource(R.drawable.bg_vintage_tab_selected)
        textView.backgroundTintList = null // Use the native drawable colors (EED4B6 with 2E1C0C stroke!)
        textView.setTextColor(Color.parseColor("#2E1C0C"))
    }

    private fun setUnselectedSegmentButton(textView: TextView) {
        textView.setBackgroundColor(Color.TRANSPARENT)
        textView.setTextColor(Color.parseColor("#64748B"))
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
