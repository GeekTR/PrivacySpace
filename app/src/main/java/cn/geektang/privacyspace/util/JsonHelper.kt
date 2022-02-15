package cn.geektang.privacyspace.util

import cn.geektang.privacyspace.bean.ConfigData
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

object JsonHelper {
    private val moshi = Moshi.Builder().build()

    fun getConfigAdapter(): JsonAdapter<ConfigData> {
        return moshi.adapter(ConfigData::class.java)
    }
}