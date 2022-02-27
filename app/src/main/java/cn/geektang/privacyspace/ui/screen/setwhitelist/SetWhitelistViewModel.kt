package cn.geektang.privacyspace.ui.screen.setwhitelist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.geektang.privacyspace.ConfigConstant
import cn.geektang.privacyspace.bean.AppInfo
import cn.geektang.privacyspace.util.AppHelper.getSharedUserId
import cn.geektang.privacyspace.util.AppHelper.loadAllAppList
import cn.geektang.privacyspace.util.AppHelper.sortApps
import cn.geektang.privacyspace.util.ConfigHelper
import cn.geektang.privacyspace.util.setDifferentValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*

class SetWhitelistViewModel(private val context: Application) : AndroidViewModel(context) {
    val allAppListFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    val appListFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    val whitelistFlow = MutableStateFlow<Set<String>>(emptySet())
    val showSystemAppsFlow = MutableStateFlow(false)
    val searchTextFlow = MutableStateFlow("")
    private val sharedUserIdMap = mutableMapOf<String, String>()
    private var isModified = false

    init {
        viewModelScope.launch {
            launch {
                ConfigHelper.configDataFlow.collect {
                    whitelistFlow.value = ConfigHelper.configDataFlow.value.whitelist
                    val sharedUserIdMapNew =
                        ConfigHelper.configDataFlow.value.sharedUserIdMap ?: emptyMap()
                    sharedUserIdMap.clear()
                    sharedUserIdMap.putAll(sharedUserIdMapNew)
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

    fun addApp2Whitelist(appInfo: AppInfo) {
        val targetSharedUserId = appInfo.getSharedUserId(context)
        if (!targetSharedUserId.isNullOrEmpty()) {
            sharedUserIdMap[appInfo.packageName] = targetSharedUserId
        }

        val whitelist = whitelistFlow.value.toMutableSet()
        whitelist.add(appInfo.packageName)
        whitelistFlow.value = whitelist
        isModified = true
    }

    fun removeApp2Whitelist(appInfo: AppInfo) {
        val whitelist = whitelistFlow.value.toMutableSet()
        whitelist.remove(appInfo.packageName)
        whitelistFlow.value = whitelist
        isModified = true
    }

    private fun updateAppInfoListFlow() {
        var appList = allAppListFlow.value
        val searchText = searchTextFlow.value
        val searchTextLowercase = searchText.lowercase(Locale.getDefault())
        if (searchText.isNotEmpty()) {
            appList = appList.filter {
                it.packageName.lowercase(Locale.getDefault()).contains(searchTextLowercase)
                        || it.appName.lowercase(Locale.getDefault()).contains(searchTextLowercase)
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

    fun tryUpdateConfig() {
        if (isModified) {
            ConfigHelper.updateWhitelist(whitelistFlow.value, sharedUserIdMap)
            isModified = false
        }
    }

    fun setSystemAppsVisible(showSystemApps: Boolean) {
        if (showSystemAppsFlow.value != showSystemApps) {
            showSystemAppsFlow.value = showSystemApps
            updateAppInfoListFlow()
        }
    }

    fun updateSearchText(searchText: String) {
        searchTextFlow.value = searchText
        updateAppInfoListFlow()
    }
}