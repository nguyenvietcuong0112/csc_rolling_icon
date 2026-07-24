package com.iconchanger.wallpaper.rolling.icons.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.cscmobi.libraryads.ads.banner_ads.CSCBanner
import com.iconchanger.wallpaper.rolling.icons.R
import com.iconchanger.wallpaper.rolling.icons.utils.AdsConfig
import com.iconchanger.wallpaper.rolling.icons.utils.RemoteConfigs

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Nút Settings góc trên bên phải trên header (Không hiện Inter)
        findViewById<ImageView>(R.id.btnHeaderSettings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // 2. Click vào thẻ Rolling Icon hoặc nút Get Started của Rolling Icon
        val cardRollingIcon = findViewById<View>(R.id.cardRollingIcon)
        val btnGetStartedRolling = findViewById<TextView>(R.id.btnGetStartedRolling)
        cardRollingIcon.setOnClickListener {
            AdsConfig.showInterClickAd(this, it) {
                val intent = Intent(this, RollingSelectionActivity::class.java)
                startActivity(intent)
            }
        }
        btnGetStartedRolling.setOnClickListener {
            AdsConfig.showInterClickAd(this, it) {
                val intent = Intent(this, RollingSelectionActivity::class.java)
                startActivity(intent)
            }
        }

        // 3. Click vào thẻ Spinning Icon hoặc nút Get Started của Spinning Icon
        val cardSpinningIcon = findViewById<View>(R.id.cardSpinningIcon)
        val btnGetStartedSpinning = findViewById<TextView>(R.id.btnGetStartedSpinning)
        cardSpinningIcon.setOnClickListener {
            AdsConfig.showInterClickAd(this, it) {
                val intent = Intent(this, SpinningIconActivity::class.java)
                startActivity(intent)
            }
        }
        btnGetStartedSpinning.setOnClickListener {
            AdsConfig.showInterClickAd(this, it) {
                val intent = Intent(this, SpinningIconActivity::class.java)
                startActivity(intent)
            }
        }

        // 4. Click vào thẻ Shape Path Icon
        findViewById<View>(R.id.cardShapePathIcon).setOnClickListener {
            AdsConfig.showInterClickAd(this, it) {
                val intent = Intent(this, ShapeSelectorActivity::class.java)
                startActivity(intent)
            }
        }

        // 5. Click vào thẻ Emoji Icon
        findViewById<View>(R.id.cardEmojiIcon).setOnClickListener {
            AdsConfig.showInterClickAd(this, it) {
                val intent = Intent(this, RollingSelectionActivity::class.java).apply {
                    putExtra("default_tab", 1)
                    putExtra("single_mode", true)
                }
                startActivity(intent)
            }
        }

        // 6. Click vào thẻ Photo Icon
        findViewById<View>(R.id.cardPhotoIcon).setOnClickListener {
            AdsConfig.showInterClickAd(this, it) {
                val intent = Intent(this, RollingSelectionActivity::class.java).apply {
                    putExtra("default_tab", 2)
                    putExtra("single_mode", true)
                }
                startActivity(intent)
            }
        }

        // 7. Click vào thẻ Wallpaper -> Mở ApiWallpaperActivity (Online API Mode)
        findViewById<View>(R.id.cardWallpaper).setOnClickListener {
            AdsConfig.showInterClickAd(this, it) {
                val intent = Intent(this, ApiWallpaperActivity::class.java)
                startActivity(intent)
            }
        }

        // Xử lý nút Back hiển thị Popup xác nhận thoát ứng dụng
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitDialog()
            }
        })

        // Load Banner Collab at bottom of MainActivity
        loadBanner()
    }

    private fun loadBanner() {
        val frAds = findViewById<android.widget.FrameLayout>(R.id.layoutBanner) ?: return
        val isEnabled = RemoteConfigs.banner_collap_home

        CSCBanner.requestBanner(
            activity = this,
            id = getString(R.string.banner_collap_home),
            typeAds = CSCBanner.TypeAds.BANNER_COLLAPSIBLE_BOTTOM,
            adFrame = frAds,
            canShowAd = isEnabled
        )
    }

    private var activeExitDialog: android.app.Dialog? = null

    private fun showExitDialog() {
        if (activeExitDialog?.isShowing == true) return

        val dialog = android.app.Dialog(this)
        activeExitDialog = dialog
        val view = layoutInflater.inflate(R.layout.dialog_exit, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        val btnCancel = view.findViewById<android.widget.Button>(R.id.btnDialogCancel)
        val btnExit = view.findViewById<android.widget.Button>(R.id.btnDialogExit)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnExit.setOnClickListener {
            dialog.dismiss()
            finishAffinity()
        }

        dialog.setOnDismissListener {
            activeExitDialog = null
        }
        dialog.show()
    }
}

