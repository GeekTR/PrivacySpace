package cn.geektang.privacyspace.hook.impl

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.ServiceManager
import android.util.ArrayMap
import android.util.SparseArray
import androidx.core.util.forEach
import cn.geektang.privacyspace.constant.ConfigConstant
import cn.geektang.privacyspace.hook.HookMain
import cn.geektang.privacyspace.util.HookUtil
import cn.geektang.privacyspace.util.XLog

object HookChecker {
    @Volatile
    private var greenChannel = false
    private var defaultBlindWhitelist: Set<String> = emptySet()

    fun shouldIntercept(
        classLoader: ClassLoader,
        userId: Int,
        targetPackageName: String,
        callingPackageName: String
    ): Boolean {
        if (greenChannel) {
            return false
        }

        if (defaultBlindWhitelist.isEmpty()) {
            greenChannel = true

            val sharedUserIdMap = getSharedUserIdMap(classLoader)
            if (null != sharedUserIdMap) {
                val defaultBlindWhitelist = ConfigConstant.defaultBlindWhitelist.toMutableSet()
                for (white in ConfigConstant.defaultBlindWhitelist) {
                    val value = sharedUserIdMap[white] ?: emptyList()
                    defaultBlindWhitelist.addAll(value)
                }
                this@HookChecker.defaultBlindWhitelist = defaultBlindWhitelist
            }
        }
        greenChannel = false

        if (callingPackageName == targetPackageName) {
            return false
        }

        var result = false
        val shouldFilterAppList = HookMain.hiddenAppList
        val userWhitelist = HookMain.whitelist
        val connectedAppsInfoMap = HookMain.connectedApps
        val multiUserConfig = HookMain.multiUserConfig
        val blindApps = HookMain.blind
        val defaultWhitelist = ConfigConstant.defaultWhitelist
        val defaultBlindWhitelist = defaultBlindWhitelist

        if (defaultBlindWhitelist.isNotEmpty()
            && !defaultBlindWhitelist.contains(targetPackageName)
            && blindApps.contains(callingPackageName)
        ) {
            XLog.i("$callingPackageName was prevented from reading ${targetPackageName}.")
            return true
        }

        if (!defaultWhitelist.contains(callingPackageName)
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

    private fun getSharedUserIdMap(classLoader: ClassLoader): Map<String, List<String>>? {
        val pms = ServiceManager.getService("package")
        val pmsClass = HookUtil.loadPms(classLoader)
        if (pms?.javaClass == pmsClass) {
            return if (Build.VERSION.SDK_INT >= 29) {
                getSharedUidMapAfterQ(pms)
            } else {
                getSharedUidMapCompat(pms)
            }
        }
        return null
    }

    // more efficient
    private fun getSharedUidMapAfterQ(pms: Any): Map<String, List<String>>? {
        val pmsClass = pms.javaClass
        return try {
            val getAppsWithSharedUserMethod =
                pmsClass.getDeclaredMethod("getAppsWithSharedUserIdsLocked")
            getAppsWithSharedUserMethod.isAccessible = true
            val getPackagesForUidMethod = pmsClass.getDeclaredMethod(
                "getPackagesForUid",
                Int::class.javaPrimitiveType
            )
            getPackagesForUidMethod.isAccessible = true

            val sharedUserIdMap = ArrayMap<String, List<String>>()
            val result = getAppsWithSharedUserMethod.invoke(pms) as SparseArray<*>
            result.forEach { key, value ->
                val packages =
                    getPackagesForUidMethod.invoke(
                        pms,
                        key
                    ) as Array<*>
                sharedUserIdMap[value.toString()] = (packages as Array<String>).toList()
            }
            sharedUserIdMap
        } catch (e: Throwable) {
            getSharedUidMapCompat(pms)
        }
    }

    // better compatibility
    private fun getSharedUidMapCompat(pms: Any): Map<String, List<String>>? {
        val pmsClass = pms.javaClass
        return try {
            val getInstalledPackagesMethod = pmsClass.getDeclaredMethod(
                "getInstalledPackages",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            ) ?: return null
            getInstalledPackagesMethod.isAccessible = true
            val resultParceledListSlice = getInstalledPackagesMethod.invoke(
                pms,
                PackageManager.MATCH_UNINSTALLED_PACKAGES,
                0
            )
            val listMethod = resultParceledListSlice.javaClass.getDeclaredMethod("getList")
            listMethod.isAccessible = true
            val resultList = listMethod.invoke(resultParceledListSlice) as? List<*> ?: return null
            val sharedUserIdMap = ArrayMap<String, MutableList<String>>()
            for (packageInfo in resultList) {
                if (packageInfo !is PackageInfo) return null
                val sharedUserId = packageInfo.sharedUserId
                if (sharedUserId.isNullOrEmpty()) {
                    continue
                }
                val sharedUserIdPackages =
                    sharedUserIdMap.getOrDefault(sharedUserId, mutableListOf())
                sharedUserIdPackages.add(packageInfo.packageName)
                sharedUserIdMap[sharedUserId] = sharedUserIdPackages
            }
            sharedUserIdMap
        } catch (e: Throwable) {
            null
        }
    }
}