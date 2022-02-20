package cn.geektang.privacyspace.hook.impl

import cn.geektang.privacyspace.ConfigConstant
import cn.geektang.privacyspace.hook.HookMain
import de.robv.android.xposed.XposedBridge

object HookChecker {

    fun shouldIntercept(
        targetPackageName: String,
        callingPackageName: String
    ): Boolean {
        var result = false
        val configData = HookMain.configData
        val shouldFilterAppList = configData.hiddenAppList
        val userWhitelist = configData.whitelist
        val connectedAppsInfoMap = configData.connectedApps
        val enableLog = configData.enableLog
        val defaultWhitelist = ConfigConstant.defaultWhitelist
        if (callingPackageName != targetPackageName
            && !defaultWhitelist.contains(callingPackageName)
            && shouldFilterAppList.contains(targetPackageName)
        ) {
            if (!userWhitelist.contains(callingPackageName)
                && connectedAppsInfoMap[callingPackageName]?.contains(targetPackageName) != true
                && connectedAppsInfoMap[targetPackageName]?.contains(callingPackageName) != true
            ) {
                if (enableLog) {
                    XposedBridge.log("已阻止${callingPackageName}获取${targetPackageName}")
                }
                result = true
            } else {
                if (enableLog) {
                    XposedBridge.log("未阻止${callingPackageName}获取${targetPackageName}")
                }
            }
        }
        return result
    }
}