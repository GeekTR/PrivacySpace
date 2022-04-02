package cn.geektang.privacyspace.ui.screen.launcher

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.geektang.privacyspace.R
import cn.geektang.privacyspace.bean.AppInfo
import cn.geektang.privacyspace.bean.ConfigData
import cn.geektang.privacyspace.bean.SystemUserInfo
import cn.geektang.privacyspace.util.AppHelper
import cn.geektang.privacyspace.util.AppHelper.getPackageInfo
import cn.geektang.privacyspace.util.AppHelper.isXposedModule
import cn.geektang.privacyspace.util.ConfigHelper
import cn.geektang.privacyspace.util.showToast
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class LauncherViewModel(private val context: Application) : AndroidViewModel(context) {
    val hiddenAppList = mutableStateListOf<AppInfo>()
    val configData = mutableStateOf(ConfigData.EMPTY)
    val systemUsers = mutableStateListOf<SystemUserInfo>()
    val multiUserConfig = mutableStateMapOf<String, Set<Int>>()
    private val sharedUserIdMap = mutableMapOf<String, String>()
    val connectedApps = mutableMapOf<String, Set<String>>()
    private var needSync = false
    private val blindApps = mutableSetOf<String>()

    init {
        ConfigHelper.initConfig(context)
        viewModelScope.launch {
            systemUsers.clear()
            systemUsers.addAll(ConfigHelper.queryAllUsers() ?: emptyList())
        }
        viewModelScope.launch {
            ConfigHelper.configDataFlow.collect {
                configData.value = it
                hiddenAppList.clear()
                hiddenAppList.addAll(it.hiddenAppList.mapToAppInfoList())

                sharedUserIdMap.clear()
                sharedUserIdMap.putAll(it.sharedUserIdMap ?: emptyMap())

                connectedApps.clear()
                connectedApps.putAll(it.connectedApps)

                multiUserConfig.clear()
                multiUserConfig.putAll(it.multiUserConfig ?: emptyMap())

                blindApps.clear()
                blindApps.addAll(it.blind ?: emptySet())
            }
        }
    }

    private fun Set<String>.mapToAppInfoList(): List<AppInfo> {
        return this.mapNotNull { packageName ->
            try {
                getPackageInfo(
                    context = context,
                    packageName = packageName,
                    PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.GET_META_DATA
                )
            } catch (e: Throwable) {
                null
            }
        }
            .sortedWith(DisplayNameComparator(context.packageManager))
            .map { packageInfo ->
                val applicationInfo = packageInfo.applicationInfo
                val appName = applicationInfo.loadLabel(context.packageManager).toString()
                val appIcon = applicationInfo.loadIcon(context.packageManager)
                val isXposedModule = applicationInfo.isXposedModule()
                AppInfo(
                    applicationInfo = applicationInfo,
                    packageName = applicationInfo.packageName,
                    appName = appName,
                    appIcon = appIcon,
                    sharedUserId = packageInfo.sharedUserId,
                    isSystemApp = AppHelper.isSystemApp(applicationInfo),
                    isXposedModule = isXposedModule
                )
            }
    }

    class DisplayNameComparator(packageManager: PackageManager) : Comparator<PackageInfo> {
        private val innerComparator = ApplicationInfo.DisplayNameComparator(packageManager)
        override fun compare(p0: PackageInfo, p1: PackageInfo): Int {
            return innerComparator.compare(p0.applicationInfo, p1.applicationInfo)
        }
    }

    fun cancelHide(appInfo: AppInfo) {
        val hasChange = this.hiddenAppList.removeIf { it.packageName == appInfo.packageName }
        if (hasChange) {
            multiUserConfig.remove(appInfo.packageName)
            if (!blindApps.contains(appInfo.packageName)) {
                connectedApps.remove(appInfo.packageName)
            }
        }
        needSync = needSync or hasChange
    }

    fun connectTo(sourceApp: AppInfo, targetApp: String) {
        val connectedAppsForSourceApp =
            connectedApps[sourceApp.packageName]?.toMutableSet() ?: mutableSetOf()
        connectedAppsForSourceApp.add(targetApp)
        connectedApps[sourceApp.packageName] = connectedAppsForSourceApp

        val sharedUserIdForSourceApp = sourceApp.sharedUserId
        if (!sharedUserIdForSourceApp.isNullOrEmpty()) {
            sharedUserIdMap[sourceApp.packageName] = sharedUserIdForSourceApp
        }

        val sharedUserIdForTargetApp = sourceApp.sharedUserId
        if (!sharedUserIdForTargetApp.isNullOrEmpty()) {
            sharedUserIdMap[targetApp] = sharedUserIdForTargetApp
        }

        needSync = true
    }

    fun disconnectTo(sourceApp: AppInfo, targetApp: String) {
        val connectedAppsForSourceApp =
            connectedApps[sourceApp.packageName]?.toMutableSet() ?: mutableSetOf()
        val hasChange = connectedAppsForSourceApp.remove(targetApp)
        connectedApps[sourceApp.packageName] = connectedAppsForSourceApp
        needSync = needSync or hasChange
    }

    suspend fun syncConfig() {
        if (needSync) {
            needSync = false
            syncConfigInner()
        }
    }

    private suspend fun syncConfigInner() {
        ConfigHelper.updateConfig(
            configData.value.copy(
                hiddenAppList = hiddenAppList.map { it.packageName }.toSet(),
                sharedUserIdMap = sharedUserIdMap.toMap(),
                connectedApps = connectedApps.toMap(),
                multiUserConfig = multiUserConfig.toMap()
            )
        )
    }

    fun changeMultiUserConfig(appInfo: AppInfo, checkedUsers: Set<Int>?) {
        if (multiUserConfig[appInfo.packageName] != checkedUsers) {
            if (null == checkedUsers) {
                multiUserConfig.remove(appInfo.packageName)
            } else {
                multiUserConfig[appInfo.packageName] = checkedUsers
            }
            needSync = true

            if (ConfigHelper.getServerVersion() < 17) {
                context.showToast(R.string.configuration_takes_effect_after_restarting_the_phone_system)
            }
        }
    }
}