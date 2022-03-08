package cn.geektang.privacyspace.hook

import android.os.Build
import android.os.FileObserver
import cn.geektang.privacyspace.constant.ConfigConstant
import cn.geektang.privacyspace.bean.ConfigData
import cn.geektang.privacyspace.hook.impl.FrameworkHookerApi26Impl
import cn.geektang.privacyspace.hook.impl.FrameworkHookerApi28Impl
import cn.geektang.privacyspace.hook.impl.FrameworkHookerApi30Impl
import cn.geektang.privacyspace.hook.impl.SpecialAppsHookerImpl
import cn.geektang.privacyspace.util.AppHelper
import cn.geektang.privacyspace.util.ConfigHelper
import cn.geektang.privacyspace.util.ConfigServer
import cn.geektang.privacyspace.util.XLog
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File

class HookMain : IXposedHookLoadPackage {

    companion object {
        private lateinit var classLoader: ClassLoader
        private var packageName: String? = null
        private var fileObserver: FileObserver? = null
        private val configServer = ConfigServer()

        @Volatile
        private var configData: ConfigData = ConfigData.EMPTY

        @Volatile
        var enableLog: Boolean = false
            private set

        @Volatile
        var hiddenAppList: Set<String> = emptySet()
            private set

        @Volatile
        var whitelist: Set<String> = emptySet()
            private set

        @Volatile
        var connectedApps: Map<String, Set<String>> = emptyMap()
            private set

        fun updateConfigData(data: ConfigData) {
            enableLog = data.enableDetailLog
            hiddenAppList = data.hiddenAppList
            val sharedUserIdMap = data.sharedUserIdMap
            val whitelistTmp = data.whitelist.toMutableSet()
            val connectedAppsTpm = data.connectedApps.toMutableMap()
            if (!sharedUserIdMap.isNullOrEmpty()) {
                sharedUserIdMap.forEach {
                    if (whitelistTmp.contains(it.key)) {
                        whitelistTmp.add(it.value)
                    }
                }

                val connectedAppsNew = mutableMapOf<String, Set<String>>()
                connectedAppsTpm.forEach {
                    val newSet = it.value.toMutableSet()
                    it.value.forEach { packageName ->
                        val sharedUserId = sharedUserIdMap[packageName]
                        if (!sharedUserId.isNullOrEmpty()) {
                            newSet.add(sharedUserId)
                        }
                    }
                    connectedAppsNew[it.key] = newSet

                    val keySharedUserId = sharedUserIdMap[it.key]
                    if (!keySharedUserId.isNullOrEmpty()) {
                        connectedAppsNew[keySharedUserId] = newSet
                    }
                }
                connectedAppsTpm.putAll(connectedAppsNew)
            }
            whitelist = whitelistTmp
            connectedApps = connectedAppsTpm
        }
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        packageName = lpparam.packageName
        classLoader = lpparam.classLoader
        if (lpparam.packageName == ConfigConstant.ANDROID_FRAMEWORK) {
            loadConfigDataAndParse()
            configServer.start(classLoader = classLoader)
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
        } else if (AppHelper.isSystemApp(lpparam.appInfo)) {
            loadConfigDataAndParse()
            startWatchingConfigFiles()
            SpecialAppsHookerImpl.start(classLoader)
        }
    }

    private fun startWatchingConfigFiles() {
        if (null == fileObserver) {
            val file = File(ConfigConstant.CONFIG_FILE_FOLDER)
            file.mkdirs()
            fileObserver =
                object : FileObserver(ConfigConstant.CONFIG_FILE_FOLDER) {
                    override fun onEvent(event: Int, path: String?) {
                        if (event == CLOSE_WRITE && path == ConfigConstant.CONFIG_FILE_JSON) {
                            loadConfigDataAndParse()
                            XLog.i("$packageName reload config.json")
                        }
                    }
                }
            fileObserver?.startWatching()
        }
    }

    private fun loadConfigDataAndParse() {
        val configDataNew = ConfigHelper.loadConfigWithSystemApp() ?: ConfigData.EMPTY
        if (configDataNew != configData) {
            configData = configDataNew
            updateConfigData(configDataNew)
        }
    }
}