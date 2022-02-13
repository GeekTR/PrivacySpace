package cn.geektang.privacyspace.hook

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.*
import cn.geektang.privacyspace.ConfigConstant
import cn.geektang.privacyspace.util.ConfigHelper
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File
import java.util.concurrent.CopyOnWriteArraySet

class HookMain : IXposedHookLoadPackage {

    companion object {
        private lateinit var classLoader: ClassLoader
        private var packageName: String? = null
        private val shouldFilterAppList = CopyOnWriteArraySet<String>()
        private val whitelist = CopyOnWriteArraySet<String>()
        private var fileObserver: FileObserver? = null
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        packageName = lpparam.packageName
        val specialHookApps = ConfigConstant.specialHookApps
        if (lpparam.packageName == ConfigConstant.ANDROID_FRAMEWORK) {
            classLoader = lpparam.classLoader
            hookAndroidSystem(lpparam)
        } else if (specialHookApps.contains(lpparam.packageName)) {
            classLoader = lpparam.classLoader
            hookSpecialApps(lpparam)
        }
    }

    private fun hookSpecialApps(lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageManagerClass = try {
            classLoader.tryLoadClass("android.app.ApplicationPackageManager")
        } catch (e: ClassNotFoundException) {
            XposedBridge.log(e)
            return
        }

        ConfigHelper.loadAppListConfigWithSystemApp(shouldFilterAppList)
        startWatchingConfigFiles()
        XposedHelpers.findAndHookMethod(
            packageManagerClass,
            "getInstalledPackages",
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
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
                    param.result = (param.result as List<*>?)?.filter {
                        !shouldFilterAppList.contains((it as ApplicationInfo).packageName)
                    }
                }
            })
    }

    private fun hookAndroidSystem(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classLoader = classLoader
        val appsFilerClass: Class<*>
        val settingBaseClass: Class<*>
        val packageSettingClass: Class<*>
        try {
            appsFilerClass = classLoader.tryLoadClass("com.android.server.pm.AppsFilter")
            settingBaseClass = classLoader.tryLoadClass("com.android.server.pm.SettingBase")
            packageSettingClass =
                classLoader.tryLoadClass("com.android.server.pm.PackageSetting")
        } catch (e: ClassNotFoundException) {
            XposedBridge.log(e)
            return
        }

        ConfigHelper.loadAppListConfigWithSystemApp(shouldFilterAppList)
        ConfigHelper.loadWhitelistConfigWithSystemApp(whitelist)
        startWatchingConfigFiles()
        XposedHelpers.findAndHookMethod(
            appsFilerClass,
            "shouldFilterApplication",
            Int::class.javaPrimitiveType,
            settingBaseClass,
            packageSettingClass,
            Int::class.javaPrimitiveType,
            FilterAppsFromAndroidMethodHook(shouldFilterAppList, whitelist)
        )
    }

    private fun startWatchingConfigFiles() {
        if (null == fileObserver) {
            fileObserver =
                object : FileObserver(File(ConfigConstant.CONFIG_FILE_FOLDER)) {
                    override fun onEvent(event: Int, path: String?) {
                        if (event == CLOSE_WRITE && path == ConfigConstant.CONFIG_FILE_APP_LIST) {
                            ConfigHelper.loadAppListConfigWithSystemApp(shouldFilterAppList)
                            XposedBridge.log("$packageName 重载filterAppList")
                        } else if (packageName == ConfigConstant.ANDROID_FRAMEWORK && event == CLOSE_WRITE && path == ConfigConstant.CONFIG_FILE_WHITELIST) {
                            ConfigHelper.loadWhitelistConfigWithSystemApp(whitelist)
                            XposedBridge.log("$packageName 重载whitelist")
                        }
                    }
                }
            fileObserver?.startWatching()
        }
    }

    @Throws(ClassNotFoundException::class)
    private fun ClassLoader.tryLoadClass(name: String): Class<*> {
        return loadClass(name) ?: throw ClassNotFoundException()
    }
}