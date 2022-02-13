package cn.geektang.privacyspace

object ConfigConstant {
    const val ANDROID_FRAMEWORK = "android"
    const val CONFIG_FILE_FOLDER = "/data/system/privacy_space/"
    const val CONFIG_FILE_APP_LIST = "app.list"
    const val CONFIG_FILE_WHITELIST = "whitelist"

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
        BuildConfig.APPLICATION_ID
    )

    val specialHookApps = setOf(
        "com.miui.cleanmaster",
        "com.miui.securitycenter"
    )
}