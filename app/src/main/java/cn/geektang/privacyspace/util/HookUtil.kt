package cn.geektang.privacyspace.util

object HookUtil {
    private val pmsClassNameArray = arrayOf(
        "com.android.server.pm.PackageManagerService",
        "com.android.server.pm.OplusPackageManagerService",
        "com.android.server.pm.OppoPackageManagerService"
    )

    fun loadPms(classLoader: ClassLoader): Class<*>? {
        var pmsClass: Class<*>? = null
        for (pmsClassName in pmsClassNameArray) {
            try {
                pmsClass = classLoader.loadClass(pmsClassName)
                if (pmsClass != null) {
                    break
                }
            } catch (ignored: ClassNotFoundException) {
            }
        }
        return pmsClass
    }
}