package com.mandg.funny.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.mandg.funny.R
import com.mandg.funny.data.PreferenceRepository
import kotlinx.coroutines.launch

class PhotoIconPickerActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnClearAll: ImageView
    private lateinit var txtCount: TextView
    private lateinit var recyclerView: RecyclerView

    private lateinit var preferenceRepository: PreferenceRepository
    private val selectedPhotosList = ArrayList<String>()
    private lateinit var adapter: PhotoPickerAdapter

    // Sử dụng Activity Result API để mở trình chọn ảnh của hệ thống
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Cố gắng duy trì quyền đọc URI (persistable URI permission) để hiển thị sau khi khởi động lại app
            try {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Bỏ qua nếu không được cấp quyền duy trì
            }
            val uriString = it.toString()
            if (!selectedPhotosList.contains(uriString)) {
                selectedPhotosList.add(uriString)
                saveAndRefresh()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_icon_picker)

        preferenceRepository = PreferenceRepository(this)

        btnBack = findViewById(R.id.btnBack)
        btnClearAll = findViewById(R.id.btnClearAll)
        txtCount = findViewById(R.id.txtCount)
        recyclerView = findViewById(R.id.photo_recycler_view)

        btnBack.setOnClickListener {
            finish()
        }

        btnClearAll.setOnClickListener {
            if (selectedPhotosList.isNotEmpty()) {
                selectedPhotosList.clear()
                saveAndRefresh()
                Toast.makeText(this, "Đã xóa toàn bộ ảnh cá nhân!", Toast.LENGTH_SHORT).show()
            }
        }

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        adapter = PhotoPickerAdapter()
        recyclerView.adapter = adapter

        // Tải các ảnh cá nhân đã chọn từ kho lưu trữ
        lifecycleScope.launch {
            val saved = preferenceRepository.getSelectedPhotos()
            selectedPhotosList.clear()
            selectedPhotosList.addAll(saved)
            updateCountHeader()
            adapter.notifyDataSetChanged()
        }
    }

    private fun updateCountHeader() {
        txtCount.text = selectedPhotosList.size.toString()
    }

    private fun saveAndRefresh() {
        updateCountHeader()
        adapter.notifyDataSetChanged()
        lifecycleScope.launch {
            preferenceRepository.setSelectedPhotos(selectedPhotosList.toSet())
        }
    }

    private inner class PhotoPickerAdapter : RecyclerView.Adapter<PhotoPickerAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val photoContainer: View = view.findViewById(R.id.photo_container)
            val addContainer: View = view.findViewById(R.id.add_container)
            val imgPhoto: ImageView = view.findViewById(R.id.imgPhoto)
            val btnEditPhoto: ImageView = view.findViewById(R.id.btnEditPhoto)
            val btnDeletePhoto: ImageView = view.findViewById(R.id.btnDeletePhoto)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_picker, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (position == 0) {
                // Ô đầu tiên hiển thị nút thêm (+)
                holder.addContainer.visibility = View.VISIBLE
                holder.photoContainer.visibility = View.GONE
                holder.itemView.setOnClickListener {
                    pickImageLauncher.launch("image/*")
                }
            } else {
                // Các ô tiếp theo hiển thị ảnh đã chọn
                holder.addContainer.visibility = View.GONE
                holder.photoContainer.visibility = View.VISIBLE
                
                val uriString = selectedPhotosList[position - 1]
                holder.imgPhoto.load(Uri.parse(uriString)) {
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_report_image)
                }

                holder.btnEditPhoto.setOnClickListener {
                    Toast.makeText(this@PhotoIconPickerActivity, "Tính năng cắt ảnh sẽ được bổ sung ở bản cập nhật tiếp theo!", Toast.LENGTH_SHORT).show()
                }

                holder.btnDeletePhoto.setOnClickListener {
                    selectedPhotosList.removeAt(position - 1)
                    saveAndRefresh()
                }

                holder.itemView.setOnClickListener(null)
            }
        }

        override fun getItemCount(): Int = selectedPhotosList.size + 1
    }
}
