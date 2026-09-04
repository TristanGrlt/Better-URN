package org.better.urn.data

import java.io.File

actual object CacheStorage {
    private val cacheDir: File by lazy {
        val userHome = System.getProperty("user.home") ?: "."
        val dir = File(userHome, ".betterurn/cache")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    private fun getFileForKey(key: String): File {
        val safeFileName = key.replace(Regex("[^a-zA-Z0-9._-]"), "_") + ".json"
        return File(cacheDir, safeFileName)
    }

    actual fun saveString(key: String, content: String) {
        try {
            val file = getFileForKey(key)
            file.writeText(content, Charsets.UTF_8)
        } catch (e: Exception) {
            println("CacheStorage JVM save Error: ${e.message}")
        }
    }

    actual fun getString(key: String): String? {
        return try {
            val file = getFileForKey(key)
            if (file.exists()) {
                file.readText(Charsets.UTF_8)
            } else {
                null
            }
        } catch (e: Exception) {
            println("CacheStorage JVM get Error: ${e.message}")
            null
        }
    }

    actual fun remove(key: String) {
        try {
            val file = getFileForKey(key)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            println("CacheStorage JVM remove Error: ${e.message}")
        }
    }

    actual fun clear() {
        try {
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile) file.delete()
            }
        } catch (e: Exception) {
            println("CacheStorage JVM clear Error: ${e.message}")
        }
    }
}
