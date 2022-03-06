package cn.geektang.privacyspace.ui.screen.managehiddenapps

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.geektang.privacyspace.BuildConfig
import cn.geektang.privacyspace.bean.AppInfo
import cn.geektang.privacyspace.constant.ConfigConstant
import cn.geektang.privacyspace.util.AppHelper
import cn.geektang.privacyspace.util.AppHelper.getPackageInfo
import cn.geektang.privacyspace.util.AppHelper.getSharedUserId
import cn.geektang.privacyspace.util.AppHelper.sortApps
import cn.geektang.privacyspace.util.ConfigHelper
import cn.geektang.privacyspace.util.setDifferentValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*

class AddHiddenAppsViewModel(private val context: Application) : AndroidViewModel(context) {
    val appInfoListFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    val allAppInfoListFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    val hiddenAppListFlow = MutableStateFlow<Set<String>>(emptySet())
    val isShowSystemAppsFlow = MutableStateFlow(false)
    val searchTextFlow = MutableStateFlow("")
    private var isModified = false
    private val connectedAppsCache = mutableMapOf<String, Set<String>>()
    private val sharedUserIdMap = mutableMapOf<String, String>()

    init {
        viewModelScope.launch {
            launch {
                ConfigHelper.configDataFlow.collect {
                    hiddenAppListFlow.value = it.hiddenAppList.toSet()
                    connectedAppsCache.clear()
                    connectedAppsCache.putAll(it.connectedApps)
                    val sharedUserIdMapNew = it.sharedUserIdMap ?: emptyMap()
                    sharedUserIdMap.clear()
                    sharedUserIdMap.putAll(sharedUserIdMapNew)
                }
            }
            launch {
                loadAllAppList(context)
            }
        }
    }

    private suspend fun loadAllAppList(context: Application) {
        AppHelper.allApps.collect { apps ->
            val appList =
                apps.sortApps(context = context, toTopCollections = hiddenAppListFlow.value)
            allAppInfoListFlow.value = appList
            updateAppInfoListFlow()
        }
    }

    private fun updateAppInfoListFlow() {
        var appList = allAppInfoListFlow.value
        val searchText = searchTextFlow.value
        val searchTextLowercase = searchText.lowercase(Locale.getDefault())
        if (searchText.isNotEmpty()) {
            appList = appList.filter {
                it.packageName.lowercase(Locale.getDefault()).contains(searchTextLowercase)
                        || it.appName.lowercase(Locale.getDefault()).contains(searchTextLowercase)
            }
        }
        if (isShowSystemAppsFlow.value) {
            appInfoListFlow.setDifferentValue(appList)
        } else {
            val hiddenAppList = hiddenAppListFlow.value
            appInfoListFlow.setDifferentValue(appList.filter {
                !it.isSystemApp || hiddenAppList.contains(it.packageName)
            })
        }
    }

    fun addApp2HiddenList(appInfo: AppInfo) {
        val targetSharedUserId = appInfo.getSharedUserId(context)
        if (!targetSharedUserId.isNullOrEmpty()) {
            sharedUserIdMap[appInfo.packageName] = targetSharedUserId
        }

        val hiddenAppList = hiddenAppListFlow.value.toMutableSet()
        hiddenAppList.add(appInfo.packageName)
        hiddenAppListFlow.value = hiddenAppList
        if (appInfo.isXposedModule && appInfo.packageName != BuildConfig.APPLICATION_ID) {
            val packageInfo = getPackageInfo(
                context,
                appInfo.packageName,
                PackageManager.GET_META_DATA
            )
            val scopeList = if (packageInfo == null) {
                emptyList()
            } else {
                AppHelper.getXposedModuleScopeList(context, packageInfo.applicationInfo).filter {
                    it != ConfigConstant.ANDROID_FRAMEWORK
                }
            }

            if (scopeList.isNotEmpty()) {
                val connectedApps =
                    connectedAppsCache.getOrDefault(appInfo.packageName, mutableSetOf())
                        .toMutableSet()
                connectedApps.addAll(scopeList)
                connectedAppsCache[appInfo.packageName] = connectedApps
            }
        }
        isModified = true
    }

    fun removeApp2HiddenList(appInfo: AppInfo) {
        val hiddenAppList = hiddenAppListFlow.value.toMutableSet()
        hiddenAppList.remove(appInfo.packageName)
        hiddenAppListFlow.value = hiddenAppList
        connectedAppsCache.remove(appInfo.packageName)
        isModified = true
    }

    fun setShowSystemApps(showSystemApps: Boolean) {
        if (showSystemApps != isShowSystemAppsFlow.value) {
            isShowSystemAppsFlow.value = showSystemApps
            updateAppInfoListFlow()
        }
    }

    fun tryUpdateConfig() {
        if (isModified) {
            ConfigHelper.updateHiddenListAndConnectedApps(
                hiddenAppListFlow.value,
                connectedAppsCache,
                sharedUserIdMap
            )
            isModified = false
        }
    }

    fun updateSearchText(searchText: String) {
        searchTextFlow.value = searchText
        updateAppInfoListFlow()
    }
}