package com.iconchanger.wallpaper.rolling.icons.render.mode

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import com.iconchanger.wallpaper.rolling.icons.render.GameRenderer

interface WallpaperModeStrategy {
    fun render(batch: SpriteBatch, camera: OrthographicCamera, delta: Float)
    fun checkTouch(touchPoint: Vector3, camera: OrthographicCamera): GameRenderer.IconData?
    fun reload()
    fun dispose()
}
