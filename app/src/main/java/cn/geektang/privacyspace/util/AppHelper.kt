package cn.geektang.privacyspace.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import cn.geektang.privacyspace.bean.AppInfo
import cn.geektang.privacyspace.ui.screen.launcher.getPackageInfo
import cn.geektang.privacyspace.ui.screen.launcher.isXposedModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


object AppHelper {
    suspend fun Context.loadAllAppList(): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            val flag =
                PackageManager.MATCH_UNINSTALLED_PACKAGES
            val packageManager = packageManager
            return@withContext packageManager
                .getInstalledApplications(flag)
                .mapNotNull { applicationInfo ->
                    try {
                        val appName = applicationInfo.loadLabel(packageManager).toString()
                        val appIcon = applicationInfo.loadIcon(packageManager)
                        val packageInfo = getPackageInfo(
                            this@loadAllAppList,
                            applicationInfo.packageName,
                            PackageManager.GET_META_DATA
                        )
                        AppInfo(
                            applicationInfo = applicationInfo,
                            packageName = applicationInfo.packageName,
                            appName = appName,
                            appIcon = appIcon,
                            isSystemApp = isSystemApp(applicationInfo),
                            isXposedModule = packageInfo.applicationInfo.isXposedModule()
                        )
                    } catch (ignored: PackageManager.NameNotFoundException) {
                        null
                    }
                }
        }
    }

    suspend fun List<AppInfo>.sortApps(
        context: Context,
        toTopCollections: Collection<String>
    ): List<AppInfo> {
        return withContext(Dispatchers.Default) {
            val applicationInfoComparator =
                ApplicationInfo.DisplayNameComparator(context.packageManager)
            return@withContext sortedWith(Comparator { t, t2 ->
                val isContainsT = toTopCollections.contains(t.packageName)
                val isContainsT2 = toTopCollections.contains(t2.packageName)
                if (isContainsT && !isContainsT2) {
                    return@Comparator -1
                } else if (!isContainsT && isContainsT2) {
                    return@Comparator 1
                }
                if (t.isXposedModule && !t2.isXposedModule) {
                    return@Comparator -1
                } else if (!t.isXposedModule && t2.isXposedModule) {
                    return@Comparator 1
                }
                return@Comparator applicationInfoComparator.compare(
                    t.applicationInfo,
                    t2.applicationInfo
                )
            })
        }
    }

    fun isSystemApp(applicationInfo: ApplicationInfo): Boolean {
        return (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM
    }

    fun Context.getLauncherPackageName(): String? {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val res = packageManager.resolveActivity(intent, 0)
        if (res?.activityInfo == null) {
            return null
        }
        return if (res.activityInfo.packageName == "android") {
            null
        } else {
            res.activityInfo.packageName
        }
    }

    fun getXposedModuleScopeList(context: Context, app: ApplicationInfo): List<String> {
        val pm = context.packageManager
        val scopeList = mutableListOf<String>()
        try {
            val scopeListResourceId: Int = app.metaData.getInt("xposedscope")
            if (scopeListResourceId != 0) {
                scopeList.addAll(
                    pm.getResourcesForApplication(app).getStringArray(scopeListResourceId)
                )
            } else {
                val scopeListString: String? = app.metaData.getString("xposedscope")
                if (scopeListString != null) {
                    scopeList.addAll(scopeListString.split(";"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return scopeList
    }
}