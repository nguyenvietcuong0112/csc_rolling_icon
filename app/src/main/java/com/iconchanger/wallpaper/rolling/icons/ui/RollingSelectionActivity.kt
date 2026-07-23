package com.iconchanger.wallpaper.rolling.icons.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.iconchanger.wallpaper.rolling.icons.R
import com.iconchanger.wallpaper.rolling.icons.adapter.AppSelectionAdapter
import com.iconchanger.wallpaper.rolling.icons.adapter.EmojiSelectionAdapter
import com.iconchanger.wallpaper.rolling.icons.adapter.PhotoSelectionAdapter
import com.iconchanger.wallpaper.rolling.icons.data.AppRepository
import com.iconchanger.wallpaper.rolling.icons.data.PreferenceRepository
import com.iconchanger.wallpaper.rolling.icons.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RollingSelectionActivity : BaseActivity() {

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
    private lateinit var appAdapter: AppSelectionAdapter

    // Emojis tab data & views
    private lateinit var emojiRecyclerView: RecyclerView
    private lateinit var emojiTabSmileys: ImageView
    private lateinit var emojiTabAnimals: ImageView
    private lateinit var emojiTabLove: ImageView
    private lateinit var emojiTabJokes: ImageView
    private val selectedEmojisSet = HashSet<String>()
    private val emojiAppBindingsMap = HashMap<String, String>()
    private val emojiList = ArrayList<String>()
    private lateinit var emojiAdapter: EmojiSelectionAdapter

    private val emojiGroup = (1..83).map { String.format("emoji_emoji_%02d", it) }
    private val animalGroup = (1..28).map { String.format("emoji_animal_%02d", it) }
    private val loveGroup = (1..20).map { String.format("emoji_love_%02d", it) }
    private val jokeGroup = (1..22).map { String.format("emoji_joke_%02d", it) }

    // Photos tab data & views
    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var txtPhotoCount: TextView
    private val selectedPhotosList = ArrayList<String>()
    private lateinit var photoAdapter: PhotoSelectionAdapter

    private val selectPictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val photos = result.data?.getStringArrayListExtra("selected_photos")
            if (photos != null) {
                selectedPhotosList.clear()
                selectedPhotosList.addAll(photos)
                updatePhotoCount()
                photoAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun openCustomPicturePicker() {
        val intent = Intent(this, SelectPictureActivity::class.java).apply {
            putStringArrayListExtra("already_selected", selectedPhotosList)
        }
        selectPictureLauncher.launch(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rolling_selection)

        appRepository = AppRepository(this)
        preferenceRepository = PreferenceRepository(this)

        val defaultTab = intent.getIntExtra("default_tab", 0)
        val singleMode = intent.getBooleanExtra("single_mode", false)

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
        appAdapter = AppSelectionAdapter(filteredAppsList, selectedAppsSet)
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
        emojiAdapter = EmojiSelectionAdapter(
            emojiList = emojiList,
            selectedEmojisSet = selectedEmojisSet,
            emojiAppBindingsMap = emojiAppBindingsMap,
            onLinkAppClick = { emojiName, resId, boundPackage ->
                val dialog = BindAppDialog(this, resId, boundPackage) { newPackage ->
                    scope.launch {
                        preferenceRepository.setEmojiAppBinding(emojiName, newPackage)
                    }
                    if (newPackage.isNullOrEmpty()) {
                        emojiAppBindingsMap.remove(emojiName)
                    } else {
                        emojiAppBindingsMap[emojiName] = newPackage
                    }
                    val pos = emojiList.indexOf(emojiName)
                    if (pos >= 0) emojiAdapter.notifyItemChanged(pos)
                }
                dialog.show()
            }
        )
        emojiRecyclerView.adapter = emojiAdapter

        setupEmojiTabs()

        // 3. Setup Photos Tab
        photoRecyclerView = findViewById(R.id.photoRecyclerView)
        txtPhotoCount = findViewById(R.id.txtPhotoCount)
        findViewById<View>(R.id.btnEmptyAddPhoto)?.setOnClickListener {
            openCustomPicturePicker()
        }

        photoRecyclerView.layoutManager = GridLayoutManager(this, 3)
        photoAdapter = PhotoSelectionAdapter(selectedPhotosList) {
            openCustomPicturePicker()
        }
        photoRecyclerView.adapter = photoAdapter

        // Load all data
        loadData()

        // Click Tiếp tục
        btnNext.setOnClickListener {
            scope.launch {
                // Save everything
                withContext(Dispatchers.IO) {
                    appRepository.saveSelectedApps(selectedAppsSet)
                    preferenceRepository.setSelectedEmojis(selectedEmojisSet)
                    preferenceRepository.setSelectedPhotos(selectedPhotosList.toSet())
                }

                if (singleMode) {
                    val intent = Intent(this@RollingSelectionActivity, CustomizeActivity::class.java).apply {
                        putExtra("mode", "rolling")
                    }
                    startActivity(intent)
                    finish()
                } else {
                    // Go to wallpaper picker
                    val intent = Intent(
                        this@RollingSelectionActivity,
                        WallpaperPickerActivity::class.java
                    ).apply {
                        putExtra("mode", "rolling")
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }

        if (defaultTab in 0..2) {
            tabLayout.getTabAt(defaultTab)?.select()
        }

        if (singleMode) {
            tabLayout.visibility = View.GONE
            val txtHeaderTitle = findViewById<TextView>(R.id.txtHeaderTitle)
            if (defaultTab == 1) {
                txtHeaderTitle.text = getString(R.string.emoji_icon_title)
            } else if (defaultTab == 2) {
                txtHeaderTitle.text = getString(R.string.photo_icon_title)
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
        val activeBg = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_emoji_tab_selected)
        val inactiveBg = null

        emojiTabSmileys.background = if (group == 1) activeBg else inactiveBg
        emojiTabSmileys.alpha = if (group == 1) 1.0f else 0.55f

        emojiTabAnimals.background = if (group == 2) activeBg else inactiveBg
        emojiTabAnimals.alpha = if (group == 2) 1.0f else 0.55f

        emojiTabLove.background = if (group == 3) activeBg else inactiveBg
        emojiTabLove.alpha = if (group == 3) 1.0f else 0.55f

        emojiTabJokes.background = if (group == 4) activeBg else inactiveBg
        emojiTabJokes.alpha = if (group == 4) 1.0f else 0.55f

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
                val sel = appRepository.getSelectedApps().map { it.packageName }.toSet()
                Pair(inst, sel)
            }
            progressBar.visibility = View.GONE
            allAppsList = installed
            selectedAppsSet.clear()
            selectedAppsSet.addAll(selectedApps)
            filterApps("")

            // Load Emojis
            val selectedEmojis = preferenceRepository.getSelectedEmojis()
            val emojiBindings = preferenceRepository.getEmojiAppBindings()
            selectedEmojisSet.clear()
            selectedEmojisSet.addAll(selectedEmojis)
            emojiAppBindingsMap.clear()
            emojiAppBindingsMap.putAll(emojiBindings)
            selectEmojiTab(1)

            // Load Photos
            val savedPhotos = preferenceRepository.getSelectedPhotos()
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
        val isEmpty = selectedPhotosList.isEmpty()
        findViewById<View>(R.id.layoutEmptyPhotoState)?.visibility =
            if (isEmpty) View.VISIBLE else View.GONE
        findViewById<View>(R.id.photoRecyclerView)?.visibility =
            if (isEmpty) View.GONE else View.VISIBLE
    }
}
