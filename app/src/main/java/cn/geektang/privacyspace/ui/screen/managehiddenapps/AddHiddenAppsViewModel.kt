package cn.geektang.privacyspace.ui.screen.managehiddenapps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.geektang.privacyspace.bean.AppInfo
import cn.geektang.privacyspace.util.AppHelper.loadAllAppList
import cn.geektang.privacyspace.util.AppHelper.sortApps
import cn.geektang.privacyspace.util.ConfigHelper
import cn.geektang.privacyspace.util.setDifferentValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AddHiddenAppsViewModel(private val context: Application) : AndroidViewModel(context) {
    val appInfoListFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    private val allAppInfoListFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    val hiddenAppListFLow = MutableStateFlow<Set<String>>(emptySet())
    val isShowSystemAppsFlow = MutableStateFlow(false)
    private var isModified = false

    init {
        viewModelScope.launch {
            launch {
                loadAllAppList(context)
            }
            launch {
                val hiddenAppList = mutableSetOf<String>()
                ConfigHelper.loadAppListConfig(hiddenAppList)
                hiddenAppListFLow.value = hiddenAppList
                allAppInfoListFlow.value = allAppInfoListFlow.value
                    .toMutableList()
                    .sortApps(context = context, toTopCollections = hiddenAppList)
                updateAppInfoListFlow()
            }
        }
    }

    private suspend fun loadAllAppList(context: Application) {
        val appList = context.loadAllAppList()
            .sortApps(context = context, toTopCollections = hiddenAppListFLow.value)
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
        val hiddenAppList = hiddenAppListFLow.value.toMutableSet()
        hiddenAppList.add(appInfo.packageName)
        hiddenAppListFLow.value = hiddenAppList
        isModified = true
    }

    fun removeApp2HiddenList(appInfo: AppInfo) {
        val hiddenAppList = hiddenAppListFLow.value.toMutableSet()
        hiddenAppList.remove(appInfo.packageName)
        hiddenAppListFLow.value = hiddenAppList
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
            ConfigHelper.updateAppListConfig(context, hiddenAppListFLow.value)
            isModified = false
        }
    }
}