package com.mandg.funny.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mandg.funny.R
import com.mandg.funny.data.AppRepository
import com.mandg.funny.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppPickerActivity : AppCompatActivity() {

    private lateinit var appRepository: AppRepository
    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEdit: EditText
    private lateinit var searchClear: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSave: Button

    private var allAppsList = listOf<AppInfo>()
    private var filteredList = mutableListOf<AppInfo>()
    private val selectedPackageNames = HashSet<String>()
    private lateinit var adapter: AppPickerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_picker)

        appRepository = AppRepository(this)

        recyclerView = findViewById(R.id.app_picker_recycler_view)
        searchEdit = findViewById(R.id.app_picker_search_edit_view)
        searchClear = findViewById(R.id.app_picker_search_edit_clear)
        progressBar = findViewById(R.id.loading_layout)
        btnSave = findViewById(R.id.btnSaveSelection)

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        adapter = AppPickerAdapter()
        recyclerView.adapter = adapter

        // Tải dữ liệu ứng dụng
        loadAppsData()

        // Lắng nghe tìm kiếm
        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.lowercase() ?: ""
                filterApps(query)
                searchClear.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        searchClear.setOnClickListener {
            searchEdit.setText("")
        }

        btnSave.setOnClickListener {
            scope.launch {
                progressBar.visibility = View.VISIBLE
                withContext(Dispatchers.IO) {
                    appRepository.saveSelectedApps(selectedPackageNames)
                }
                progressBar.visibility = View.GONE
                finish()
            }
        }
    }

    private fun loadAppsData() {
        scope.launch {
            progressBar.visibility = View.VISIBLE
            
            val (installed, selected) = withContext(Dispatchers.IO) {
                val inst = appRepository.getInstalledApps()
                val sel = appRepository.getSelectedApps().map { it.packageName }.toSet()
                Pair(inst, sel)
            }

            allAppsList = installed
            selectedPackageNames.clear()
            selectedPackageNames.addAll(selected)

            filterApps("")
            progressBar.visibility = View.GONE
        }
    }

    private fun filterApps(query: String) {
        filteredList.clear()
        if (query.isEmpty()) {
            filteredList.addAll(allAppsList)
        } else {
            allAppsList.forEach { app ->
                if (app.appName.lowercase().contains(query)) {
                    filteredList.add(app)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    private inner class AppPickerAdapter : RecyclerView.Adapter<AppPickerAdapter.ViewHolder>() {

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
            val app = filteredList[position]
            holder.titleView.text = app.appName

            // Load app icon
            val pm = packageManager
            try {
                val icon = pm.getApplicationIcon(app.packageName)
                holder.iconView.setImageDrawable(icon)
            } catch (e: Exception) {
                holder.iconView.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            val isSelected = selectedPackageNames.contains(app.packageName)
            holder.checkedView.setImageResource(if (isSelected) R.drawable.app_picker_checked else R.drawable.checkbox_unselected)

            holder.itemView.setOnClickListener {
                if (selectedPackageNames.contains(app.packageName)) {
                    selectedPackageNames.remove(app.packageName)
                    holder.checkedView.setImageResource(R.drawable.checkbox_unselected)
                } else {
                    selectedPackageNames.add(app.packageName)
                    holder.checkedView.setImageResource(R.drawable.app_picker_checked)
                }
            }
        }

        override fun getItemCount(): Int = filteredList.size
    }
}
