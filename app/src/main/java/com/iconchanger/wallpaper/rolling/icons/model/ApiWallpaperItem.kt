package com.iconchanger.wallpaper.rolling.icons.model

data class ApiWallpaperItem(
    val id: String,
    val category: String,
    val name: String,
    val originalImageUrl: String,
    val thumbnailImageUrl: String,
    val views: Long
)
