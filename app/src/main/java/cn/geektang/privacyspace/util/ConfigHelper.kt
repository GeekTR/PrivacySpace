package cn.geektang.privacyspace.util

import android.content.Context
import android.content.pm.ResolveInfo
import cn.geektang.privacyspace.bean.ConfigData
import cn.geektang.privacyspace.constant.ConfigConstant
import cn.geektang.privacyspace.util.AppHelper.getApkInstallerPackageName
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
    const val LOADING_STATUS_FAILED = 3

    private lateinit var configClient: ConfigClient
    private val scope = MainScope()
    val loadingStatusFlow = MutableStateFlow(LOADING_STATUS_INIT)
    val configDataFlow = MutableStateFlow(ConfigData.EMPTY)

    @Volatile
    private var isServerStart: Boolean = false

    fun initConfig(context: Context) {
        if (!::configClient.isInitialized) {
            configClient = ConfigClient(context.applicationContext)
            AppHelper.startWatchingAppsCountChange(context, onAppRemoved = {packageName ->
                removeConfigForApp(packageName)
            })
        }
        val serverVersion = configClient.serverVersion()
        isServerStart = serverVersion > 0
        if (!isServerStart) {
            loadingStatusFlow.value = LOADING_STATUS_FAILED
            return
        }
        scope.launch {
            val installerPackageName = context.getApkInstallerPackageName()
            loadingStatusFlow.value = LOADING_STATUS_LOADING
            var configData = configClient.queryConfig()
            if (null == configData) {
                configClient.migrateOldConfig()
                configData = configClient.queryConfig()
            }
            configData = configData ?: configDataFlow.value
            if (!installerPackageName.isNullOrEmpty()
                && !configData.whitelist.contains(installerPackageName)
            ) {
                val whitelistNew = configData.whitelist.toMutableSet()
                whitelistNew.add(installerPackageName)
                updateWhitelist(
                    whitelistNew = whitelistNew,
                    sharedUserIdMapNew = configData.sharedUserIdMap ?: emptyMap()
                )
            }
            configDataFlow.value = configData
            loadingStatusFlow.value = LOADING_STATUS_SUCCESSFUL
        }
    }

    private fun removeConfigForApp(packageName: String) {
        // switch to ui thread
        scope.launch {
            val configData = configDataFlow.value
            if (configData == ConfigData.EMPTY) {
                return@launch
            }
            val hiddenAppListNew = configData.hiddenAppList.toMutableSet()
            val connectedAppsNew = configData.connectedApps.toMutableMap()
            if (hiddenAppListNew.contains(packageName)) {
                hiddenAppListNew.remove(packageName)
                connectedAppsNew.remove(packageName)
                updateHiddenListAndConnectedApps(hiddenAppListNew, connectedAppsNew)
            }
        }
    }

    fun loadConfigWithSystemApp(): ConfigData? {
        val configFile =
            File("${ConfigConstant.CONFIG_FILE_FOLDER}${ConfigConstant.CONFIG_FILE_JSON}")
        return try {
            JsonHelper.getConfigAdapter().fromJson(configFile.readText())
        } catch (e: Throwable) {
            XLog.e(e, "ConfigHelper loadConfigWithSystemApp failed.")
            null
        }
    }

    private suspend fun updateConfigFileInner(configData: ConfigData) {
        withContext(Dispatchers.IO) {
            configClient.updateConfig(configData)
        }
    }

    fun updateHiddenListAndConnectedApps(
        hiddenAppListNew: Set<String>,
        connectedAppsNew: Map<String, Set<String>>,
        sharedUserIdMapNew: Map<String, String>? = null
    ) {
        val newConfigData = configDataFlow.value.copy(
            hiddenAppList = hiddenAppListNew.toMutableSet(),
            connectedApps = connectedAppsNew.toMutableMap(),
            sharedUserIdMap = sharedUserIdMapNew?.toMutableMap()
                ?: configDataFlow.value.sharedUserIdMap
        )
        configDataFlow.value = newConfigData
        scope.launch {
            updateConfigFileInner(newConfigData)
        }
    }

    fun updateWhitelist(whitelistNew: Set<String>, sharedUserIdMapNew: Map<String, String>) {
        val newConfigData = configDataFlow.value.copy(
            whitelist = whitelistNew.toMutableSet(),
            sharedUserIdMap = sharedUserIdMapNew.toMutableMap()
        )
        configDataFlow.value = newConfigData
        scope.launch {
            updateConfigFileInner(newConfigData)
        }
    }

    fun updateConnectedApps(
        connectedAppsNew: Map<String, Set<String>>,
        sharedUserIdMapNew: Map<String, String>
    ) {
        val newConfigData = configDataFlow.value.copy(
            connectedApps = connectedAppsNew.toMutableMap(),
            sharedUserIdMap = sharedUserIdMapNew.toMutableMap()
        )
        configDataFlow.value = newConfigData
        scope.launch {
            updateConfigFileInner(newConfigData)
        }
    }

    fun rebootTheSystem() {
        configClient.rebootTheSystem()
    }

    fun ResolveInfo.getPackageName(): String? {
        var packageName = activityInfo?.packageName
        if (packageName.isNullOrEmpty()) {
            packageName = providerInfo?.packageName
        }
        if (packageName.isNullOrEmpty()) {
            packageName = serviceInfo?.packageName
        }
        if (packageName.isNullOrEmpty()) {
            packageName = resolvePackageName
        }
        return packageName
    }
}