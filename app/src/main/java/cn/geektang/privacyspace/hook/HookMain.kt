package cn.geektang.privacyspace.hook

import android.os.Build
import android.os.FileObserver
import cn.geektang.privacyspace.ConfigConstant
import cn.geektang.privacyspace.bean.ConfigData
import cn.geektang.privacyspace.hook.impl.FrameworkHookerApi26Impl
import cn.geektang.privacyspace.hook.impl.FrameworkHookerApi28Impl
import cn.geektang.privacyspace.hook.impl.FrameworkHookerApi30Impl
import cn.geektang.privacyspace.hook.impl.SpecialAppsHookerImpl
import cn.geektang.privacyspace.util.ConfigHelper
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

class HookMain : IXposedHookLoadPackage {

    companion object {
        private lateinit var classLoader: ClassLoader
        private var packageName: String? = null
        private var fileObserver: FileObserver? = null

        @Volatile
        var configData: ConfigData = ConfigData.EMPTY
            private set
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        packageName = lpparam.packageName
        val specialHookApps = ConfigConstant.specialHookApps
        if (lpparam.packageName == ConfigConstant.ANDROID_FRAMEWORK) {
            classLoader = lpparam.classLoader

            configData = ConfigHelper.loadConfigWithSystemApp() ?: ConfigData.EMPTY
            startWatchingConfigFiles()
            when {
                Build.VERSION.SDK_INT >= 30 -> {
                    FrameworkHookerApi30Impl.start(classLoader)
                }
                Build.VERSION.SDK_INT >= 28 -> {
                    FrameworkHookerApi28Impl.start(classLoader)
                }
                else -> {
                    FrameworkHookerApi26Impl.start(classLoader)
                }
            }
        } else if (specialHookApps.contains(lpparam.packageName)) {
            classLoader = lpparam.classLoader
            configData = ConfigHelper.loadConfigWithSystemApp() ?: ConfigData.EMPTY
            startWatchingConfigFiles()
            SpecialAppsHookerImpl.start(classLoader)
        }
    }

    private fun startWatchingConfigFiles() {
        if (null == fileObserver) {
            fileObserver =
                object : FileObserver(ConfigConstant.CONFIG_FILE_FOLDER) {
                    override fun onEvent(event: Int, path: String?) {
                        if (event == CLOSE_WRITE && path == ConfigConstant.CONFIG_FILE_JSON) {
                            val newConfigData =
                                ConfigHelper.loadConfigWithSystemApp() ?: ConfigData.EMPTY
                            if (newConfigData != configData) {
                                configData = newConfigData
                                if (configData.enableLog) {
                                    XposedBridge.log("$packageName 重载config.json")
                                }
                            }
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