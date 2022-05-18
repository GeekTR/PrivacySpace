package cn.geektang.privacyspace.ui.screen.setconnectedapps

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import cn.geektang.privacyspace.bean.AppInfo
import cn.geektang.privacyspace.util.AppHelper
import cn.geektang.privacyspace.util.AppHelper.isMatch
import cn.geektang.privacyspace.util.AppHelper.sortApps
import cn.geektang.privacyspace.util.ConfigHelper
import cn.geektang.privacyspace.util.setDifferentValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class SetConnectedAppsViewModel(
    private val context: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(context) {
    private val targetPackageName = savedStateHandle.get<String>("targetPackageName")
    val allAppListFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    val appNameFlow = MutableStateFlow("")
    val appListFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    val whitelistFlow = MutableStateFlow<Set<String>>(emptySet())
    val showSystemAppsFlow = MutableStateFlow(true)
    val searchTextFlow = MutableStateFlow("")
    val showSelectAll = MutableStateFlow(false)
    private var isModified = false
    private val sharedUserIdMap = mutableMapOf<String, String>()

    init {
        viewModelScope.launch {
            launch {
                withContext(Dispatchers.IO) {
                    if (null != targetPackageName) {
                        val applicationInfo = AppHelper.getPackageInfo(
                            context,
                            targetPackageName,
                            PackageManager.MATCH_UNINSTALLED_PACKAGES
                        )?.applicationInfo
                        if (null != applicationInfo) {
                            appNameFlow.emit(
                                applicationInfo.loadLabel(context.packageManager).toString()
                            )
                        } else {
                            appNameFlow.emit(targetPackageName)
                        }
                    }
                }
            }

            launch {
                ConfigHelper.configDataFlow.collect {
                    whitelistFlow.value = ConfigHelper.configDataFlow
                        .value.connectedApps[targetPackageName] ?: emptySet()
                    val sharedUserIdMapNew =
                        ConfigHelper.configDataFlow.value.sharedUserIdMap ?: emptyMap()
                    sharedUserIdMap.clear()
                    sharedUserIdMap.putAll(sharedUserIdMapNew)
                }
            }

            launch {
                AppHelper.allApps.collect { apps ->
                    allAppListFlow.value =
                        apps.filter {
                            targetPackageName != it.packageName
                        }
                            .sortApps(context = context, toTopCollections = whitelistFlow.value)
                    updateAppInfoListFlow()
                }
            }
        }
    }

    private fun updateAppInfoListFlow() {
        var appList = allAppListFlow.value
        val searchText = searchTextFlow.value
        val searchTextLowercase = searchText.lowercase(Locale.getDefault())
        if (searchText.isNotEmpty()) {
            appList = appList.filter {
                it.isMatch(searchTextLowercase)
            }
        }
        if (showSystemAppsFlow.value) {
            appListFlow.setDifferentValue(appList)
        } else {
            val whitelist = whitelistFlow.value
            appListFlow.setDifferentValue(appList.filter {
                !it.isSystemApp || whitelist.contains(it.packageName)
            })
        }
    }

    fun addApp2HiddenList(appInfo: AppInfo) {
        val sharedUserId = appInfo.sharedUserId
        if (!sharedUserId.isNullOrEmpty()) {
            sharedUserIdMap[appInfo.packageName] = sharedUserId
        }
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

        if (appInfo.isSystemApp) {
            showSelectAll.value = false
        }
    }

    fun tryUpdateConfig() {
        val targetPackageName = targetPackageName
        if (isModified && !targetPackageName.isNullOrEmpty()) {
            val connectedAppsNew = ConfigHelper.configDataFlow.value.connectedApps.toMutableMap()
            connectedAppsNew[targetPackageName] = whitelistFlow.value
            ConfigHelper.updateConnectedApps(connectedAppsNew, sharedUserIdMap)
            isModified = false
        }
    }

    fun setShowSystemApps(showSystemApps: Boolean) {
        if (showSystemApps != showSystemAppsFlow.value) {
            showSystemAppsFlow.value = showSystemApps
            updateAppInfoListFlow()
        }
    }

    fun updateSearchText(searchText: String) {
        searchTextFlow.value = searchText
        updateAppInfoListFlow()
    }

    fun selectAllSystemApps(selectAll: Boolean) {
        if (selectAll && !showSystemAppsFlow.value) {
            setShowSystemApps(true)
        }
        val whitelistNew = whitelistFlow.value.toMutableSet()
        if (selectAll) {
            for (appInfo in appListFlow.value) {
                if (appInfo.isSystemApp && !whitelistNew.contains(appInfo.packageName)) {
                    whitelistNew.add(appInfo.packageName)

                    val targetSharedUserId = appInfo.sharedUserId
                    if (!targetSharedUserId.isNullOrEmpty()) {
                        sharedUserIdMap[appInfo.packageName] = targetSharedUserId
                    }
                }
            }
        } else {
            for (appInfo in appListFlow.value) {
                if (appInfo.isSystemApp) {
                    whitelistNew.remove(appInfo.packageName)
                }
            }
        }
        whitelistFlow.value = whitelistNew
        showSelectAll.value = selectAll
        isModified = true
    }
}