package cn.geektang.privacyspace.bean

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SystemUserInfo(
    val id: Int,
    val name: String
)