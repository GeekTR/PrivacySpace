package cn.geektang.privacyspace.hook.impl

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Binder
import cn.geektang.privacyspace.hook.Hooker
import cn.geektang.privacyspace.util.ConfigHelper.getPackageName
import cn.geektang.privacyspace.util.HookUtil
import cn.geektang.privacyspace.util.XLog
import cn.geektang.privacyspace.util.tryLoadClass
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Method

object FrameworkHookerApi26Impl : XC_MethodHook(), Hooker {
    private lateinit var pmsClass: Class<*>
    private lateinit var packageSettingClass: Class<*>
    private lateinit var getPackageNameForUidMethod: Method

    override fun start(classLoader: ClassLoader) {
        try {
            pmsClass = HookUtil.loadPms(classLoader) ?: throw PackageManager.NameNotFoundException()
            packageSettingClass =
                classLoader.tryLoadClass("com.android.server.pm.PackageSetting")
            getPackageNameForUidMethod = pmsClass.getDeclaredMethod(
                "getNameForUid",
                Int::class.javaPrimitiveType
            )
            getPackageNameForUidMethod.isAccessible = true
        } catch (e: Throwable) {
            XLog.e(e, "pmsClass load failed.")
            return
        }

        pmsClass.declaredMethods.forEach { method ->
            when (method.name) {
                "filterSharedLibPackageLPr" -> {
                    if (method.parameterCount == 4) {
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
            "filterSharedLibPackageLPr" -> {
                hookFilterSharedLibPackageLPr(param)
            }
            "applyPostResolutionFilter" -> {
                hookApplyPostResolutionFilter(param)
            }
        }
    }

    private fun hookApplyPostResolutionFilter(param: MethodHookParam) {
        val resultList = param.result as? MutableList<*> ?: return
        val uid = Binder.getCallingUid()
        val userId = uid / 100000
        val callingPackageName =
            getPackageNameForUidMethod.invoke(param.thisObject, uid)
                ?.toString()?.split(":")?.first() ?: return
        val waitRemoveList = mutableListOf<ResolveInfo>()
        for (resolveInfo in resultList) {
            val targetPackageName = (resolveInfo as? ResolveInfo)?.getPackageName() ?: continue
            val shouldIntercept = HookChecker.shouldIntercept(
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

    private fun hookFilterSharedLibPackageLPr(param: MethodHookParam) {
        if (param.result == true) {
            return
        }
        val packageSetting = param.args.first()
        val targetPackageName = packageSetting?.packageName ?: return
        val userId = param.args[2] as? Int ?: return
        val callingPackageName =
            getPackageNameForUidMethod.invoke(param.thisObject, param.args[1])
                ?.toString()?.split(":")?.first() ?: return

        val shouldIntercept = HookChecker.shouldIntercept(
            userId,
            targetPackageName,
            callingPackageName
        )
        if (shouldIntercept) {
            param.result = true
        }
    }
}