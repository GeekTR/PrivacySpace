package cn.geektang.privacyspace.constant

import cn.geektang.privacyspace.BuildConfig

object ConfigConstant {
    const val ANDROID_FRAMEWORK = "android"
    const val CONFIG_FILE_FOLDER_ORIGINAL = "/data/system/privacy_space/"
    const val CONFIG_FILE_FOLDER = "/data/system/${BuildConfig.APPLICATION_ID}/"
    const val CONFIG_FILE_JSON = "config.json"

    val defaultWhitelist = setOf(
        "com.android.systemui",
        "android.uid.system",
        "com.android.providers.media.module",
        "com.android.providers.telephony",
        "com.android.providers.calendar",
        "com.android.providers.media",
        "com.android.providers.downloads",
        "com.android.providers.downloads.ui",
        "com.android.providers.settings",
        "com.android.providers.partnerbookmarks",
        "com.android.providers.settings.auto_generated_rro_product__",
        "com.android.providers.contacts.auto_generated_rro_product__",
        "com.android.providers.telephony.auto_generated_rro_product__",
        "com.android.bookmarkprovider",
        "com.android.providers.blockednumber",
        "com.android.providers.userdictionary",
        "com.android.providers.media.module",
        "com.android.providers.contacts",
        "com.android.permissioncontroller",
        "com.lbe.security.miui",
        "com.google.android.documentsui",
        "android.uid.phone",
        "com.topjohnwu.magisk",
        "android.uid.nfc",
        "android.uid.bluetooth",
        "android.uid.systemui",
        "android.uid.networkstack",
        "com.google.uid.shared",
        "com.miui.packageinstaller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.google.android.providers.media.module",
        "com.google.android.permissioncontroller",
        "com.google.android.webview",
        BuildConfig.APPLICATION_ID
    )

    val defaultBlindWhitelist = setOf(
        "android",
        "android.uid.system",
        "android.uid.phone",
        "android.uid.bluetooth",
        "android.uid.nfc",
        "android.uid.se",
        "android.uid.networkstack",
        "android.uid.shell",
        "android.uid.shared",
        "android.uid.qtiphone",
        "android.uid.systemui",
        "android.media",
        "android.uid.calendar",
        "com.android.emergency.uid",
        "com.google.uid.shared",
        "com.google.android.webview",
        "com.google.android.providers.media.module",
        "com.android.providers.media.module",
        "com.android.providers.telephony",
        "com.android.providers.calendar",
        "com.android.providers.media",
        "com.android.providers.downloads",
        "com.android.providers.downloads.ui",
        "com.android.providers.settings",
        "com.android.providers.partnerbookmarks",
        "com.android.providers.settings.auto_generated_rro_product__",
        "com.android.providers.contacts.auto_generated_rro_product__",
        "com.android.providers.telephony.auto_generated_rro_product__",
        "com.android.bookmarkprovider",
        "com.android.providers.blockednumber",
        "com.android.providers.userdictionary",
        "com.android.providers.media.module",
        "com.android.providers.contacts",
        "com.android.permissioncontroller"
    )
}