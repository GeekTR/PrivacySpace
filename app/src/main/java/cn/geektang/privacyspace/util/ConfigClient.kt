package cn.geektang.privacyspace.util

import android.content.Context
import android.util.Log
import cn.geektang.privacyspace.bean.ConfigData
import cn.geektang.privacyspace.bean.SystemUserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ConfigClient(context: Context) {
    private val packageManager = context.packageManager

    fun serverVersion(): Int {
        return connectServer(ConfigServer.QUERY_SERVER_VERSION)?.toIntOrNull() ?: -1
    }

    fun rebootTheSystem() {
        connectServer(ConfigServer.REBOOT_THE_SYSTEM)
    }

    suspend fun migrateOldConfig() {
        withContext(Dispatchers.IO) {
            connectServer(ConfigServer.MIGRATE_OLD_CONFIG_FILE)
        }
    }

    suspend fun queryConfig(): ConfigData? {
        return withContext(Dispatchers.IO) {
            val configJson = connectServer(ConfigServer.QUERY_CONFIG)
            if (configJson.isNullOrBlank()) {
                return@withContext null
            }
            return@withContext try {
                JsonHelper.configAdapter().fromJson(configJson)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("PrivacySpace", "Config is invalid.")
                null
            }
        }
    }

    suspend fun updateConfig(configData: ConfigData) {
        withContext(Dispatchers.IO) {
            val configJson = JsonHelper.configAdapter().toJson(configData)
            connectServer("${ConfigServer.UPDATE_CONFIG}$configJson")
        }
    }

    suspend fun querySystemUserList(): List<SystemUserInfo>? {
        return withContext(Dispatchers.IO) {
            val userListJson = connectServer(ConfigServer.GET_USERS)
            try {
                JsonHelper.systemUserInfoListAdapter().fromJson(userListJson)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun connectServer(methodName: String): String? {
        return try {
            packageManager.getInstallerPackageName(methodName)
        } catch (e: Exception) {
            null
        }
    }
}