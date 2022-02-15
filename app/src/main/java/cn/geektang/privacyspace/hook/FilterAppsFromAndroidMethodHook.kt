package cn.geektang.privacyspace.hook

import cn.geektang.privacyspace.ConfigConstant
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

class FilterAppsFromAndroidMethodHook : XC_MethodHook() {

    override fun afterHookedMethod(param: MethodHookParam) {
        if (param.result == true) {
            return
        }
        val configData = HookMain.configData
        val shouldFilterAppList = configData.hiddenAppList
        val userWhitelist = configData.whitelist
        val connectedAppsInfoMap = configData.connectedApps
        val enableLog = configData.enableLog
        val defaultWhitelist = ConfigConstant.defaultWhitelist
        val targetPackageName = param.args.getOrNull(2)?.packageName ?: return
        val callingPackageName = param.args.getOrNull(1)?.packageName ?: return
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
                param.result = true
            } else {
                if (enableLog) {
                    XposedBridge.log("未阻止${callingPackageName}获取${targetPackageName}")
                }
            }
        }
    }

    private val Any.packageName: String
        get() = toString().substringAfterLast(" ").substringBefore("/")
}