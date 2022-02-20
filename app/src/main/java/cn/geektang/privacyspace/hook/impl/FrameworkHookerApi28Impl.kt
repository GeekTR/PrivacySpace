package cn.geektang.privacyspace.hook.impl

import android.content.ComponentName
import android.os.UserHandle
import cn.geektang.privacyspace.hook.Hooker
import cn.geektang.privacyspace.util.tryLoadClass
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Field
import java.lang.reflect.Method

object FrameworkHookerApi28Impl : XC_MethodHook(), Hooker {
    private lateinit var pmsClass: Class<*>
    private lateinit var packageSettingClass: Class<*>
    private lateinit var getPackageNameForUidMethod: Method
    private lateinit var settingsClass: Class<*>
    private lateinit var mSettingsField: Field
    private lateinit var getAppIdMethod: Method
    private lateinit var getSettingLPrMethod: Method

    override fun start(classLoader: ClassLoader) {
        try {
            pmsClass =
                classLoader.tryLoadClass("com.android.server.pm.PackageManagerService")
            packageSettingClass =
                classLoader.tryLoadClass("com.android.server.pm.PackageSetting")
            settingsClass = classLoader.tryLoadClass("com.android.server.pm.Settings")
            mSettingsField = pmsClass.getDeclaredField("mSettings")
            mSettingsField.isAccessible = true
            getPackageNameForUidMethod = pmsClass.getDeclaredMethod(
                "getNameForUid",
                Int::class.javaPrimitiveType
            )
            getAppIdMethod =
                UserHandle::class.java.getDeclaredMethod("getAppId", Int::class.javaPrimitiveType)
            getSettingLPrMethod =
                settingsClass.getDeclaredMethod("getSettingLPr", Int::class.javaPrimitiveType)
            getPackageNameForUidMethod.isAccessible = true
            getAppIdMethod.isAccessible = true
            getSettingLPrMethod.isAccessible = true
        } catch (e: Throwable) {
            XposedBridge.log("pmsClass加载失败")
            XposedBridge.log(e)
            return
        }

        XposedHelpers.findAndHookMethod(
            pmsClass,
            "filterAppAccessLPr",
            packageSettingClass,
            Int::class.javaPrimitiveType,
            ComponentName::class.java,
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
        val callingUid = param.args[1] as Int
        val callingAppId = getAppIdMethod.invoke(null, callingUid)
        val mSettings = mSettingsField.get(param.thisObject)
        val callingPackageName =
            getSettingLPrMethod.invoke(mSettings, callingAppId)?.packageName ?: return

        val shouldIntercept = HookChecker.shouldIntercept(
            targetPackageName,
            callingPackageName
        )
        if (shouldIntercept) {
            param.result = true
        }
    }
}