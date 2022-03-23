package cn.geektang.privacyspace.hook.impl

import cn.geektang.privacyspace.constant.ConfigConstant
import cn.geektang.privacyspace.hook.HookMain
import cn.geektang.privacyspace.util.XLog

object HookChecker {
    fun shouldIntercept(
        userId: Int,
        targetPackageName: String,
        callingPackageName: String
    ): Boolean {
        if (callingPackageName == targetPackageName) {
            return false
        }

        var result = false
        val shouldFilterAppList = HookMain.hiddenAppList
        val userWhitelist = HookMain.whitelist
        val connectedAppsInfoMap = HookMain.connectedApps
        val multiUserConfig = HookMain.multiUserConfig
        val blindApps = HookMain.blind
        val defaultWhitelist = ConfigConstant.defaultWhitelist
        val defaultBlindWhitelist = ConfigConstant.defaultBlindWhitelist

        if (!defaultBlindWhitelist.contains(targetPackageName)
            && blindApps.contains(callingPackageName)
        ) {
            XLog.i("$callingPackageName was prevented from reading ${targetPackageName}.")
            return true
        }

        if (!defaultWhitelist.contains(callingPackageName)
            && shouldFilterAppList.contains(targetPackageName)
        ) {
            val appMultiUserConfig = multiUserConfig[targetPackageName]
            // User's custom whitelist and 'connected apps'
            if (!userWhitelist.contains(callingPackageName)
                && connectedAppsInfoMap[callingPackageName]?.contains(targetPackageName) != true
                && connectedAppsInfoMap[targetPackageName]?.contains(callingPackageName) != true
                && (appMultiUserConfig.isNullOrEmpty() || appMultiUserConfig.contains(userId))
            ) {
                XLog.d("$callingPackageName was prevented from reading ${targetPackageName}.")
                result = true
            } else {
                XLog.d("$callingPackageName read ${targetPackageName}.")
            }
        }
        return result
    }
}