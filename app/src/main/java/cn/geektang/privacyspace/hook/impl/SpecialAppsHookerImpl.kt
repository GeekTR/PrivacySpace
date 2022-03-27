package cn.geektang.privacyspace.hook.impl

import android.app.ActivityManager
import android.content.pm.*
import android.os.Build
import cn.geektang.privacyspace.hook.HookMain
import cn.geektang.privacyspace.hook.Hooker
import cn.geektang.privacyspace.util.ConfigHelper.getPackageName
import cn.geektang.privacyspace.util.XLog
import cn.geektang.privacyspace.util.loadClassSafe
import cn.geektang.privacyspace.util.tryLoadClass
import com.android.internal.os.BatterySipper
import com.android.internal.os.BatteryStatsHelper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object SpecialAppsHookerImpl : XC_MethodHook(), Hooker {
    override fun start(classLoader: ClassLoader) {
        val packageManagerClass = try {
            classLoader.tryLoadClass("android.app.ApplicationPackageManager")
        } catch (e: ClassNotFoundException) {
            XLog.e(e, "SpecialAppsHookerImpl start failed.")
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

//        XposedHelpers.findAndHookMethod(
//            View::class.java,
//            "setOnClickListener",
//            View.OnClickListener::class.java,
//            object : XC_MethodHook() {
//                private val cache = mutableSetOf<Class<*>>()
//
//                override fun afterHookedMethod(param: MethodHookParam) {
//                    val listener = param.args.first()
//                    val clazz = listener.javaClass
//                    if (!cache.contains(clazz)) {
//                        cache.add(clazz)
//                        val method =
//                            listener.javaClass.getDeclaredMethod("onClick", View::class.java)
//                        method.isAccessible = true
//                        XposedBridge.hookMethod(method, this@SpecialAppsHookerImpl)
//                    }
//                }
//            }
//        )

        XposedHelpers.findAndHookMethod(
            BatteryStatsHelper::class.java,
            "getUsageList",
            this
        )
        XposedHelpers.findAndHookMethod(
            BatteryStatsHelper::class.java,
            "getMobilemsppList",
            this
        )

        XposedHelpers.findAndHookMethod(
            ActivityManager::class.java,
            "getRunningAppProcesses",
            this
        )

        val clazz = classLoader.loadClassSafe("com.miui.dock.edit.DockAppEditActivity") ?: return
        for (method in clazz.declaredMethods) {
            if (method.parameterCount == 1 && method.parameterTypes.first() == PackageInfo::class.java) {
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val shouldFilterAppList = HookMain.hiddenAppList
                        val packageInfo = param.args.first() as PackageInfo
                        if (shouldFilterAppList.contains(packageInfo.packageName)) {
                            param.result = Unit
                        }
                    }
                })
            }
        }
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
            "getUsageList", "getMobilemsppList" -> {
                val result = param.result as? MutableList<*> ?: return
                val iterator = result.iterator()
                while (iterator.hasNext()) {
                    val batterySipper = (iterator.next() as? BatterySipper) ?: continue
                    val packages = batterySipper.packages
                    if (packages.isNullOrEmpty()) {
                        continue
                    }
                    for (packageName in packages) {
                        if (shouldFilterAppList.contains(packageName)) {
                            iterator.remove()
                            break
                        }
                    }
                }
            }
            "getRunningAppProcesses" -> {
                val result = param.result as? List<*> ?: return
                param.result = result.filter {
                    it as ActivityManager.RunningAppProcessInfo
                    var shouldFilter = false
                    it.pkgList.forEach {
                        if (shouldFilterAppList.contains(it)) {
                            shouldFilter = true
                        }
                    }
                    !shouldFilter
                }
            }
//            "onClick" -> {
//                var view = param.args.first() as View?
//                XLog.d("onClick ${view?.context}")
//                do {
////                    XLog.d("onClick $view")
//                    view = view?.parent as? View
//                } while (view != null)
//
////                for (stackTraceElement in Thread.currentThread().stackTrace) {
////                    XLog.d(stackTraceElement.toString())
////                }
////                XLog.d("onClick end ***********")
//            }
            else -> {}
        }
    }
}