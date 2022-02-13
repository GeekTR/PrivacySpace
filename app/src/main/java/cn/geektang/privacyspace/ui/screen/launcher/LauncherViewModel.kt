package cn.geektang.privacyspace.ui.screen.launcher

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.geektang.privacyspace.bean.AppInfo
import cn.geektang.privacyspace.util.AppHelper
import cn.geektang.privacyspace.util.ConfigHelper
import cn.geektang.privacyspace.util.setDifferentValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LauncherViewModel(private val context: Application) : AndroidViewModel(context) {

    val appListFlow = MutableStateFlow(emptyList<AppInfo>())

    fun refreshAppList() {
        viewModelScope.launch {
            val packageNameList = mutableSetOf<String>()
            ConfigHelper.loadAppListConfig(packageNameList)
            val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PackageManager.MATCH_UNINSTALLED_PACKAGES
            } else {
                PackageManager.GET_UNINSTALLED_PACKAGES
            }
            val appList = packageNameList.mapNotNull { packageName ->
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
                    AppInfo(
                        applicationInfo = applicationInfo,
                        packageName = applicationInfo.packageName,
                        appName = appName,
                        appIcon = appIcon,
                        isSystemApp = AppHelper.isSystemApp(applicationInfo),
                        isXposedModule = packageInfo.applicationInfo.isXposedModule()
                    )
                }
            appListFlow.setDifferentValue(appList)
        }
    }
}