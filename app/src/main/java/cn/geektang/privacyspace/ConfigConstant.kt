package cn.geektang.privacyspace

object ConfigConstant {
    const val ANDROID_FRAMEWORK = "android"
    const val CONFIG_FILE_FOLDER_ORIGINAL = "/data/system/privacy_space/"
    const val CONFIG_FILE_FOLDER = "/data/system/${BuildConfig.APPLICATION_ID}/"
    const val CONFIG_FILE_JSON = "config.json"

    val defaultWhitelist = setOf(
        "com.android.systemui",
        "android.uid.system",
        "com.android.providers.media.module",
        "com.lbe.security.miui",
        "com.google.android.documentsui",
        "com.android.vending",
        "android.uid.phone",
        "com.topjohnwu.magisk",
        "android.uid.nfc",
        "android.uid.systemui",
        "android.uid.networkstack",
        "com.google.uid.shared",
        "com.miui.packageinstaller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.google.android.providers.media.module",
        BuildConfig.APPLICATION_ID
    )
}