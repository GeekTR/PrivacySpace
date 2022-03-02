package cn.geektang.privacyspace.util

import cn.geektang.privacyspace.hook.HookMain
import de.robv.android.xposed.XposedBridge

object XLog {
    fun d(message: String) {
        if (HookMain.enableLog) {
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