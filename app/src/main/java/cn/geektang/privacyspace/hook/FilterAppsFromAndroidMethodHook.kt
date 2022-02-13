package cn.geektang.privacyspace.hook

import cn.geektang.privacyspace.ConfigConstant
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

class FilterAppsFromAndroidMethodHook(
    private val shouldFilterAppList: Set<String>,
    private val userWhitelist: Set<String>
) : XC_MethodHook() {

    override fun afterHookedMethod(param: MethodHookParam) {
        if (param.result == true) {
            return
        }
        val defaultWhitelist = ConfigConstant.defaultWhitelist
        val targetPackageName = param.args.getOrNull(2)?.packageName ?: return
        val callingPackageName = param.args.getOrNull(1)?.packageName ?: return
        if (callingPackageName != targetPackageName
            && !defaultWhitelist.contains(callingPackageName)
            && shouldFilterAppList.contains(targetPackageName)
        ) {
            if (!userWhitelist.contains(callingPackageName)) {
                XposedBridge.log("已阻止${callingPackageName}获取${targetPackageName}")
                param.result = true
            } else {
                XposedBridge.log("未阻止${callingPackageName}获取${targetPackageName}")
            }
        }
    }

    private val Any.packageName: String
        get() = toString().substringAfterLast(" ").substringBefore("/")
}