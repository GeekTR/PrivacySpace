package cn.geektang.privacyspace.bean

import cn.geektang.privacyspace.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConfigData(
    @Json(name = "enableLog")
    val enableDetailLog: Boolean,
    val hiddenAppList: Set<String>,
    val whitelist: Set<String>,
    val connectedApps: Map<String, Set<String>>,
    val sharedUserIdMap: Map<String, String>?
) {
    companion object {
        val EMPTY = ConfigData(
            BuildConfig.DEBUG,
            hiddenAppList = emptySet(),
            whitelist = emptySet(),
            connectedApps = emptyMap(),
            sharedUserIdMap = emptyMap()
        )
    }
}
