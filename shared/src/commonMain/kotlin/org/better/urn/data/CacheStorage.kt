package org.better.urn.data

expect object CacheStorage {
    fun saveString(key: String, content: String)
    fun getString(key: String): String?
    fun remove(key: String)
    fun clear()
}
