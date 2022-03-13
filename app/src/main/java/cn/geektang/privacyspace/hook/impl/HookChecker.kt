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
        var result = false
        val shouldFilterAppList = HookMain.hiddenAppList
        val userWhitelist = HookMain.whitelist
        val connectedAppsInfoMap = HookMain.connectedApps
        val multiUserConfig = HookMain.multiUserConfig
        val defaultWhitelist = ConfigConstant.defaultWhitelist

        if (callingPackageName != targetPackageName
            && !defaultWhitelist.contains(callingPackageName)
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