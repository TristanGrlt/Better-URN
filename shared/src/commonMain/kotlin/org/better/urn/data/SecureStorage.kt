package org.better.urn.data

expect object SecureStorage {
    fun saveSecureString(key: String, value: String)
    fun getSecureString(key: String): String?
    fun removeSecureString(key: String)
    fun clear()
}
