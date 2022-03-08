package cn.geektang.privacyspace.util

import android.annotation.StringRes
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast


val Context.sp: SharedPreferences
    get() {
        return getSharedPreferences("config", Context.MODE_PRIVATE)
    }

var SharedPreferences.hasReadNotice: Boolean
    get() {
        return getBoolean("readNotice", false)
    }
    set(value) {
        edit().putBoolean("readNotice", value).apply()
    }

var SharedPreferences.hasReadNotice2: Boolean
    get() {
        return getBoolean("readNotice2", false)
    }
    set(value) {
        edit().putBoolean("readNotice2", value).apply()
    }

var SharedPreferences.iconCellCountLandscape: Int
    get() {
        return getInt("iconCellCountLandscape", 8)
    }
    set(value) {
        edit().putInt("iconCellCountLandscape", value).apply()
    }

var SharedPreferences.iconPaddingLandscape: Float
    get() {
        return getFloat("iconPaddingLandscape", 0.5f)
    }
    set(value) {
        edit().putFloat("iconPaddingLandscape", value).apply()
    }

var SharedPreferences.iconCellCountPortrait: Int
    get() {
        return getInt("iconCellPortrait", 4)
    }
    set(value) {
        edit().putInt("iconCellPortrait", value).apply()
    }

var SharedPreferences.iconPaddingPortrait: Float
    get() {
        return getFloat("iconPaddingPortrait", 0.5f)
    }
    set(value) {
        edit().putFloat("iconPaddingPortrait", value).apply()
    }

fun Context.showToast(@StringRes textRes: Int) {
    showToast(getString(textRes))
}

fun Context.showToast(text: String) {
    Toast.makeText(
        this,
        String.format(text),
        Toast.LENGTH_SHORT
    ).show()
}

fun Context.openUrl(url: String) {
    val uri = Uri.parse(url)
    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}