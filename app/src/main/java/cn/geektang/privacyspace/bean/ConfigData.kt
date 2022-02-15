package cn.geektang.privacyspace.bean

import cn.geektang.privacyspace.BuildConfig
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConfigData(
    val enableLog: Boolean,
    val hiddenAppList: List<String>,
    val whitelist: List<String>,
    val connectedApps: Map<String, Set<String>>
) {
    companion object {
        val EMPTY = ConfigData(
            BuildConfig.DEBUG,
            hiddenAppList = emptyList(),
            whitelist = emptyList(),
            connectedApps = emptyMap()
        )
    }
}
