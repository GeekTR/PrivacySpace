package cn.geektang.privacyspace.ui.screen.setconnectedapps

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import cn.geektang.privacyspace.ConfigConstant
import cn.geektang.privacyspace.bean.AppInfo
import cn.geektang.privacyspace.util.AppHelper.loadAllAppList
import cn.geektang.privacyspace.util.AppHelper.sortApps
import cn.geektang.privacyspace.util.ConfigHelper
import cn.geektang.privacyspace.util.setDifferentValue
import com.android.server.connectivity.PacManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SetConnectedAppsViewModel(
    private val context: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(context) {
    private val targetPackageName = savedStateHandle.get<String>("targetPackageName")
    private val allAppListFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    val appNameFlow = MutableStateFlow("")
    val appListFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    val whitelistFlow = MutableStateFlow<Set<String>>(emptySet())
    val showSystemAppsFlow = MutableStateFlow(false)
    private var isModified = false

    init {
        viewModelScope.launch {
            launch {
                withContext(Dispatchers.IO) {
                    if (null != targetPackageName) {
                        val applicationInfo = context.packageManager.getApplicationInfo(
                            targetPackageName,
                            PackageManager.MATCH_UNINSTALLED_PACKAGES
                        )
                        appNameFlow.emit(
                            applicationInfo.loadLabel(context.packageManager).toString()
                        )
                    }
                }
            }

            launch {
                ConfigHelper.configDataFlow.collect {
                    whitelistFlow.value = ConfigHelper.configDataFlow
                        .value.connectedApps[targetPackageName] ?: emptySet()
                }
            }

            val defaultWhitelist = ConfigConstant.defaultWhitelist
            allAppListFlow.value =
                context.loadAllAppList()
                    .filter {
                        !defaultWhitelist.contains(it.packageName)
                    }
                    .sortApps(context = context, toTopCollections = whitelistFlow.value)
            updateAppInfoListFlow()
        }
    }

    private fun updateAppInfoListFlow() {
        val appList = allAppListFlow.value
        if (showSystemAppsFlow.value) {
            appListFlow.setDifferentValue(appList)
        } else {
            appListFlow.setDifferentValue(appList.filter {
                !it.isSystemApp
            })
        }
    }

    fun addApp2HiddenList(appInfo: AppInfo) {
        isModified = true
        val whitelistNew = whitelistFlow.value.toMutableSet()
        whitelistNew.add(appInfo.packageName)
        whitelistFlow.value = whitelistNew
    }

    fun removeApp2HiddenList(appInfo: AppInfo) {
        isModified = true
        val whitelistNew = whitelistFlow.value.toMutableSet()
        whitelistNew.remove(appInfo.packageName)
        whitelistFlow.value = whitelistNew
    }

    fun tryUpdateConfig() {
        val targetPackageName = targetPackageName
        if (isModified && !targetPackageName.isNullOrEmpty()) {
            val connectedAppsNew = ConfigHelper.configDataFlow.value.connectedApps.toMutableMap()
            connectedAppsNew[targetPackageName] = whitelistFlow.value
            ConfigHelper.updateConnectedApps(context, connectedAppsNew)
            isModified = false
        }
    }

    fun setShowSystemApps(showSystemApps: Boolean) {
        if (showSystemApps != showSystemAppsFlow.value) {
            showSystemAppsFlow.value = showSystemApps
            updateAppInfoListFlow()
        }
    }
}