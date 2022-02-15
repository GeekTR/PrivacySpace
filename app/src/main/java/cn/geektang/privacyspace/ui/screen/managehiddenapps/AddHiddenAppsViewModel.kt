package cn.geektang.privacyspace.ui.screen.managehiddenapps

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.geektang.privacyspace.BuildConfig
import cn.geektang.privacyspace.ConfigConstant
import cn.geektang.privacyspace.bean.AppInfo
import cn.geektang.privacyspace.ui.screen.launcher.getPackageInfo
import cn.geektang.privacyspace.util.AppHelper
import cn.geektang.privacyspace.util.AppHelper.loadAllAppList
import cn.geektang.privacyspace.util.AppHelper.sortApps
import cn.geektang.privacyspace.util.ConfigHelper
import cn.geektang.privacyspace.util.setDifferentValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AddHiddenAppsViewModel(private val context: Application) : AndroidViewModel(context) {
    val appInfoListFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    private val allAppInfoListFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    val hiddenAppListFlow = MutableStateFlow<Set<String>>(emptySet())
    val isShowSystemAppsFlow = MutableStateFlow(false)
    private var isModified = false
    private val connectedAppsCache = mutableMapOf<String, Set<String>>()

    init {
        viewModelScope.launch {
            launch {
                ConfigHelper.configDataFlow.collect {
                    hiddenAppListFlow.value = it.hiddenAppList.toSet()
                    connectedAppsCache.clear()
                    connectedAppsCache.putAll(it.connectedApps)
                }
            }
            loadAllAppList(context)
        }
    }

    private suspend fun loadAllAppList(context: Application) {
        val appList = context.loadAllAppList()
            .sortApps(context = context, toTopCollections = hiddenAppListFlow.value)
        allAppInfoListFlow.value = appList
        updateAppInfoListFlow()
    }

    private fun updateAppInfoListFlow() {
        val appList = allAppInfoListFlow.value
        if (isShowSystemAppsFlow.value) {
            appInfoListFlow.setDifferentValue(appList)
        } else {
            appInfoListFlow.setDifferentValue(appList.filter {
                !it.isSystemApp
            })
        }
    }

    fun addApp2HiddenList(appInfo: AppInfo) {
        val hiddenAppList = hiddenAppListFlow.value.toMutableSet()
        hiddenAppList.add(appInfo.packageName)
        hiddenAppListFlow.value = hiddenAppList
        if (appInfo.isXposedModule && appInfo.packageName != BuildConfig.APPLICATION_ID) {
            val packageInfo = getPackageInfo(
                context,
                appInfo.packageName,
                PackageManager.GET_META_DATA
            )
            val scopeList =
                AppHelper.getXposedModuleScopeList(context, packageInfo.applicationInfo).filter {
                    it != ConfigConstant.ANDROID_FRAMEWORK
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
                context,
                hiddenAppListFlow.value,
                connectedAppsCache
            )
            isModified = false
        }
    }
}