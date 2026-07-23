package com.iconchanger.wallpaper.rolling.icons.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.iconchanger.wallpaper.rolling.icons.R

data class CategoryItem(
    val name: String,
    val coverUrl: String
)

class CategoryAdapter(
    private val categories: List<CategoryItem>,
    private val onCategoryClick: (CategoryItem) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgCategoryCover: ImageView = view.findViewById(R.id.imgCategoryCover)
        val txtCategoryTitle: TextView = view.findViewById(R.id.txtCategoryTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_api_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = categories[position]
        holder.txtCategoryTitle.text = item.name

        holder.imgCategoryCover.load(item.coverUrl) {
            crossfade(true)
            placeholder(R.drawable.bg_card_border)
            error(R.drawable.bg_card_border)
        }

        holder.itemView.setOnClickListener {
            onCategoryClick(item)
        }
    }

    override fun getItemCount(): Int = categories.size
}
