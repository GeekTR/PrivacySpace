package cn.geektang.privacyspace.bean

import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

class AppInfo(
    val applicationInfo: ApplicationInfo,
    val packageName: String,
    val appIcon: Drawable,
    val appName: String,
    val isSystemApp: Boolean,
    val isXposedModule: Boolean
)