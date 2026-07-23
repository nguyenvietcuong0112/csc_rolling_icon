package com.iconchanger.wallpaper.rolling.icons.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.card.MaterialCardView
import com.iconchanger.wallpaper.rolling.icons.R
import com.iconchanger.wallpaper.rolling.icons.model.ApiWallpaperItem

class ApiWallpaperAdapter(
    private val items: List<ApiWallpaperItem>,
    private val onItemClick: (ApiWallpaperItem) -> Unit
) : RecyclerView.Adapter<ApiWallpaperAdapter.ViewHolder>() {

    private var selectedPosition = 0

    fun resetSelection() {
        selectedPosition = 0
    }

    fun getSelectedItem(): ApiWallpaperItem? {
        return if (selectedPosition in items.indices) items[selectedPosition] else null
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardWallpaper: MaterialCardView = view.findViewById(R.id.cardWallpaper)
        val imgThumbnail: ImageView = view.findViewById(R.id.imgThumbnail)
        val imgSelectedOverlay: ImageView = view.findViewById(R.id.imgSelectedOverlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_api_wallpaper, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.itemView.context
        val item = items[position]

        holder.imgThumbnail.load(item.thumbnailImageUrl) {
            crossfade(true)
            placeholder(R.drawable.bg_card_border)
            error(R.drawable.bg_card_border)
        }

        val isSelected = position == selectedPosition
        if (isSelected) {
            holder.cardWallpaper.strokeColor = ContextCompat.getColor(context, R.color.cosmic_accent)
            holder.imgSelectedOverlay.setImageResource(R.drawable.ic_check_circle)
        } else {
            holder.cardWallpaper.strokeColor = Color.TRANSPARENT
            holder.imgSelectedOverlay.setImageResource(R.drawable.ic_circle_unselected)
        }

        holder.itemView.setOnClickListener {
            val previous = selectedPosition
            selectedPosition = holder.bindingAdapterPosition
            if (previous >= 0) notifyItemChanged(previous)
            notifyItemChanged(selectedPosition)
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
