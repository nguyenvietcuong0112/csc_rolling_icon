package com.rolling.spinning.icon3d.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rolling.spinning.icon3d.R
import com.rolling.spinning.icon3d.data.AppRepository
import com.rolling.spinning.icon3d.data.PreferenceRepository
import com.rolling.spinning.icon3d.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SpinningAppPickerActivity : BaseActivity() {

    private lateinit var appRepository: AppRepository
    private lateinit var preferenceRepository: PreferenceRepository
    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var txtHeaderTitle: TextView
    private lateinit var btnNext: TextView
    private lateinit var recyclerViewGrid: RecyclerView
    private lateinit var recyclerViewHorizontal: RecyclerView
    private lateinit var txtSelectedCount: TextView
    private lateinit var btnClear: ImageView
    private lateinit var progressBar: android.view.View

    private var spinningPattern = "dual_circle"
    private var allAppsList = listOf<AppInfo>()
    private val selectedPackageNames = LinkedHashSet<String>() // Dùng LinkedHashSet để giữ thứ tự chọn

    private lateinit var gridAdapter: AppGridAdapter
    private lateinit var horizontalAdapter: SelectedAppsHorizontalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spinning_app_picker)

        appRepository = AppRepository(this)
        preferenceRepository = PreferenceRepository(this)

        spinningPattern = intent.getStringExtra("spinning_pattern") ?: "dual_circle"

        txtHeaderTitle = findViewById(R.id.txtHeaderTitle)
        btnNext = findViewById(R.id.btnNext)
        recyclerViewGrid = findViewById(R.id.appGridRecyclerView)
        recyclerViewHorizontal = findViewById(R.id.selectedAppsHorizontalRecyclerView)
        txtSelectedCount = findViewById(R.id.txtSelectedCount)
        btnClear = findViewById(R.id.btnClearSelection)
        progressBar = findViewById(R.id.progressBar)

        // Đặt tên Header dựa theo kiểu xoay
        txtHeaderTitle.text = when (spinningPattern) {
            "single_circle" -> getString(R.string.single_circle_title)
            "dual_circle" -> getString(R.string.dual_circle_title)
            "vortex" -> getString(R.string.vortex_spiral_title)
            else -> getString(R.string.spinning_icon_title)
        }

        // Cài đặt RecyclerViews
        recyclerViewGrid.layoutManager = GridLayoutManager(this, 4)
        gridAdapter = AppGridAdapter()
        recyclerViewGrid.adapter = gridAdapter

        recyclerViewHorizontal.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        horizontalAdapter = SelectedAppsHorizontalAdapter()
        recyclerViewHorizontal.adapter = horizontalAdapter

        // Lắng nghe Back
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Lắng nghe Clear
        btnClear.setOnClickListener {
            selectedPackageNames.clear()
            updateSelectionViews()
        }

        // Lắng nghe Next
        btnNext.setOnClickListener {
            if (selectedPackageNames.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_select_one_app), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            scope.launch {
                progressBar.visibility = View.VISIBLE
                preferenceRepository.setSelectedAppsSpinning(selectedPackageNames)
                preferenceRepository.setSpinningPattern(spinningPattern)
                progressBar.visibility = View.GONE

                val intent = Intent(this@SpinningAppPickerActivity, WallpaperPickerActivity::class.java).apply {
                    putExtra("mode", "spinning")
                }
                startActivity(intent)
            }
        }

        loadAppsData()
    }

    private fun loadAppsData() {
        scope.launch {
            progressBar.visibility = View.VISIBLE
            
            val (installed, selected) = withContext(Dispatchers.IO) {
                val inst = appRepository.getInstalledApps()
                val sel = preferenceRepository.getSelectedAppsSpinning()
                Pair(inst, sel)
            }

            allAppsList = installed
            selectedPackageNames.clear()
            
            if (selected.isEmpty()) {
                // Nếu chưa từng chọn app nào cho chế độ xoay, lấy mặc định 12 app đầu tiên
                val defaultSelected = installed.take(12).map { it.packageName }
                selectedPackageNames.addAll(defaultSelected)
            } else {
                selectedPackageNames.addAll(selected)
            }

            updateSelectionViews()
            progressBar.visibility = View.GONE
        }
    }

    private fun updateSelectionViews() {
        gridAdapter.notifyDataSetChanged()
        horizontalAdapter.notifyDataSetChanged()
        
        txtSelectedCount.text = getString(R.string.selected_count_format, selectedPackageNames.size, allAppsList.size)
        
        // Cuộn list ngang tới cuối khi chọn thêm phần tử mới
        if (selectedPackageNames.isNotEmpty()) {
            recyclerViewHorizontal.smoothScrollToPosition(selectedPackageNames.size - 1)
        }
    }

    // Adapter cho Grid hiển thị toàn bộ app cài đặt
    private inner class AppGridAdapter : RecyclerView.Adapter<AppGridAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val iconView: ImageView = view.findViewById(R.id.app_picker_icon_view)
            val titleView: TextView = view.findViewById(R.id.app_picker_title_view)
            val checkedView: ImageView = view.findViewById(R.id.app_picker_checked_view)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_picker, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = allAppsList[position]
            holder.titleView.text = app.appName

            val pm = packageManager
            try {
                val icon = pm.getApplicationIcon(app.packageName)
                holder.iconView.setImageDrawable(icon)
            } catch (e: Exception) {
                holder.iconView.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            val isSelected = selectedPackageNames.contains(app.packageName)
            holder.checkedView.setImageResource(if (isSelected) R.drawable.checkbox_selected else R.drawable.checkbox_unselected)

            holder.itemView.setOnClickListener {
                if (selectedPackageNames.contains(app.packageName)) {
                    selectedPackageNames.remove(app.packageName)
                } else {
                    selectedPackageNames.add(app.packageName)
                }
                updateSelectionViews()
            }
        }

        override fun getItemCount(): Int = allAppsList.size
    }

    // Adapter cho danh sách app nằm ngang bên dưới footer
    private inner class SelectedAppsHorizontalAdapter : RecyclerView.Adapter<SelectedAppsHorizontalAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val iconView: ImageView = view.findViewById(R.id.imgSelectedIcon)
            val removeView: ImageView = view.findViewById(R.id.btnRemoveIcon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_selected_app_horizontal, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val packageName = selectedPackageNames.toList()[position]

            val pm = packageManager
            try {
                val icon = pm.getApplicationIcon(packageName)
                holder.iconView.setImageDrawable(icon)
            } catch (e: Exception) {
                holder.iconView.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            // Click vào icon hoặc nút delete đều xóa app khỏi list chọn
            val removeAction = View.OnClickListener {
                selectedPackageNames.remove(packageName)
                updateSelectionViews()
            }
            holder.iconView.setOnClickListener(removeAction)
            holder.removeView.setOnClickListener(removeAction)
        }

        override fun getItemCount(): Int = selectedPackageNames.size
    }
}
