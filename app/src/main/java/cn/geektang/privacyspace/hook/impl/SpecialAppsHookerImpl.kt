package cn.geektang.privacyspace.hook.impl

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import cn.geektang.privacyspace.hook.HookMain
import cn.geektang.privacyspace.hook.Hooker
import cn.geektang.privacyspace.util.tryLoadClass
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object SpecialAppsHookerImpl : Hooker {
    override fun start(classLoader: ClassLoader) {
        val packageManagerClass = try {
            classLoader.tryLoadClass("android.app.ApplicationPackageManager")
        } catch (e: ClassNotFoundException) {
            XposedBridge.log(e)
            return
        }

        XposedHelpers.findAndHookMethod(
            packageManagerClass,
            "getInstalledPackages",
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    val shouldFilterAppList = HookMain.configData.hiddenAppList
                    param.result = (param.result as List<*>?)?.filter {
                        !shouldFilterAppList.contains((it as PackageInfo).packageName)
                    }
                }
            })

        XposedHelpers.findAndHookMethod(
            packageManagerClass,
            "getInstalledApplications",
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    val shouldFilterAppList = HookMain.configData.hiddenAppList
                    param.result = (param.result as List<*>?)?.filter {
                        !shouldFilterAppList.contains((it as ApplicationInfo).packageName)
                    }
                }
            })
    }
}