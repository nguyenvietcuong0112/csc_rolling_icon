package com.mandg.funny.wallpaper

import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService
import com.mandg.funny.render.GameRenderer

class RollingWallpaperService : AndroidLiveWallpaperService() {
    override fun onCreateApplication() {
        super.onCreateApplication()
        
        val config = AndroidApplicationConfiguration().apply {
            getTouchEventsForLiveWallpaper = true
            useAccelerometer = true
            // Hỗ trợ RGBA8888 để nền suốt hoặc mượt
            r = 8
            g = 8
            b = 8
            a = 8
        }
        
        val renderer = GameRenderer(applicationContext)
        initialize(renderer, config)
    }
}
