package com.iconchanger.wallpaper.rolling.icons.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.iconchanger.wallpaper.rolling.icons.R
import com.iconchanger.wallpaper.rolling.icons.ui.ApiWallpaperItem

class ApiWallpaperAdapter(
    private val items: List<ApiWallpaperItem>,
    private val onItemClick: (ApiWallpaperItem) -> Unit
) : RecyclerView.Adapter<ApiWallpaperAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgThumbnail: ImageView = view.findViewById(R.id.imgThumbnail)
        val imgSelectedOverlay: ImageView = view.findViewById(R.id.imgSelectedOverlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_api_wallpaper, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.imgThumbnail.load(item.thumbnailImageUrl) {
            crossfade(true)
            placeholder(R.drawable.bg_card_border)
            error(R.drawable.bg_card_border)
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
