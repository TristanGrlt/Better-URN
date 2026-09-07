package org.better.urn.data

import java.io.File

object CourseImageCache {
    /**
     * Generates a unique cache key based on course ID and raw image URL.
     */
    fun getCacheKey(courseId: Int, rawImageUrl: String): String {
        val cleanUrl = rawImageUrl.substringBefore("?")
        val hash = cleanUrl.hashCode().toString().replace("-", "n")
        return "course_img_${courseId}_$hash"
    }

    /**
     * Returns the cached file path if the image exists locally on disk.
     */
    fun getCachedImagePath(courseId: Int, rawImageUrl: String): String? {
        if (rawImageUrl.isBlank()) return null
        if (rawImageUrl.startsWith("http://") || rawImageUrl.startsWith("https://")) {
            val key = getCacheKey(courseId, rawImageUrl)
            return CacheStorage.getFilePath(key)
        }
        val cleanPath = rawImageUrl.removePrefix("file:")
        return if (File(cleanPath).exists()) cleanPath else null
    }

    /**
     * Downloads and caches the course illustration image if not present on disk.
     * Returns the local file path if cached or downloaded, or [rawImageUrl] on failure.
     */
    suspend fun getOrFetchCourseImage(
        courseId: Int,
        rawImageUrl: String,
        client: MoodleClient,
    ): String {
        if (rawImageUrl.isBlank()) return rawImageUrl
        if (!rawImageUrl.startsWith("http://") && !rawImageUrl.startsWith("https://")) {
            return rawImageUrl
        }

        val cachedPath = getCachedImagePath(courseId, rawImageUrl)
        if (cachedPath != null) return cachedPath

        val bytes = client.downloadBytes(rawImageUrl) ?: return rawImageUrl
        if (bytes.isEmpty()) return rawImageUrl

        val key = getCacheKey(courseId, rawImageUrl)
        return CacheStorage.saveBytes(key, bytes) ?: rawImageUrl
    }
}
