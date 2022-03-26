package cn.geektang.privacyspace.hook.impl

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.UserHandle
import cn.geektang.privacyspace.hook.Hooker
import cn.geektang.privacyspace.util.ConfigHelper.getPackageName
import cn.geektang.privacyspace.util.HookUtil
import cn.geektang.privacyspace.util.XLog
import cn.geektang.privacyspace.util.tryLoadClass
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Field
import java.lang.reflect.Method

object FrameworkHookerApi28Impl : XC_MethodHook(), Hooker {
    private lateinit var pmsClass: Class<*>
    private lateinit var settingsClass: Class<*>
    private lateinit var mSettingsField: Field
    private lateinit var getAppIdMethod: Method
    private lateinit var getSettingLPrMethod: Method
    private lateinit var classLoader: ClassLoader

    override fun start(classLoader: ClassLoader) {
        this.classLoader = classLoader
        try {
            pmsClass = HookUtil.loadPms(classLoader) ?: throw PackageManager.NameNotFoundException()
            mSettingsField = pmsClass.getDeclaredField("mSettings")
            mSettingsField.isAccessible = true

            settingsClass = classLoader.tryLoadClass("com.android.server.pm.Settings")
            getAppIdMethod =
                UserHandle::class.java.getDeclaredMethod("getAppId", Int::class.javaPrimitiveType)
            getSettingLPrMethod =
                settingsClass.getDeclaredMethod("getSettingLPr", Int::class.javaPrimitiveType)
            getAppIdMethod.isAccessible = true
            getSettingLPrMethod.isAccessible = true
        } catch (e: Throwable) {
            XLog.e(e, "pms load failed.")
            return
        }
        pmsClass.declaredMethods.forEach { method ->
            when (method.name) {
                "filterAppAccessLPr" -> {
                    if (method.parameterCount == 5) {
                        XposedBridge.hookMethod(method, this)
                    }
                }
                "applyPostResolutionFilter" -> {
                    XposedBridge.hookMethod(method, this)
                }
                else -> {}
            }
        }
    }

    override fun afterHookedMethod(param: MethodHookParam) {
        when (param.method.name) {
            "filterAppAccessLPr" -> {
                hookFilterAppAccess(param)
            }
            "applyPostResolutionFilter" -> {
                hookApplyPostResolutionFilter(param)
            }
            else -> {}
        }

    }

    private fun hookApplyPostResolutionFilter(param: MethodHookParam) {
        val resultList = param.result as? MutableList<*> ?: return
        val callingUid = param.args[3] as? Int ?: return
        val userId = param.args[5] as? Int ?: return
        val callingPackageName = getPackageName(param.thisObject, callingUid) ?: return
        val waitRemoveList = mutableListOf<ResolveInfo>()
        for (resolveInfo in resultList) {
            val targetPackageName = (resolveInfo as? ResolveInfo)?.getPackageName() ?: continue
            val shouldIntercept = HookChecker.shouldIntercept(
                classLoader,
                userId,
                targetPackageName,
                callingPackageName
            )
            if (shouldIntercept) {
                waitRemoveList.add(resolveInfo)
            }
        }

        for (resolveInfo in waitRemoveList) {
            resultList.remove(resolveInfo)
        }
        if (waitRemoveList.isNotEmpty()) {
            param.result = resultList
        }
    }

    private fun hookFilterAppAccess(param: MethodHookParam) {
        if (param.result == true) {
            return
        }
        val packageSetting = param.args.first()
        val targetPackageName = packageSetting?.packageName ?: return
        val callingUid = param.args[1] as Int
        val userId = param.args[4] as Int
        val callingPackageName = getPackageName(param.thisObject, callingUid) ?: return

        val shouldIntercept = HookChecker.shouldIntercept(
            classLoader,
            userId,
            targetPackageName,
            callingPackageName
        )
        if (shouldIntercept) {
            param.result = true
        }
    }

    private fun getPackageName(pms: Any, uid: Int): String? {
        val callingAppId = getAppIdMethod.invoke(null, uid)
        val mSettings = mSettingsField.get(pms)
        return getSettingLPrMethod.invoke(mSettings, callingAppId)?.packageName
    }
}