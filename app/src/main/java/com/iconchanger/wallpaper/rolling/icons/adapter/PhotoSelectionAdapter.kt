package com.iconchanger.wallpaper.rolling.icons.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.iconchanger.wallpaper.rolling.icons.R
import java.io.File

class PhotoSelectionAdapter(
    private val selectedPhotosList: List<String>,
    private val onAddPhotoClick: () -> Unit
) : RecyclerView.Adapter<PhotoSelectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photoContainer: View = view.findViewById(R.id.photo_container)
        val addContainer: View = view.findViewById(R.id.add_container)
        val imgPhoto: ImageView = view.findViewById(R.id.imgPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_picker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == 0) {
            holder.addContainer.visibility = View.VISIBLE
            holder.photoContainer.visibility = View.GONE
            holder.itemView.setOnClickListener {
                onAddPhotoClick()
            }
        } else {
            holder.addContainer.visibility = View.GONE
            holder.photoContainer.visibility = View.VISIBLE

            val uriString = selectedPhotosList[position - 1]
            val imgFile = File(uriString)
            if (imgFile.exists()) {
                holder.imgPhoto.load(imgFile) {
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_report_image)
                }
            } else {
                holder.imgPhoto.load(Uri.parse(uriString)) {
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_report_image)
                }
            }
        }
    }

    override fun getItemCount(): Int = selectedPhotosList.size + 1
}
