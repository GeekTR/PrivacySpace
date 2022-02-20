package cn.geektang.privacyspace.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream

object Su {
    suspend fun checkRoot(): Boolean {
        return withContext(Dispatchers.IO) {
            var result = false
            var process: Process? = null
            try {
                process = Runtime.getRuntime().exec("su")
                process.outputStream.use { outputStream ->
                    DataOutputStream(outputStream).use { dataOutputStream ->
                        dataOutputStream.writeBytes("exit\n")
                        dataOutputStream.flush()
                    }
                }
                result = process.waitFor() == 0
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                process?.destroy()
            }
            return@withContext result
        }
    }
}