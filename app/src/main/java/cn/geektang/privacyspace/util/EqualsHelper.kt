package cn.geektang.privacyspace.util

import cn.geektang.privacyspace.bean.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow

fun List<AppInfo>.isAllEquals(others: List<AppInfo>): Boolean {
    if (this.size != others.size) {
        return false
    }

    var isAllEquals = true
    for (index in 0 until size) {
        val thisApp = get(index)
        val othersApp = others[index]
        if (
            thisApp.packageName != othersApp.packageName
            || thisApp.appName != othersApp.appName
            || thisApp.isXposedModule != othersApp.isXposedModule
            || thisApp.isSystemApp != othersApp.isSystemApp
        ) {
            isAllEquals = false
            break
        }
    }
    return isAllEquals
}

fun MutableStateFlow<List<AppInfo>>.setDifferentValue(newValue: List<AppInfo>) {
    if (!value.isAllEquals(newValue)) {
        value = newValue
    }
}