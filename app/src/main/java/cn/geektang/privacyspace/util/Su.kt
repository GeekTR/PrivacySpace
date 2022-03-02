package cn.geektang.privacyspace.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.lang.Exception

object Su {
    suspend fun exec(command: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec("su -c $command")
                process.outputStream.use { outputStream ->
                    DataOutputStream(outputStream).use {
                        it.writeBytes("exit\n")
                        it.flush()
                    }
                }
                process.waitFor() == 0
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}