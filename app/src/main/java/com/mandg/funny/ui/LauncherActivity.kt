package com.mandg.funny.ui

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.mandg.funny.R
import com.mandg.funny.model.AppInfo
import com.mandg.funny.data.AppRepository
import com.mandg.funny.render.GameRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LauncherActivity : AndroidApplication() {

    var isGameMode = false
    private var score = 0
    private lateinit var txtGameScore: TextView

    private val coroutineScope = MainScope()

    private lateinit var btnDrawer: ImageView
    private lateinit var appDrawerLayout: FrameLayout
    private lateinit var drawerRecyclerView: RecyclerView
    private lateinit var btnMinimizeDrawer: ImageView
    private lateinit var btnSearch: ImageView

    private lateinit var appRepository: AppRepository
    private val appList = ArrayList<AppInfo>()
    private lateinit var drawerAdapter: DrawerAppAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isGameMode = intent.getBooleanExtra("is_game_mode", false)

        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            useGL30 = false
        }
        val gdxView = initializeForView(GameRenderer(this), config)

        setContentView(R.layout.activity_launcher)

        val gdxContainer = findViewById<FrameLayout>(R.id.gdxContainer)
        gdxContainer.addView(gdxView)

        txtGameScore = findViewById(R.id.txtGameScore)
        val layoutClock = findViewById<LinearLayout>(R.id.layoutClock)
        val btnExit = findViewById<Button>(R.id.btnExitLauncher)
        btnDrawer = findViewById(R.id.btnDrawer)
        appDrawerLayout = findViewById(R.id.appDrawerLayout)
        drawerRecyclerView = findViewById(R.id.drawerRecyclerView)
        btnMinimizeDrawer = findViewById(R.id.btnMinimizeDrawer)
        btnSearch = findViewById(R.id.btnSearch)

        appRepository = AppRepository(this)

        // Thiết lập RecyclerView cho Drawer
        drawerRecyclerView.layoutManager = GridLayoutManager(this, 4)
        drawerAdapter = DrawerAppAdapter()
        drawerRecyclerView.adapter = drawerAdapter

        if (isGameMode) {
            txtGameScore.visibility = View.VISIBLE
            layoutClock.visibility = View.GONE
            btnDrawer.visibility = View.GONE
            btnExit.visibility = View.VISIBLE
            btnExit.text = "Thoát Game"
            btnExit.setOnClickListener {
                finish()
            }
        } else {
            txtGameScore.visibility = View.GONE
            layoutClock.visibility = View.VISIBLE
            btnDrawer.visibility = View.VISIBLE
            btnExit.visibility = View.GONE
        }

        // Click mở Drawer
        btnDrawer.setOnClickListener {
            loadDrawerApps()
            appDrawerLayout.visibility = View.VISIBLE
        }

        // Click đóng Drawer
        btnMinimizeDrawer.setOnClickListener {
            appDrawerLayout.visibility = View.GONE
        }
        appDrawerLayout.setOnClickListener {
            appDrawerLayout.visibility = View.GONE
        }

        // Nút tìm kiếm nhanh
        btnSearch.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra("query", "")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Đang mở trình tìm kiếm...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadDrawerApps() {
        coroutineScope.launch {
            val apps = appRepository.getInstalledApps()
            appList.clear()
            
            // Thêm nút Cài đặt của chính Launcher vào danh sách để người dùng tiện truy cập
            appList.add(
                AppInfo(
                    packageName = packageName,
                    appName = "Launcher Settings",
                    className = SettingsActivity::class.java.name,
                    iconCacheKey = "app_settings",
                    type = AppInfo.TYPE_APP
                )
            )
            
            appList.addAll(apps)
            drawerAdapter.notifyDataSetChanged()
        }
    }

    override fun onBackPressed() {
        if (appDrawerLayout.visibility == View.VISIBLE) {
            appDrawerLayout.visibility = View.GONE
            return
        }

        if (isGameMode) {
            finish()
            return
        }

        // Hiển thị Dialog xác nhận thoát app đúng theo yêu cầu
        AlertDialog.Builder(this)
            .setTitle("Thoát ứng dụng")
            .setMessage("Bạn có chắc chắn muốn thoát khỏi Launcher?")
            .setPositiveButton("Thoát") { _, _ ->
                finish()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        super.onDestroy()
    }

    fun incrementScore() {
        score++
        runOnUiThread {
            txtGameScore.text = "Điểm: $score"
        }
    }

    // Adapter danh sách ứng dụng trong Drawer
    private inner class DrawerAppAdapter : RecyclerView.Adapter<DrawerAppAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val iconView: ImageView = view.findViewById(R.id.app_icon_view)
            val titleView: TextView = view.findViewById(R.id.app_title_view)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_drawer_app, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val appInfo = appList[position]
            holder.titleView.text = appInfo.appName

            if (appInfo.className == SettingsActivity::class.java.name) {
                // Biểu tượng cài đặt cho shortcut của Launcher
                holder.iconView.setImageResource(android.R.drawable.ic_menu_preferences)
                holder.itemView.setOnClickListener {
                    val intent = Intent(this@LauncherActivity, SettingsActivity::class.java)
                    startActivity(intent)
                    appDrawerLayout.visibility = View.GONE
                }
            } else {
                // Tải icon mặc định của ứng dụng
                try {
                    val icon = packageManager.getApplicationIcon(appInfo.packageName)
                    holder.iconView.setImageDrawable(icon)
                } catch (e: PackageManager.NameNotFoundException) {
                    holder.iconView.setImageResource(android.R.drawable.sym_def_app_icon)
                }

                holder.itemView.setOnClickListener {
                    val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                    if (launchIntent != null) {
                        startActivity(launchIntent)
                        appDrawerLayout.visibility = View.GONE
                    } else {
                        Toast.makeText(this@LauncherActivity, "Không thể mở ứng dụng này!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        override fun getItemCount(): Int = appList.size
    }
}
