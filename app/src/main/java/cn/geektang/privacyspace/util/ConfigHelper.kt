package cn.geektang.privacyspace.util

import android.content.Context
import cn.geektang.privacyspace.ConfigConstant
import de.robv.android.xposed.XposedBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.concurrent.thread

object ConfigHelper {
    private val lock = Any()

    fun loadAppListConfigWithSystemApp(appList: MutableSet<String>) {
        loadConfigWithSystemAppInner(appList, ConfigConstant.CONFIG_FILE_APP_LIST)
    }

    fun loadWhitelistConfigWithSystemApp(appList: MutableSet<String>) {
        loadConfigWithSystemAppInner(appList, ConfigConstant.CONFIG_FILE_WHITELIST)
    }

    private fun loadConfigWithSystemAppInner(appList: MutableSet<String>, fileName: String) {
        synchronized(lock) {
            try {
                appList.clear()
                val apps = File("${ConfigConstant.CONFIG_FILE_FOLDER}${fileName}")
                appList.addAll(apps.readText().split(","))
            } catch (e: Throwable) {
                XposedBridge.log(e)
            }
        }
    }

    suspend fun loadAppListConfig(appList: MutableSet<String>) {
        loadConfigInner("${ConfigConstant.CONFIG_FILE_FOLDER}${ConfigConstant.CONFIG_FILE_APP_LIST}", appList)
    }

    suspend fun loadWhitelistConfig(appList: MutableSet<String>) {
        loadConfigInner("${ConfigConstant.CONFIG_FILE_FOLDER}${ConfigConstant.CONFIG_FILE_WHITELIST}", appList)
    }

    private suspend fun loadConfigInner(
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

    fun updateAppListConfig(context: Context, hiddenAppList: Set<String>) {
        updateConfigInner(context, "config/${ConfigConstant.CONFIG_FILE_APP_LIST}", hiddenAppList)
    }

    fun updateWhitelistConfig(context: Context, whitelist: Set<String>) {
        updateConfigInner(context, "config/${ConfigConstant.CONFIG_FILE_WHITELIST}", whitelist)
    }

    private fun updateConfigInner(context: Context, localFilePath: String, appList: Set<String>) {
        thread {
            val localConfigFile = File(context.cacheDir, localFilePath)
            val appListStringBuilder = StringBuilder()
            appList.forEachIndexed { index, packageName ->
                appListStringBuilder.append(packageName)
                if (index != appList.size - 1) {
                    appListStringBuilder.append(",")
                }
            }
            localConfigFile.parentFile?.mkdirs()
            if (localConfigFile.exists()) {
                localConfigFile.delete()
            }
            localConfigFile.createNewFile()
            localConfigFile.writeText(appListStringBuilder.toString())

            Runtime.getRuntime().exec("su -c mkdir -pv ${ConfigConstant.CONFIG_FILE_FOLDER}").waitFor()
            Runtime.getRuntime()
                .exec("su -c cp ${localConfigFile.absolutePath} ${ConfigConstant.CONFIG_FILE_FOLDER}")
                .waitFor()
            Runtime.getRuntime().exec("su -c chmod 604 ${ConfigConstant.CONFIG_FILE_FOLDER}*").waitFor()
        }
    }
}