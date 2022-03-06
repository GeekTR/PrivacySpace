package cn.geektang.privacyspace.hook.impl

import cn.geektang.privacyspace.hook.Hooker
import cn.geektang.privacyspace.util.XLog
import cn.geektang.privacyspace.util.tryLoadClass
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object FrameworkHookerApi30Impl : XC_MethodHook(), Hooker {
    override fun start(classLoader: ClassLoader) {
        val appsFilerClass: Class<*>
        val settingBaseClass: Class<*>
        val packageSettingClass: Class<*>
        try {
            appsFilerClass = classLoader.tryLoadClass("com.android.server.pm.AppsFilter")
            settingBaseClass = classLoader.tryLoadClass("com.android.server.pm.SettingBase")
            packageSettingClass =
                classLoader.tryLoadClass("com.android.server.pm.PackageSetting")
        } catch (e: ClassNotFoundException) {
            XLog.e(e, "FrameworkHookerApi30Impl start failed.")
            return
        }

        XposedHelpers.findAndHookMethod(
            appsFilerClass,
            "shouldFilterApplication",
            Int::class.javaPrimitiveType,
            settingBaseClass,
            packageSettingClass,
            Int::class.javaPrimitiveType,
            this
        )
    }

    override fun afterHookedMethod(param: MethodHookParam) {
        if (param.result == true) {
            return
        }
        val targetPackageName = param.args.getOrNull(2)?.packageName ?: return
        val callingPackageName = param.args.getOrNull(1)?.packageName ?: return
        val shouldIntercept = HookChecker.shouldIntercept(
            targetPackageName = targetPackageName,
            callingPackageName = callingPackageName
        )
        if (shouldIntercept) {
            param.result = true
        }
    }
}