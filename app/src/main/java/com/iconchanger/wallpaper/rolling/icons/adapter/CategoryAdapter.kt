package com.iconchanger.wallpaper.rolling.icons.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.iconchanger.wallpaper.rolling.icons.R

class CategoryAdapter(
    private val categories: List<String>,
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var selectedIndex = 0

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtCategoryName: TextView = view.findViewById(R.id.txtCategoryName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category_tab, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.itemView.context
        val category = categories[position]
        holder.txtCategoryName.text = category

        val isSelected = position == selectedIndex
        if (isSelected) {
            holder.txtCategoryName.background = ContextCompat.getDrawable(context, R.drawable.bg_vintage_tab_selected)
            holder.txtCategoryName.setTextColor(ContextCompat.getColor(context, R.color.cosmic_accent))
        } else {
            holder.txtCategoryName.background = ContextCompat.getDrawable(context, R.drawable.bg_card_border)
            holder.txtCategoryName.setTextColor(ContextCompat.getColor(context, R.color.cosmic_text_primary))
        }

        holder.itemView.setOnClickListener {
            val previous = selectedIndex
            selectedIndex = holder.bindingAdapterPosition
            if (previous >= 0) notifyItemChanged(previous)
            notifyItemChanged(selectedIndex)
            onCategoryClick(category)
        }
    }

    override fun getItemCount(): Int = categories.size
}
