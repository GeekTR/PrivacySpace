package cn.geektang.privacyspace.hook.impl

import cn.geektang.privacyspace.ConfigConstant
import cn.geektang.privacyspace.hook.HookMain
import cn.geektang.privacyspace.util.XLog

object HookChecker {
    fun shouldIntercept(
        targetPackageName: String,
        callingPackageName: String
    ): Boolean {
        var result = false
        val shouldFilterAppList = HookMain.hiddenAppList
        val userWhitelist = HookMain.whitelist
        val connectedAppsInfoMap = HookMain.connectedApps
        val defaultWhitelist = ConfigConstant.defaultWhitelist

        if (callingPackageName != targetPackageName
            && !defaultWhitelist.contains(callingPackageName)
            && shouldFilterAppList.contains(targetPackageName)
        ) {
            // User's custom whitelist and 'connected apps'
            if (!userWhitelist.contains(callingPackageName)
                && connectedAppsInfoMap[callingPackageName]?.contains(targetPackageName) != true
                && connectedAppsInfoMap[targetPackageName]?.contains(callingPackageName) != true
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