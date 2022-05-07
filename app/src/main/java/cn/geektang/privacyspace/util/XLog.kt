package cn.geektang.privacyspace.util

import de.robv.android.xposed.XposedBridge

object XLog {
    var enableLog : Boolean = false

    fun d(message: String) {
        if (enableLog) {
            XposedBridge.log("[PrivacySpace] [Debug] $message")
        }
    }

    fun i(message: String) {
        XposedBridge.log("[PrivacySpace] [Info] $message")
    }

    fun e(message: String) {
        XposedBridge.log("[PrivacySpace] [Error] $message")
    }

    fun e(cause: Throwable, message: String) {
        XposedBridge.log("[PrivacySpace] [Error] $message")
        XposedBridge.log(cause)
    }
}