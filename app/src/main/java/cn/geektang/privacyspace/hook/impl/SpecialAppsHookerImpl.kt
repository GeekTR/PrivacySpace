package cn.geektang.privacyspace.hook.impl

import android.app.ActivityManager
import android.content.Context
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
import java.lang.reflect.Constructor

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

        val miuiRvClass = classLoader.loadClassSafe("miuix.recyclerview.widget.RecyclerView")
        if (miuiRvClass != null) {
            XposedHelpers.findAndHookConstructor(
                miuiRvClass,
                Context::class.java,
                GameBoosterHooker()
            )
        }

        val dockAppEditActivityClass =
            classLoader.loadClassSafe("com.miui.dock.edit.DockAppEditActivity")
        if (null != dockAppEditActivityClass) {
            for (method in dockAppEditActivityClass.declaredMethods) {
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
//                    XLog.d("onClick $view")
//                    view = view?.parent as? View
//                } while (view != null)
//            }
            else -> {}
        }
    }

    private class GameBoosterHooker() : XC_MethodHook() {
        private val hookCache = mutableSetOf<Class<*>>()
        private var list: MutableList<*>? = null

        override fun afterHookedMethod(param: MethodHookParam) {
            val thisClass = param.thisObject.javaClass
            if (param.method is Constructor<*>) {
                if (hookCache.contains(thisClass) || !thisClass.name.startsWith("com.miui.gamebooster.windowmanager.")) {
                    return
                }
                hookCache.add(thisClass)
                for (method in thisClass.declaredMethods) {
                    if (method.name != "setDockType"
                        && method.parameterCount == 1
                        && method.parameterTypes.first() == Int::class.javaPrimitiveType
                    ) {
                        XposedBridge.hookMethod(method, this)
                    }
                }
            } else {
                if (list == null) {
                    queryAndGetList(thisClass, param)
                }
                val listObj = list ?: return
                val iterator = listObj.iterator()
                val shouldFilterAppList = HookMain.hiddenAppList
                while (iterator.hasNext()) {
                    val appInfo = iterator.next() ?: continue
                    val appInfoClass = appInfo.javaClass

                    val shouldFilter = shouldFilter(appInfoClass, appInfo, shouldFilterAppList)
                    if (shouldFilter) {
                        iterator.remove()
                    }
                }
            }
        }

        private fun shouldFilter(
            appInfoClass: Class<Any>,
            appInfo: Any,
            shouldFilterAppList: Set<String>,
        ): Boolean {
            for (declaredField in appInfoClass.declaredFields) {
                declaredField.isAccessible = true
                val packageName = declaredField.get(appInfo)?.getPackageName() ?: continue
                if (packageName.isNotBlank() && shouldFilterAppList.contains(packageName)) {
                    return true
                }
            }
            return false
        }

        private fun queryAndGetList(thisClass: Class<Any>, param: MethodHookParam) {
            for (declaredField in thisClass.declaredFields) {
                declaredField.isAccessible = true
                val value = declaredField.get(param.thisObject)
                list = value as? MutableList<*>
                        ?: continue
                break
            }
        }

        private fun Any?.getPackageName(): String? {
            return toString().split(",").getOrNull(1)
                ?.substringAfter("'")?.substringBefore("'")
        }
    }
}