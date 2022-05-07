package cn.geektang.privacyspace.hook.impl

import android.app.ActivityManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.pm.*
import android.os.Build
import cn.geektang.privacyspace.hook.HookMain
import cn.geektang.privacyspace.hook.Hooker
import cn.geektang.privacyspace.util.ConfigHelper.getPackageName
import cn.geektang.privacyspace.util.XLog
import cn.geektang.privacyspace.util.tryLoadClass
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object SettingsAppHookImpl : Hooker, XC_MethodHook() {

    override fun start(classLoader: ClassLoader) {
        val packageManagerClass = try {
            classLoader.tryLoadClass("android.content.pm.IPackageManager\$Stub\$Proxy")
        } catch (e: ClassNotFoundException) {
            XLog.e(e, "SettingsAppHookImpl start failed.")
            return
        }
        XLog.i("Hook class packageManagerClass.")
        for (method in packageManagerClass.declaredMethods) {
            when (method.name) {
                "getInstalledPackages", "getInstalledApplications", "getInstalledModules", "queryIntentActivities" -> {
                    XLog.d("Hook method ${method.name}")
                    XposedBridge.hookMethod(method, this)
                }
                else -> {}
            }
        }

        XposedHelpers.findAndHookMethod(
            UsageStatsManager::class.java,
            "queryUsageStats",
            Int::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            this
        )

        XposedHelpers.findAndHookMethod(
            ActivityManager::class.java,
            "getRunningAppProcesses",
            this
        )
    }

    override fun afterHookedMethod(param: MethodHookParam) {
        val hiddenAppList = HookMain.configData.hiddenAppList
        when (param.method.name) {
            "getInstalledPackages" -> {
                val result = param.result as ParceledListSlice<PackageInfo>
                val iterator = result.list.iterator()
                while (iterator.hasNext()) {
                    val packageInfo = iterator.next()
                    if (hiddenAppList.contains(packageInfo.packageName)) {
                        iterator.remove()
                        XLog.i("com.android.settings was prevented from reading ${packageInfo.packageName}.")
                    }
                }
            }
            "getInstalledApplications" -> {
                val result = param.result as ParceledListSlice<ApplicationInfo>
                val iterator = result.list.iterator()
                while (iterator.hasNext()) {
                    val applicationInfo = iterator.next()
                    if (hiddenAppList.contains(applicationInfo.packageName)) {
                        iterator.remove()
                        XLog.i("com.android.settings was prevented from reading ${applicationInfo.packageName}.")
                    }
                }
            }
            "getInstalledModules" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val result = param.result as MutableList<ModuleInfo>
                    val iterator = result.iterator()
                    while (iterator.hasNext()) {
                        val applicationInfo = iterator.next()
                        if (hiddenAppList.contains(applicationInfo.packageName)) {
                            iterator.remove()
                            XLog.i("com.android.settings was prevented from reading ${applicationInfo.packageName}.")
                        }
                    }
                }
            }
            "queryIntentActivities" -> {
                val result = param.result as ParceledListSlice<ResolveInfo>
                val iterator = result.list.iterator()
                while (iterator.hasNext()) {
                    val resolveInfo = iterator.next()
                    if (hiddenAppList.contains(resolveInfo.getPackageName())) {
                        iterator.remove()
                        XLog.i("com.android.settings was prevented from reading ${resolveInfo.getPackageName()}.")
                    }
                }
            }
            "queryUsageStats" -> {
                val result = param.result as MutableList<UsageStats>
                val iterator = result.iterator()
                while (iterator.hasNext()) {
                    val usageStats = iterator.next()
                    if (hiddenAppList.contains(usageStats.packageName)) {
                        iterator.remove()
                        XLog.i("com.android.settings was prevented from reading ${usageStats.packageName}.")
                    }
                }
            }
            "getRunningAppProcesses" -> {
                val result = param.result as? List<*> ?: return
                param.result = result.filter {
                    it as ActivityManager.RunningAppProcessInfo
                    var shouldFilter = false
                    it.pkgList.forEach {
                        if (hiddenAppList.contains(it)) {
                            shouldFilter = true
                        }
                    }
                    !shouldFilter
                }
            }
            else -> {}
        }
    }
}