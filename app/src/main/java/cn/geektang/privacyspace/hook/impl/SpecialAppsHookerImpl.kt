package cn.geektang.privacyspace.hook.impl

import android.app.ActivityManager
import android.content.pm.*
import android.os.Build
import cn.geektang.privacyspace.hook.HookMain
import cn.geektang.privacyspace.hook.Hooker
import cn.geektang.privacyspace.util.ConfigHelper.getPackageName
import cn.geektang.privacyspace.util.tryLoadClass
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object SpecialAppsHookerImpl : XC_MethodHook(), Hooker {
    override fun start(classLoader: ClassLoader) {
        val packageManagerClass = try {
            classLoader.tryLoadClass("android.app.ApplicationPackageManager")
        } catch (e: ClassNotFoundException) {
            XposedBridge.log(e)
            return
        }

        val hookMethodSet =
            setOf(
                "getInstalledPackages",
                "getInstalledApplications",
                "getInstalledModules",
                //TODO More testing is needed here
//                "queryIntentActivities",
//                "queryIntentActivityOptions",
//                "queryBroadcastReceivers",
//                "queryIntentServices",
//                "queryIntentContentProviders",
//                "resolveService",
//                "resolveActivity",
//                "resolveContentProvider",
//                "queryContentProviders",
//                "getPackageInfo"
            )
        packageManagerClass.declaredMethods.forEach {
            if (hookMethodSet.contains(it.name)) {
                XposedBridge.hookMethod(it, this)
            }
        }

        //TODO More testing is needed here
//        XposedHelpers.findAndHookMethod(
//            ActivityManager::class.java,
//            "getRunningAppProcesses",
//            object : XC_MethodHook() {
//                override fun afterHookedMethod(param: MethodHookParam) {
//                    val result = param.result as? List<*> ?: return
//                    val shouldFilterAppList = HookMain.hiddenAppList
//                    param.result = result.filter {
//                        it as ActivityManager.RunningAppProcessInfo
//                        var shouldFilter = false
//                        it.pkgList.forEach {
//                            if (shouldFilterAppList.contains(it)) {
//                                shouldFilter = true
//                            }
//                        }
//                        !shouldFilter
//                    }
//                }
//            })
    }

    override fun afterHookedMethod(param: MethodHookParam) {
        val shouldFilterAppList = HookMain.hiddenAppList
        when (param.method.name) {
            "getInstalledPackages" -> {
                param.result = (param.result as? List<*>?)?.filter {
                    val packageName = (it as? PackageInfo)?.packageName ?: return
                    !shouldFilterAppList.contains(packageName)
                }
            }
            "getInstalledApplications" -> {
                param.result = (param.result as? List<*>?)?.filter {
                    val packageName = (it as? ApplicationInfo)?.packageName ?: return
                    !shouldFilterAppList.contains(packageName)
                }
            }
            "getInstalledModules" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    param.result = (param.result as? List<*>?)?.filter {
                        val packageName = (it as? ModuleInfo)?.packageName ?: return
                        !shouldFilterAppList.contains(packageName)
                    }
                }
            }
            "queryIntentActivities", "queryIntentActivityOptions", "queryBroadcastReceivers", "queryIntentServices", "queryIntentContentProviders" -> {
                param.result = (param.result as? List<*>?)?.filter {
                    val packageName = (it as? ResolveInfo)?.getPackageName() ?: return
                    !shouldFilterAppList.contains(packageName)
                }
            }
            "resolveService", "resolveActivity" -> {
                val packageName = (param.result as? ResolveInfo)?.getPackageName()
                if (!packageName.isNullOrEmpty() && shouldFilterAppList.contains(packageName)) {
                    param.result = null
                }
            }
            "resolveContentProvider" -> {
                val packageName = (param.result as? ProviderInfo)?.packageName ?: return
                if (shouldFilterAppList.contains(packageName)) {
                    param.result = null
                }
            }
            "queryContentProviders" -> {
                param.result = (param.result as? List<*>?)?.filter {
                    val packageName = (it as? ProviderInfo)?.packageName ?: return
                    !shouldFilterAppList.contains(packageName)
                }
            }
            "getPackageInfo" -> {
                val packageName = (param.result as? PackageInfo)?.packageName ?: return
                if (shouldFilterAppList.contains(packageName)) {
                    param.throwable = PackageManager.NameNotFoundException()
                }
            }
            else -> {}
        }
    }
}