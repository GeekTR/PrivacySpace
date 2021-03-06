package cn.geektang.privacyspace.ui.screen.blind

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.geektang.privacyspace.BuildConfig
import cn.geektang.privacyspace.bean.AppInfo
import cn.geektang.privacyspace.util.AppHelper
import cn.geektang.privacyspace.util.AppHelper.isMatch
import cn.geektang.privacyspace.util.AppHelper.sortApps
import cn.geektang.privacyspace.util.ConfigHelper
import cn.geektang.privacyspace.util.setDifferentValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*

class AddBlindAppsViewModel(private val context: Application) : AndroidViewModel(context),
    AddBlindAppsActions {
    val appInfoListFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    val allAppInfoListFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    val whitelistFlow = MutableStateFlow<Set<String>>(emptySet())
    val blindAppsListFlow = MutableStateFlow<Set<String>>(emptySet())
    val isShowSystemAppsFlow = MutableStateFlow(false)
    val searchTextFlow = MutableStateFlow("")
    private var isModified = false
    private val connectedAppsCache = mutableMapOf<String, Set<String>>()
    private val hiddenAppList = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            launch {
                ConfigHelper.configDataFlow.collect {
                    whitelistFlow.value = it.whitelist.toSet()
                    blindAppsListFlow.value = it.blind?.toSet() ?: emptySet()

                    connectedAppsCache.clear()
                    connectedAppsCache.putAll(it.connectedApps)

                    hiddenAppList.clear()
                    hiddenAppList.addAll(it.hiddenAppList)
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
                apps.sortApps(context = context, toTopCollections = blindAppsListFlow.value)
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
                it.isMatch(searchTextLowercase)
            }
        }
        if (isShowSystemAppsFlow.value) {
            appInfoListFlow.setDifferentValue(appList)
        } else {
            val hiddenAppList = blindAppsListFlow.value
            appInfoListFlow.setDifferentValue(appList.filter {
                BuildConfig.APPLICATION_ID != it.packageName &&
                        (!it.isSystemApp || hiddenAppList.contains(it.packageName))
            })
        }
    }

    override fun addApp2BlindList(appInfo: AppInfo) {
        val newAppsList = blindAppsListFlow.value.toMutableSet()
        newAppsList.add(appInfo.packageName)
        blindAppsListFlow.value = newAppsList
        isModified = true
    }

    override fun removeApp2BlindList(appInfo: AppInfo) {
        val newAppsList = blindAppsListFlow.value.toMutableSet()
        newAppsList.remove(appInfo.packageName)
        blindAppsListFlow.value = newAppsList
        if (!hiddenAppList.contains(appInfo.packageName)) {
            connectedAppsCache.remove(appInfo.packageName)
        }
        isModified = true
    }

    override fun setSystemAppsVisible(showSystemApps: Boolean) {
        if (showSystemApps != isShowSystemAppsFlow.value) {
            isShowSystemAppsFlow.value = showSystemApps
            updateAppInfoListFlow()
        }
    }

    fun tryUpdateConfig() {
        if (isModified) {
            val blindAppsList = blindAppsListFlow.value
            val whitelist = whitelistFlow.value.toMutableSet()
            for (app in blindAppsList) {
                if (whitelist.contains(app)) {
                    whitelist.remove(app)
                }
            }

            ConfigHelper.updateBlindApps(
                whitelistNew = whitelist,
                blindAppsListNew = blindAppsList,
                connectedAppsNew = connectedAppsCache
            )
            isModified = false
        }
    }

    override fun updateSearchText(searchText: String) {
        searchTextFlow.value = searchText
        updateAppInfoListFlow()
    }
}