package cn.geektang.privacyspace.util

import android.content.Context
import android.content.pm.PackageManager
import cn.geektang.privacyspace.BuildConfig
import cn.geektang.privacyspace.ConfigConstant
import cn.geektang.privacyspace.bean.ConfigData
import cn.geektang.privacyspace.ui.screen.launcher.getPackageInfo
import de.robv.android.xposed.XposedBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object ConfigHelper {
    const val LOADING_STATUS_INIT = 0
    const val LOADING_STATUS_LOADING = 1
    const val LOADING_STATUS_SUCCESSFUL = 2

    private val lock = Any()
    private val scope = MainScope()
    val loadingStatusFlow = MutableStateFlow(LOADING_STATUS_INIT)
    val configDataFlow = MutableStateFlow(ConfigData.EMPTY)

    @Volatile
    private var root: Boolean = false

    fun markIsRoot() {
        root = true
    }

    fun initConfig(context: Context) {
        if (!root) {
            return
        }
        scope.launch {
            loadingStatusFlow.value = LOADING_STATUS_LOADING
            var configData = loadConfig()
            if (null == configData) {
                val hiddenAppList = mutableSetOf<String>()
                val whitelist = mutableSetOf<String>()
                loadHiddenAppListConfigOld(hiddenAppList)
                loadWhitelistConfigOld(whitelist)
                if (hiddenAppList.isNotEmpty() || whitelist.isNotEmpty()) {
                    val connectedApps = mutableMapOf<String, Set<String>>()
                    for (packageName in hiddenAppList) {
                        val packageInfo = getPackageInfo(
                            context,
                            packageName,
                            PackageManager.GET_META_DATA
                        )
                        val scopeList =
                            AppHelper.getXposedModuleScopeList(context, packageInfo.applicationInfo)
                                .filter {
                                    it != ConfigConstant.ANDROID_FRAMEWORK
                                }
                        if (scopeList.isNotEmpty()) {
                            connectedApps[packageName] = scopeList.toSet()
                        }
                    }

                    removeOldConfigFiles()
                    configData = ConfigData(
                        enableLog = BuildConfig.DEBUG,
                        hiddenAppList = hiddenAppList,
                        whitelist = whitelist,
                        connectedApps = connectedApps
                    )
                    updateConfigFileInner(context, configData)
                }
            }
            if (null != configData) {
                configDataFlow.value = configData
            }
            loadingStatusFlow.value = LOADING_STATUS_SUCCESSFUL
        }
    }

    fun loadConfigWithSystemApp(): ConfigData? {
        val configFile =
            File("${ConfigConstant.CONFIG_FILE_FOLDER}${ConfigConstant.CONFIG_FILE_JSON}")
        return try {
            JsonHelper.getConfigAdapter().fromJson(configFile.readText())
        } catch (e: Throwable) {
            XposedBridge.log(e)
            null
        }
    }

    private suspend fun loadConfig(): ConfigData? {
        return withContext(Dispatchers.IO) {
            val process =
                Runtime.getRuntime()
                    .exec("su -c cat ${ConfigConstant.CONFIG_FILE_FOLDER}${ConfigConstant.CONFIG_FILE_JSON}")
            val configString = process.inputStream.use { inputStream ->
                String(inputStream.readBytes())
            }
            process.waitFor()
            return@withContext if (configString.isNotBlank()) {
                try {
                    JsonHelper.getConfigAdapter().fromJson(configString)
                } catch (e: Throwable) {
                    XposedBridge.log(e)
                    null
                }
            } else {
                null
            }
        }
    }

    private suspend fun removeOldConfigFiles() {
        withContext(Dispatchers.IO) {
            Runtime.getRuntime()
                .exec("su -c rm -f ${ConfigConstant.CONFIG_FILE_FOLDER}${ConfigConstant.CONFIG_FILE_APP_LIST}")
            Runtime.getRuntime()
                .exec("su -c rm -f ${ConfigConstant.CONFIG_FILE_FOLDER}${ConfigConstant.CONFIG_FILE_WHITELIST}")
        }
    }

    private suspend fun loadHiddenAppListConfigOld(appList: MutableSet<String>) {
        loadConfigInnerOld(
            "${ConfigConstant.CONFIG_FILE_FOLDER}${ConfigConstant.CONFIG_FILE_APP_LIST}",
            appList
        )
    }

    private suspend fun loadWhitelistConfigOld(appList: MutableSet<String>) {
        loadConfigInnerOld(
            "${ConfigConstant.CONFIG_FILE_FOLDER}${ConfigConstant.CONFIG_FILE_WHITELIST}",
            appList
        )
    }

    private suspend fun loadConfigInnerOld(
        configFilePath: String,
        appList: MutableSet<String>
    ) {
        withContext(Dispatchers.IO) {
            val process =
                Runtime.getRuntime().exec("su -c cat $configFilePath")
            val appListString = process.inputStream.use { inputStream ->
                String(inputStream.readBytes())
            }
            process.waitFor()
            if (appListString.isNotBlank()) {
                synchronized(lock) {
                    appList.clear()
                    appList.addAll(appListString.split(","))
                }
            }
        }
    }

    private suspend fun updateConfigFileInner(context: Context, configData: ConfigData) {
        withContext(Dispatchers.IO) {
            val localConfigFile =
                File(context.cacheDir, "config/${ConfigConstant.CONFIG_FILE_JSON}")
            val configFileJson = JsonHelper.getConfigAdapter().toJson(configData)
            localConfigFile.parentFile?.mkdirs()
            if (localConfigFile.exists()) {
                localConfigFile.delete()
            }
            localConfigFile.createNewFile()
            localConfigFile.writeText(configFileJson)

            Runtime.getRuntime().exec("su -c mkdir -pv ${ConfigConstant.CONFIG_FILE_FOLDER}")
                .waitFor()
            Runtime.getRuntime()
                .exec("su -c cp ${localConfigFile.absolutePath} ${ConfigConstant.CONFIG_FILE_FOLDER}")
                .waitFor()
            Runtime.getRuntime().exec("su -c chmod 604 ${ConfigConstant.CONFIG_FILE_FOLDER}*")
                .waitFor()
        }
    }

    fun updateHiddenListAndConnectedApps(
        context: Context,
        hiddenAppListNew: Set<String>,
        connectedAppsNew: Map<String, Set<String>>
    ) {
        val newConfigData = configDataFlow.value.copy(
            hiddenAppList = hiddenAppListNew,
            connectedApps = connectedAppsNew.toMutableMap()
        )
        configDataFlow.value = newConfigData
        scope.launch {
            updateConfigFileInner(context, newConfigData)
        }
    }

    fun updateWhitelist(context: Context, whitelistNew: Set<String>) {
        val newConfigData = configDataFlow.value.copy(
            whitelist = whitelistNew
        )
        configDataFlow.value = newConfigData
        scope.launch {
            updateConfigFileInner(context, newConfigData)
        }
    }

    fun updateConnectedApps(context: Context, connectedAppsNew: Map<String, Set<String>>) {
        val newConfigData = configDataFlow.value.copy(
            connectedApps = connectedAppsNew.toMutableMap()
        )
        configDataFlow.value = newConfigData
        scope.launch {
            updateConfigFileInner(context, newConfigData)
        }
    }
}