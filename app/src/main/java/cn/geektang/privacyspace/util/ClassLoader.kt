package cn.geektang.privacyspace.util

@Throws(ClassNotFoundException::class)
fun ClassLoader.tryLoadClass(name: String): Class<*> {
    return loadClass(name) ?: throw ClassNotFoundException()
}

fun ClassLoader.loadClassSafe(name: String): Class<*>? {
    return try {
        tryLoadClass(name)
    } catch (e: ClassNotFoundException) {
        null
    }
}