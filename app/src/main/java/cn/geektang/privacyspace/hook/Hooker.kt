package cn.geektang.privacyspace.hook

interface Hooker {
    fun start(classLoader: ClassLoader)

    val Any.packageName: String
        get() = toString().substringAfterLast(" ").substringBefore("/")
}