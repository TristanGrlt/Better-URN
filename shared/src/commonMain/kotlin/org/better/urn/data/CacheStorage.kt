package org.better.urn.data

expect object CacheStorage {
    fun saveString(key: String, content: String)
    fun getString(key: String): String?
    fun saveBytes(key: String, bytes: ByteArray): String?
    fun getFilePath(key: String): String?
    fun remove(key: String)
    fun clear()
}


