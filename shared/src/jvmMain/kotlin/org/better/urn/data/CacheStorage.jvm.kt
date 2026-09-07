package org.better.urn.data

import java.io.File

actual object CacheStorage {
    var overrideCacheDir: File? = null

    private fun isRunningInTest(): Boolean {
        if (System.getProperty("betterurn.cache.dir") != null) return true
        if (System.getProperty("org.gradle.test.worker") != null) return true
        return Thread.currentThread().stackTrace.any { element ->
            val name = element.className.lowercase()
            name.contains("test") || name.contains("junit")
        }
    }

    private val cacheDir: File
        get() {
            val dir = overrideCacheDir
                ?: System.getProperty("betterurn.cache.dir")?.let { File(it) }
                ?: if (isRunningInTest()) {
                    val tmpDir = System.getProperty("java.io.tmpdir") ?: "."
                    File(tmpDir, "betterurn_test_cache")
                } else {
                    val userHome = System.getProperty("user.home") ?: "."
                    File(userHome, ".betterurn/cache")
                }
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    private val imageCacheDir: File
        get() {
            val dir = File(cacheDir, "image_cache")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    private fun getFileForKey(key: String): File {
        val safeFileName = key.replace(Regex("[^a-zA-Z0-9._-]"), "_") + ".json"
        return File(cacheDir, safeFileName)
    }

    private fun getFileForImageKey(key: String): File {
        val safeFileName = key.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(imageCacheDir, safeFileName)
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

    actual fun saveBytes(key: String, bytes: ByteArray): String? {
        return try {
            val file = getFileForImageKey(key)
            file.writeBytes(bytes)
            file.absolutePath
        } catch (e: Exception) {
            println("CacheStorage JVM saveBytes Error: ${e.message}")
            null
        }
    }

    actual fun getFilePath(key: String): String? {
        return try {
            val file = getFileForImageKey(key)
            if (file.exists() && file.length() > 0) {
                file.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            println("CacheStorage JVM getFilePath Error: ${e.message}")
            null
        }
    }

    actual fun remove(key: String) {
        try {
            val file = getFileForKey(key)
            if (file.exists()) {
                file.delete()
            }
            val imgFile = getFileForImageKey(key)
            if (imgFile.exists()) {
                imgFile.delete()
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
            imageCacheDir.listFiles()?.forEach { file ->
                if (file.isFile) file.delete()
            }
        } catch (e: Exception) {
            println("CacheStorage JVM clear Error: ${e.message}")
        }
    }
}

