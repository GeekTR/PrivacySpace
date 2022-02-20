package cn.geektang.privacyspace.hook.impl

import cn.geektang.privacyspace.hook.Hooker
import cn.geektang.privacyspace.util.tryLoadClass
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Method

object FrameworkHookerApi26Impl : XC_MethodHook(), Hooker {
    private lateinit var pmsClass: Class<*>
    private lateinit var packageSettingClass: Class<*>
    private lateinit var getPackageNameForUidMethod: Method

    override fun start(classLoader: ClassLoader) {
        try {
            pmsClass =
                classLoader.tryLoadClass("com.android.server.pm.PackageManagerService")
            packageSettingClass =
                classLoader.tryLoadClass("com.android.server.pm.PackageSetting")
            getPackageNameForUidMethod = pmsClass.getDeclaredMethod(
                "getNameForUid",
                Int::class.javaPrimitiveType
            )
            getPackageNameForUidMethod.isAccessible = true
        } catch (e: Throwable) {
            XposedBridge.log("pmsClass加载失败")
            XposedBridge.log(e)
            return
        }

        XposedHelpers.findAndHookMethod(
            pmsClass,
            "filterSharedLibPackageLPr",
            packageSettingClass,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            this
        )
    }

    override fun afterHookedMethod(param: MethodHookParam) {
        if (param.result == true) {
            return
        }
        val packageSetting = param.args.first()
        val targetPackageName = packageSetting?.packageName ?: return
        val callingPackageName =
            getPackageNameForUidMethod.invoke(param.thisObject, param.args[1])
                ?.toString()?.split(":")?.first() ?: return

        val shouldIntercept = HookChecker.shouldIntercept(
            targetPackageName,
            callingPackageName
        )
        if (shouldIntercept) {
            param.result = true
        }
    }
}