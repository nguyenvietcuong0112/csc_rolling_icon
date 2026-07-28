package com.iconchanger.wallpaper.rolling.icons.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import com.iconchanger.wallpaper.rolling.icons.R
import com.iconchanger.wallpaper.rolling.icons.utils.RemoteConfigs

class SuccessActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_success)

        findViewById<Button>(R.id.btnOk)?.setOnClickListener {
            navigateToHome()
        }

//        loadNativeAd()
    }

    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

//    private fun loadNativeAd() {
//        val frAds = findViewById<android.widget.FrameLayout>(R.id.layoutAds) ?: return
//        com.cscmobi.libraryads.ads.native_ads.CSCNativeManager.showNative(
//            adFrame = frAds,
//            adName = "native_all",
//            adId = getString(R.string.native_all),
//            adLayout = R.layout.layout_native_media,
//            canShowAd = RemoteConfigs.native_all
//        )
//    }

    override fun onBackPressed() {
        navigateToHome()
    }
}
