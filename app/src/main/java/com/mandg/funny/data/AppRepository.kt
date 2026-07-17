package com.mandg.funny.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mandg.funny.model.AppInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map


class AppRepository(private val context: Context) {

    companion object {
        private val KEY_SELECTED_APPS = stringSetPreferencesKey("selected_packages")
    }

    fun getInstalledApps(): List<AppInfo> {
        val packageManager = context.packageManager
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val appList = ArrayList<AppInfo>()

        for (app in apps) {
            val packageName = app.packageName
            val appName = app.loadLabel(packageManager).toString()
            
            // Lọc: Chỉ giữ lại các ứng dụng thực sự có thể khởi chạy (có Launch Intent)
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null && appName.isNotEmpty()) {
                val className = launchIntent.component?.className ?: ""
                appList.add(
                    AppInfo(
                        packageName = packageName,
                        appName = appName,
                        className = className,
                        iconCacheKey = packageName,
                        type = AppInfo.TYPE_APP
                    )
                )
            }
        }
        return appList.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
    }

    suspend fun getSelectedApps(): List<AppInfo> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val packageManager = context.packageManager
        val selectedPackages = context.dataStore.data.map { preferences ->
            preferences[KEY_SELECTED_APPS]
        }.first()

        if (selectedPackages == null) {
            val allApps = getInstalledApps()
            val defaultSelected = allApps.take(24)
            val packages = defaultSelected.map { it.packageName }.toSet()
            saveSelectedApps(packages)
            allApps.forEach { app ->
                app.isSelected = packages.contains(app.packageName)
            }
            allApps.filter { it.isSelected }
        } else {
            val appList = ArrayList<AppInfo>()
            for (packageName in selectedPackages) {
                try {
                    val app = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                    val appName = app.loadLabel(packageManager).toString()
                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                    if (launchIntent != null && appName.isNotEmpty()) {
                        val className = launchIntent.component?.className ?: ""
                        appList.add(
                            AppInfo(
                                packageName = packageName,
                                appName = appName,
                                className = className,
                                iconCacheKey = packageName,
                                type = AppInfo.TYPE_APP,
                                isSelected = true
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Ứng dụng đã bị gỡ cài đặt
                }
            }
            appList
        }
    }

    suspend fun saveSelectedApps(packages: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_APPS] = packages
        }
    }
}
