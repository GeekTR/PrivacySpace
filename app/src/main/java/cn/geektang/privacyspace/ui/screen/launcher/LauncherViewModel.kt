package cn.geektang.privacyspace.ui.screen.launcher

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.geektang.privacyspace.bean.AppInfo
import cn.geektang.privacyspace.util.AppHelper
import cn.geektang.privacyspace.util.ConfigHelper
import cn.geektang.privacyspace.util.Su
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class LauncherViewModel(private val context: Application) : AndroidViewModel(context) {
    val rootStateFlow = MutableStateFlow(false)

    init {
        checkRootAndInitConfig()
    }

    val appListFlow = ConfigHelper.configDataFlow.map {
        it.hiddenAppList.mapToAppInfoList()
    }

    private fun Set<String>.mapToAppInfoList(): List<AppInfo> {
        val flag = PackageManager.MATCH_UNINSTALLED_PACKAGES
        return this.mapNotNull { packageName ->
            try {
                context.packageManager.getApplicationInfo(
                    packageName,
                    flag
                )
            } catch (e: Throwable) {
                null
            }
        }
            .sortedWith(ApplicationInfo.DisplayNameComparator(context.packageManager))
            .map { applicationInfo ->
                val appName = applicationInfo.loadLabel(context.packageManager).toString()
                val appIcon = applicationInfo.loadIcon(context.packageManager)
                val packageInfo = getPackageInfo(
                    context,
                    applicationInfo.packageName,
                    PackageManager.GET_META_DATA
                )
                val isXposedModule = packageInfo.applicationInfo.isXposedModule()
                AppInfo(
                    applicationInfo = applicationInfo,
                    packageName = applicationInfo.packageName,
                    appName = appName,
                    appIcon = appIcon,
                    isSystemApp = AppHelper.isSystemApp(applicationInfo),
                    isXposedModule = isXposedModule
                )
            }
    }

    fun checkRootAndInitConfig() {
        viewModelScope.launch {
            val root = Su.checkRoot()
            if (root) {
                ConfigHelper.markIsRoot()
                ConfigHelper.initConfig(context)
            }
            rootStateFlow.value = root
        }
    }

    fun onUninstallApp(appInfo: AppInfo) {
        removeAppFromHiddenList(appInfo)
    }

    fun cancelHide(appInfo: AppInfo) {
        removeAppFromHiddenList(appInfo)
    }

    private fun removeAppFromHiddenList(appInfo: AppInfo) {
        val configData = ConfigHelper.configDataFlow.value
        val hiddenAppListNew = configData.hiddenAppList.toMutableSet()
        if (hiddenAppListNew.remove(appInfo.packageName)) {
            val connectedAppsNew = configData.connectedApps.toMutableMap()
            connectedAppsNew.remove(appInfo.packageName)
            ConfigHelper.updateHiddenListAndConnectedApps(
                context = context,
                hiddenAppListNew = hiddenAppListNew,
                connectedAppsNew = connectedAppsNew
            )
        }
    }
}