package com.mandg.funny.data

import android.content.Context
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PreferenceRepository(private val context: Context) {

    companion object {
        val KEY_ICON_SIZE = floatPreferencesKey("icon_size")
        val KEY_DENSITY = floatPreferencesKey("density")
        val KEY_FRICTION = floatPreferencesKey("friction")
        val KEY_RESTITUTION = floatPreferencesKey("restitution")
        val KEY_ENABLE_SENSOR = booleanPreferencesKey("enable_sensor")
        val KEY_FPS_LIMIT = intPreferencesKey("fps_limit")

        // Cấu hình Nền
        val KEY_BG_TYPE = intPreferencesKey("bg_type")
        val KEY_BG_COLOR = stringPreferencesKey("bg_color")
        val KEY_BG_GRADIENT_START = stringPreferencesKey("bg_gradient_start")
        val KEY_BG_GRADIENT_END = stringPreferencesKey("bg_gradient_end")
        val KEY_BG_IMAGE_PATH = stringPreferencesKey("bg_image_path")

        // Bộ sưu tập ảnh cá nhân chọn làm icon
        val KEY_SELECTED_PHOTOS = stringSetPreferencesKey("selected_photo_uris")

        // Bộ sưu tập Emoji chọn làm icon
        val KEY_SELECTED_EMOJIS = stringSetPreferencesKey("selected_emojis")

        // Cấu hình float button và wallpaper touch
        val KEY_ENABLE_FLOAT_BUTTON = booleanPreferencesKey("enable_float_button")
        val KEY_ENABLE_WALLPAPER_TOUCH = booleanPreferencesKey("enable_wallpaper_touch")
    }

    suspend fun getIconSize(): Float = getValue(KEY_ICON_SIZE, 1.0f)
    suspend fun setIconSize(value: Float) = setValue(KEY_ICON_SIZE, value)

    suspend fun getDensity(): Float = getValue(KEY_DENSITY, 1.5f)
    suspend fun setDensity(value: Float) = setValue(KEY_DENSITY, value)

    suspend fun getFriction(): Float = getValue(KEY_FRICTION, 0.3f)
    suspend fun setFriction(value: Float) = setValue(KEY_FRICTION, value)

    suspend fun getRestitution(): Float = getValue(KEY_RESTITUTION, 0.5f)
    suspend fun setRestitution(value: Float) = setValue(KEY_RESTITUTION, value)

    suspend fun isSensorEnabled(): Boolean = getValue(KEY_ENABLE_SENSOR, true)
    suspend fun setSensorEnabled(value: Boolean) = setValue(KEY_ENABLE_SENSOR, value)

    suspend fun getFpsLimit(): Int = getValue(KEY_FPS_LIMIT, 60)
    suspend fun setFpsLimit(value: Int) = setValue(KEY_FPS_LIMIT, value)

    // Background Getters & Setters
    suspend fun getBgType(): Int = getValue(KEY_BG_TYPE, 0)
    suspend fun setBgType(value: Int) = setValue(KEY_BG_TYPE, value)

    suspend fun getBgColor(): String = getValue(KEY_BG_COLOR, "#121212")
    suspend fun setBgColor(value: String) = setValue(KEY_BG_COLOR, value)

    suspend fun getBgGradientStart(): String = getValue(KEY_BG_GRADIENT_START, "#1e272e")
    suspend fun setBgGradientStart(value: String) = setValue(KEY_BG_GRADIENT_START, value)

    suspend fun getBgGradientEnd(): String = getValue(KEY_BG_GRADIENT_END, "#0f0f10")
    suspend fun setBgGradientEnd(value: String) = setValue(KEY_BG_GRADIENT_END, value)

    suspend fun getBgImagePath(): String = getValue(KEY_BG_IMAGE_PATH, "")
    suspend fun setBgImagePath(value: String) = setValue(KEY_BG_IMAGE_PATH, value)

    // Selected Photos Set
    suspend fun getSelectedPhotos(): Set<String> = getValue(KEY_SELECTED_PHOTOS, emptySet())
    suspend fun setSelectedPhotos(uris: Set<String>) = setValue(KEY_SELECTED_PHOTOS, uris)

    // Selected Emojis Set
    suspend fun getSelectedEmojis(): Set<String> = getValue(KEY_SELECTED_EMOJIS, emptySet())
    suspend fun setSelectedEmojis(emojis: Set<String>) = setValue(KEY_SELECTED_EMOJIS, emojis)

    suspend fun isFloatButtonEnabled(): Boolean = getValue(KEY_ENABLE_FLOAT_BUTTON, true)
    suspend fun setFloatButtonEnabled(value: Boolean) = setValue(KEY_ENABLE_FLOAT_BUTTON, value)

    suspend fun isWallpaperTouchEnabled(): Boolean = getValue(KEY_ENABLE_WALLPAPER_TOUCH, true)
    suspend fun setWallpaperTouchEnabled(value: Boolean) = setValue(KEY_ENABLE_WALLPAPER_TOUCH, value)

    private suspend fun <T> getValue(key: Preferences.Key<T>, defaultValue: T): T {
        return context.dataStore.data.map { preferences ->
            preferences[key]
        }.first() ?: defaultValue
    }

    private suspend fun <T> setValue(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}
