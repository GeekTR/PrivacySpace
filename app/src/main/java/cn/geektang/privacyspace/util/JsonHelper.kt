package cn.geektang.privacyspace.util

import cn.geektang.privacyspace.bean.ConfigData
import cn.geektang.privacyspace.bean.SystemUserInfo
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

object JsonHelper {
    private val moshi = Moshi.Builder().build()

    fun configAdapter(): JsonAdapter<ConfigData> {
        return moshi.adapter(ConfigData::class.java)
    }

    fun systemUserInfoListAdapter(): JsonAdapter<List<SystemUserInfo>> {
        return moshi.adapter(
            Types.newParameterizedType(
                List::class.java,
                SystemUserInfo::class.java
            )
        )
    }
}