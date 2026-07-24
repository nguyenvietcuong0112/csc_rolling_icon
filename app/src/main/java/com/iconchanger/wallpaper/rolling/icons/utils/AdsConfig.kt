package com.iconchanger.wallpaper.rolling.icons.utils

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.cscmobi.libraryads.ads.inter_ads.CSCInter
import com.cscmobi.libraryads.commons.utils.isInternetConnected
import com.iconchanger.wallpaper.rolling.icons.R

object AdsConfig {

    @Volatile
    private var isShowingOrLoadingAd = false

    fun showInterClickAd(
        activity: AppCompatActivity,
        view: View? = null,
        onAdClosedAction: () -> Unit
    ) {
        if (isShowingOrLoadingAd) {
            return
        }

        isShowingOrLoadingAd = true
        view?.isEnabled = false

        fun resetState() {
            isShowingOrLoadingAd = false
            try {
                view?.isEnabled = true
            } catch (_: Exception) {}
        }

        val isEnabled = RemoteConfigs.inter_click
        val interClickId = activity.getString(R.string.inter_click)

        // 1. Nếu không bật qc hoặc mất mạng ➔ Chạy thẳng action, không hiện Loading
        if (!isEnabled || !activity.isInternetConnected()) {
            resetState()
            onAdClosedAction()
            return
        }

        // 2. Khởi tạo & Hiển thị Dialog Fullscreen Loading
        val loadingDialog = DialogLoadingAd(activity)
        try {
            if (!activity.isFinishing && !activity.isDestroyed) {
                loadingDialog.show()
            }
        } catch (_: Exception) {
            resetState()
            onAdClosedAction()
            return
        }

        // Hàm phụ trợ ẩn Loading an toàn
        fun dismissLoading() {
            try {
                if (loadingDialog.isShowing && !activity.isFinishing && !activity.isDestroyed) {
                    loadingDialog.dismiss()
                }
            } catch (_: Exception) {}
        }

        CSCInter.loadAndShowInter(
            activity = activity,
            adId = interClickId,
            timeDelay = 500L,
            timeOut = 20000L,
            canShowId = isEnabled,
            onShown = {
                dismissLoading()
            },
            nextAction = { _ ->
                dismissLoading()
                resetState()
                onAdClosedAction()
            }
        )
    }
}
