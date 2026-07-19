package com.rolling.spinning.icon3d.ui

import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.tabs.TabLayout
import com.rolling.spinning.icon3d.R
import com.rolling.spinning.icon3d.data.AppRepository
import com.rolling.spinning.icon3d.data.PreferenceRepository
import com.rolling.spinning.icon3d.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShapeSelectionActivity : BaseActivity() {

    private lateinit var appRepository: AppRepository
    private lateinit var preferenceRepository: PreferenceRepository
    private val scope = CoroutineScope(Dispatchers.Main)

    // Tab Views
    private lateinit var tabLayout: TabLayout
    private lateinit var layoutTabApps: LinearLayout
    private lateinit var layoutTabEmojis: LinearLayout
    private lateinit var layoutTabPhotos: LinearLayout
    private lateinit var btnNext: Button

    // Apps tab data & views
    private lateinit var appRecyclerView: RecyclerView
    private lateinit var appSearchEdit: EditText
    private lateinit var appSearchClear: ImageView
    private lateinit var progressBar: android.view.View
    private var allAppsList = listOf<AppInfo>()
    private var filteredAppsList = mutableListOf<AppInfo>()
    private val selectedAppsSet = HashSet<String>()
    private lateinit var appAdapter: AppAdapter

    // Emojis tab data & views
    private lateinit var emojiRecyclerView: RecyclerView
    private lateinit var emojiTabSmileys: ImageView
    private lateinit var emojiTabAnimals: ImageView
    private lateinit var emojiTabLove: ImageView
    private lateinit var emojiTabJokes: ImageView
    private val selectedEmojisSet = HashSet<String>()
    private val emojiList = ArrayList<String>()
    private lateinit var emojiAdapter: EmojiAdapter

    private val emojiGroup = (1..83).map { String.format("emoji_emoji_%02d", it) }
    private val animalGroup = (1..28).map { String.format("emoji_animal_%02d", it) }
    private val loveGroup = (1..20).map { String.format("emoji_love_%02d", it) }
    private val jokeGroup = (1..22).map { String.format("emoji_joke_%02d", it) }

    // Photos tab data & views
    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var txtPhotoCount: TextView
    private val selectedPhotosList = ArrayList<String>()
    private lateinit var photoAdapter: PhotoAdapter

    // Photo picker launcher
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore
            }
            val uriString = it.toString()
            if (!selectedPhotosList.contains(uriString)) {
                selectedPhotosList.add(uriString)
                photoAdapter.notifyDataSetChanged()
                updatePhotoCount()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shape_selection)

        appRepository = AppRepository(this)
        preferenceRepository = PreferenceRepository(this)

        // Bind layouts
        tabLayout = findViewById(R.id.tabLayout)
        layoutTabApps = findViewById(R.id.layoutTabApps)
        layoutTabEmojis = findViewById(R.id.layoutTabEmojis)
        layoutTabPhotos = findViewById(R.id.layoutTabPhotos)
        btnNext = findViewById(R.id.btnNext)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Setup Tabs
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_apps)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_emoji)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_photos)))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        layoutTabApps.visibility = View.VISIBLE
                        layoutTabEmojis.visibility = View.GONE
                        layoutTabPhotos.visibility = View.GONE
                    }
                    1 -> {
                        layoutTabApps.visibility = View.GONE
                        layoutTabEmojis.visibility = View.VISIBLE
                        layoutTabPhotos.visibility = View.GONE
                    }
                    2 -> {
                        layoutTabApps.visibility = View.GONE
                        layoutTabEmojis.visibility = View.GONE
                        layoutTabPhotos.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // 1. Setup Apps Tab
        appRecyclerView = findViewById(R.id.appRecyclerView)
        appSearchEdit = findViewById(R.id.appSearchEdit)
        appSearchClear = findViewById(R.id.appSearchClear)
        progressBar = findViewById(R.id.progressBar)

        appRecyclerView.layoutManager = GridLayoutManager(this, 3)
        appAdapter = AppAdapter()
        appRecyclerView.adapter = appAdapter

        appSearchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.lowercase() ?: ""
                filterApps(query)
                appSearchClear.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        appSearchClear.setOnClickListener {
            appSearchEdit.setText("")
        }

        // 2. Setup Emojis Tab
        emojiRecyclerView = findViewById(R.id.emojiRecyclerView)
        emojiTabSmileys = findViewById(R.id.emojiTabSmileys)
        emojiTabAnimals = findViewById(R.id.emojiTabAnimals)
        emojiTabLove = findViewById(R.id.emojiTabLove)
        emojiTabJokes = findViewById(R.id.emojiTabJokes)

        emojiRecyclerView.layoutManager = GridLayoutManager(this, 4)
        emojiAdapter = EmojiAdapter()
        emojiRecyclerView.adapter = emojiAdapter

        setupEmojiTabs()

        // 3. Setup Photos Tab
        photoRecyclerView = findViewById(R.id.photoRecyclerView)
        txtPhotoCount = findViewById(R.id.txtPhotoCount)

        photoRecyclerView.layoutManager = GridLayoutManager(this, 3)
        photoAdapter = PhotoAdapter()
        photoRecyclerView.adapter = photoAdapter

        // Load all data
        loadData()

        // Click Tiếp tục
        btnNext.setOnClickListener {
            scope.launch {
                // Save everything to shape-specific preferences
                withContext(Dispatchers.IO) {
                    preferenceRepository.setSelectedAppsShape(selectedAppsSet)
                    preferenceRepository.setSelectedEmojisShape(selectedEmojisSet)
                    preferenceRepository.setSelectedPhotosShape(selectedPhotosList.toSet())
                }

                // Go to wallpaper picker
                val intent = Intent(this@ShapeSelectionActivity, WallpaperPickerActivity::class.java).apply {
                    putExtra("mode", "shape_path")
                }
                startActivity(intent)
                finish()
            }
        }
    }

    private fun setupEmojiTabs() {
        emojiTabSmileys.setOnClickListener { selectEmojiTab(1) }
        emojiTabAnimals.setOnClickListener { selectEmojiTab(2) }
        emojiTabLove.setOnClickListener { selectEmojiTab(3) }
        emojiTabJokes.setOnClickListener { selectEmojiTab(4) }
    }

    private fun selectEmojiTab(group: Int) {
        emojiTabSmileys.isSelected = (group == 1)
        emojiTabAnimals.isSelected = (group == 2)
        emojiTabLove.isSelected = (group == 3)
        emojiTabJokes.isSelected = (group == 4)

        emojiTabSmileys.setBackgroundColor(if (group == 1) 0xFFEED4B6.toInt() else 0)
        emojiTabAnimals.setBackgroundColor(if (group == 2) 0xFFEED4B6.toInt() else 0)
        emojiTabLove.setBackgroundColor(if (group == 3) 0xFFEED4B6.toInt() else 0)
        emojiTabJokes.setBackgroundColor(if (group == 4) 0xFFEED4B6.toInt() else 0)

        emojiList.clear()
        when (group) {
            1 -> emojiList.addAll(emojiGroup)
            2 -> emojiList.addAll(animalGroup)
            3 -> emojiList.addAll(loveGroup)
            4 -> emojiList.addAll(jokeGroup)
        }
        emojiAdapter.notifyDataSetChanged()
    }

    private fun loadData() {
        progressBar.visibility = View.VISIBLE
        scope.launch {
            // Load Apps
            val (installed, selectedApps) = withContext(Dispatchers.IO) {
                val inst = appRepository.getInstalledApps()
                val sel = preferenceRepository.getSelectedAppsShape()
                Pair(inst, sel)
            }
            progressBar.visibility = View.GONE
            allAppsList = installed
            selectedAppsSet.clear()
            if (selectedApps.isEmpty()) {
                // Default select 24 apps
                selectedAppsSet.addAll(allAppsList.take(24).map { it.packageName })
            } else {
                selectedAppsSet.addAll(selectedApps)
            }
            filterApps("")

            // Load Emojis
            val selectedEmojis = preferenceRepository.getSelectedEmojisShape()
            selectedEmojisSet.clear()
            selectedEmojisSet.addAll(selectedEmojis)
            selectEmojiTab(1)

            // Load Photos
            val savedPhotos = preferenceRepository.getSelectedPhotosShape()
            selectedPhotosList.clear()
            selectedPhotosList.addAll(savedPhotos)
            updatePhotoCount()
            photoAdapter.notifyDataSetChanged()
        }
    }

    private fun filterApps(query: String) {
        filteredAppsList.clear()
        if (query.isEmpty()) {
            filteredAppsList.addAll(allAppsList)
        } else {
            allAppsList.forEach { app ->
                if (app.appName.lowercase().contains(query)) {
                    filteredAppsList.add(app)
                }
            }
        }
        appAdapter.notifyDataSetChanged()
    }

    private fun updatePhotoCount() {
        txtPhotoCount.text = getString(R.string.photos_count_format, selectedPhotosList.size)
    }

    // App Adapter
    private inner class AppAdapter : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

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
            val app = filteredAppsList[position]
            holder.titleView.text = app.appName

            val pm = packageManager
            try {
                val icon = pm.getApplicationIcon(app.packageName)
                holder.iconView.setImageDrawable(icon)
            } catch (e: Exception) {
                holder.iconView.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            val isSelected = selectedAppsSet.contains(app.packageName)
            holder.checkedView.setImageResource(if (isSelected) R.drawable.checkbox_selected else R.drawable.checkbox_unselected)

            holder.itemView.setOnClickListener {
                if (selectedAppsSet.contains(app.packageName)) {
                    selectedAppsSet.remove(app.packageName)
                    holder.checkedView.setImageResource(R.drawable.checkbox_unselected)
                } else {
                    selectedAppsSet.add(app.packageName)
                    holder.checkedView.setImageResource(R.drawable.checkbox_selected)
                }
            }
        }

        override fun getItemCount(): Int = filteredAppsList.size
    }

    // Emoji Adapter
    private inner class EmojiAdapter : RecyclerView.Adapter<EmojiAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val iconView: ImageView = view.findViewById(R.id.emoji_item_icon_view)
            val checkedView: ImageView = view.findViewById(R.id.emoji_item_icon_checked_view)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_emoji, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val emojiName = emojiList[position]
            val resId = resources.getIdentifier(emojiName, "drawable", packageName)

            if (resId != 0) {
                holder.iconView.setImageResource(resId)
            } else {
                holder.iconView.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            val isSelected = selectedEmojisSet.contains(emojiName)
            holder.checkedView.setImageResource(if (isSelected) R.drawable.checkbox_selected else R.drawable.checkbox_unselected)

            holder.itemView.setOnClickListener {
                if (selectedEmojisSet.contains(emojiName)) {
                    selectedEmojisSet.remove(emojiName)
                    holder.checkedView.setImageResource(R.drawable.checkbox_unselected)
                } else {
                    selectedEmojisSet.add(emojiName)
                    holder.checkedView.setImageResource(R.drawable.checkbox_selected)
                }
            }
        }

        override fun getItemCount(): Int = emojiList.size
    }

    // Photo Adapter
    private inner class PhotoAdapter : RecyclerView.Adapter<PhotoAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val photoContainer: View = view.findViewById(R.id.photo_container)
            val addContainer: View = view.findViewById(R.id.add_container)
            val imgPhoto: ImageView = view.findViewById(R.id.imgPhoto)
            val btnDeletePhoto: ImageView = view.findViewById(R.id.btnDeletePhoto)
            val btnEditPhoto: ImageView = view.findViewById(R.id.btnEditPhoto)
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
                    pickImageLauncher.launch("image/*")
                }
            } else {
                holder.addContainer.visibility = View.GONE
                holder.photoContainer.visibility = View.VISIBLE

                val uriString = selectedPhotosList[position - 1]
                holder.imgPhoto.load(Uri.parse(uriString)) {
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_report_image)
                }

                holder.btnEditPhoto.visibility = View.GONE

                holder.btnDeletePhoto.setOnClickListener {
                    selectedPhotosList.removeAt(position - 1)
                    notifyDataSetChanged()
                    updatePhotoCount()
                }
                holder.itemView.setOnClickListener(null)
            }
        }

        override fun getItemCount(): Int = selectedPhotosList.size + 1
    }
}
