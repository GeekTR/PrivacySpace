package cn.geektang.privacyspace.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import androidx.collection.ArrayMap
import cn.geektang.privacyspace.BuildConfig
import cn.geektang.privacyspace.R
import cn.geektang.privacyspace.bean.AppInfo
import com.microsoft.appcenter.analytics.Analytics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

object AppHelper {
    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: Flow<List<AppInfo>> = _allApps
    private var getAppsRetryTimes = 0

    suspend fun initialize(context: Context) {
        val apps = try {
            val result = context.loadAllAppList()
            getAppsRetryTimes = 0
            result
        } catch (ignored: CancellationException) {
            return
        } catch (e: Exception) {
            val properties = ArrayMap<String, String>()
            properties["exception"] = e.javaClass.name
            Analytics.trackEvent("ReadAppsFailed", properties)
            getAppsRetryTimes++

            e.printStackTrace()
            if (getAppsRetryTimes == 3) {
                context.showToast(R.string.tips_get_apps_failed)
            }
            delay(1000)
            // retry after 1 seconds
            initialize(context)
            return
        }
        _allApps.emit(apps)
    }

    private suspend fun Context.loadAllAppList(): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            val packageManager = packageManager
            return@withContext packageManager
                .getInstalledPackages(PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.GET_META_DATA)
                .mapNotNull { packageInfo ->
                    val applicationInfo = packageInfo.applicationInfo
                    val appName = applicationInfo.loadLabel(packageManager).toString()
                    val appIcon = applicationInfo.loadIcon(packageManager)
                    AppInfo(
                        applicationInfo = applicationInfo,
                        packageName = applicationInfo.packageName,
                        appName = appName,
                        appIcon = appIcon,
                        sharedUserId = packageInfo.sharedUserId,
                        isSystemApp = isSystemApp(applicationInfo),
                        isXposedModule = applicationInfo.isXposedModule()
                    )
                }
        }
    }

    suspend fun List<AppInfo>.sortApps(
        context: Context,
        toTopCollections: Collection<String>
    ): List<AppInfo> {
        return withContext(Dispatchers.Default) {
            val applicationInfoComparator =
                ApplicationInfo.DisplayNameComparator(context.packageManager)
            return@withContext sortedWith(Comparator { t, t2 ->
                val isContainsT = toTopCollections.contains(t.packageName)
                val isContainsT2 = toTopCollections.contains(t2.packageName)
                if (isContainsT && !isContainsT2) {
                    return@Comparator -1
                } else if (!isContainsT && isContainsT2) {
                    return@Comparator 1
                }
                if (t.isXposedModule && !t2.isXposedModule) {
                    return@Comparator -1
                } else if (!t.isXposedModule && t2.isXposedModule) {
                    return@Comparator 1
                }
                return@Comparator applicationInfoComparator.compare(
                    t.applicationInfo,
                    t2.applicationInfo
                )
            })
        }
    }

    fun isSystemApp(applicationInfo: ApplicationInfo): Boolean {
        return (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM
    }

    fun Context.getLauncherPackageName(): String? {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val res = packageManager.resolveActivity(intent, 0)
        if (res?.activityInfo == null) {
            return null
        }
        return if (res.activityInfo.packageName == "android") {
            null
        } else {
            res.activityInfo.packageName
        }
    }

    fun Context.getApkInstallerPackageName(): String? {
        val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
        val intent = Intent(Intent.ACTION_DELETE, uri)
        val res = packageManager.resolveActivity(intent, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        if (res?.activityInfo == null) {
            return null
        }
        return if (res.activityInfo.packageName == "android") {
            null
        } else {
            res.activityInfo.packageName
        }
    }

    fun getXposedModuleScopeList(context: Context, app: ApplicationInfo): List<String> {
        val pm = context.packageManager
        val scopeList = mutableListOf<String>()
        try {
            val scopeListResourceId: Int = app.metaData.getInt("xposedscope")
            if (scopeListResourceId != 0) {
                scopeList.addAll(
                    pm.getResourcesForApplication(app).getStringArray(scopeListResourceId)
                )
            } else {
                val scopeListString: String? = app.metaData.getString("xposedscope")
                if (scopeListString != null) {
                    scopeList.addAll(scopeListString.split(";"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return scopeList
    }

    fun getPackageInfo(context: Context, packageName: String, flag: Int = 0): PackageInfo? {
        return try {
            context.packageManager.getPackageInfo(
                packageName,
                flag
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun ApplicationInfo.isXposedModule(): Boolean {
        return metaData?.getBoolean("xposedmodule") == true ||
                metaData?.containsKey("xposedminversion") == true
    }

    fun startWatchingAppsCountChange(
        context: Context,
        onAppRemoved: (packageName: String) -> Unit
    ) {
        val packageFilter = IntentFilter()
        packageFilter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        packageFilter.addDataScheme("package")
        val receiver = object : BroadcastReceiver() {
            val scope = MainScope()
            override fun onReceive(cotext: Context, intent: Intent) {
                val packageName = intent.dataString?.substringAfter("package:") ?: return

                when (intent.action) {
                    Intent.ACTION_PACKAGE_FULLY_REMOVED -> {
                        onAppRemoved(packageName)
                        scope.launch {
                            val apps = _allApps.value.toMutableList().filter {
                                it.packageName != packageName
                            }
                            _allApps.emit(apps)
                        }
                    }
                    Intent.ACTION_PACKAGE_ADDED -> {
                        val appInfo = try {
                            getAppInfo(context, packageName) ?: return
                        } catch (e: PackageManager.NameNotFoundException) {
                            e.printStackTrace()
                            return
                        }
                        // switch to ui thread
                        scope.launch {
                            val apps = _allApps.value.toMutableList()
                            apps.add(appInfo)
                            _allApps.emit(apps)
                        }
                    }
                    else -> {}
                }
            }
        }
        context.applicationContext.registerReceiver(receiver, packageFilter)
    }

    @Throws(PackageManager.NameNotFoundException::class)
    private fun getAppInfo(context: Context, packageName: String): AppInfo? {
        val packageManager = context.packageManager
        val packageInfo =
            getPackageInfo(context, packageName, PackageManager.GET_META_DATA) ?: return null
        val applicationInfo = packageInfo.applicationInfo
        val appName = applicationInfo.loadLabel(packageManager).toString()
        val appIcon = applicationInfo.loadIcon(packageManager)
        return AppInfo(
            applicationInfo = applicationInfo,
            packageName = applicationInfo.packageName,
            appName = appName,
            appIcon = appIcon,
            sharedUserId = packageInfo.sharedUserId,
            isSystemApp = isSystemApp(applicationInfo),
            isXposedModule = packageInfo.applicationInfo.isXposedModule()
        )
    }
}