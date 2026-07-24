package com.iconchanger.wallpaper.rolling.icons.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.cscmobi.libraryads.ads.native_ads.CSCNativeManager
import com.iconchanger.wallpaper.rolling.icons.R
import com.iconchanger.wallpaper.rolling.icons.utils.RemoteConfigs

class PermissionActivity : BaseActivity() {

    private lateinit var switchPhotoPermission: SwitchCompat
    private lateinit var btnContinue: Button

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        switchPhotoPermission.isChecked = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isPhotoPermissionGranted()) {
            val intent = Intent(this@PermissionActivity, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_permission)

        switchPhotoPermission = findViewById(R.id.switchPhotoPermission)
        btnContinue = findViewById(R.id.btnContinue)

        updatePermissionSwitchState()

        switchPhotoPermission.setOnClickListener {
            if (!isPhotoPermissionGranted()) {
                val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                requestPermissionLauncher.launch(permissionToRequest)
            } else {
                switchPhotoPermission.isChecked = true
            }
        }

        btnContinue.setOnClickListener {
            val intent = Intent(this@PermissionActivity, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            finish()
        }

        loadNativeAd()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionSwitchState()
    }

    private fun updatePermissionSwitchState() {
        switchPhotoPermission.isChecked = isPhotoPermissionGranted()
    }

    private fun isPhotoPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun loadNativeAd() {
        val frAds = findViewById<FrameLayout>(R.id.fr_ads) ?: return
        val isEnabled = RemoteConfigs.native_all

        CSCNativeManager.showNative(
            adFrame = frAds,
            adName = "native_all",
            adId = getString(R.string.native_all),
            adLayout = R.layout.layout_native_media,
            canShowAd = isEnabled
        )
    }
}
