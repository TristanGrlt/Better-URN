package org.better.urn.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CourseImageCacheTest {

    @AfterTest
    fun cleanup() {
        CacheStorage.clear()
    }

    @Test
    fun testCacheStorageSaveAndRetrieveBytes() {
        val dummyBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val key = "test_image_key"

        assertNull(CacheStorage.getFilePath(key))

        val path = CacheStorage.saveBytes(key, dummyBytes)
        assertNotNull(path)
        assertTrue(java.io.File(path).exists())

        val retrievedPath = CacheStorage.getFilePath(key)
        assertNotNull(retrievedPath)
        assertEquals(path, retrievedPath)

        CacheStorage.remove(key)
        assertNull(CacheStorage.getFilePath(key))
    }

    @Test
    fun testCourseImageCacheKey() {
        val key1 = CourseImageCache.getCacheKey(101, "https://moodle.univ.fr/image.jpg?token=abc")
        val key2 = CourseImageCache.getCacheKey(101, "https://moodle.univ.fr/image.jpg?token=xyz")
        val key3 = CourseImageCache.getCacheKey(102, "https://moodle.univ.fr/image.jpg?token=abc")

        assertEquals(key1, key2)
        assertTrue(key1 != key3)
    }

    @Test
    fun testCourseWithResolvedImageUrl() {
        val rawUrl = "https://moodle.univ.fr/pluginfile.php/123/course/overviewfiles/cover.jpg"
        val course = Course(
            id = 123,
            fullname = "Maths",
            shortname = "MATH101",
            overviewfiles = listOf(MoodleFile(rawUrl))
        )

        val token = "token123"
        val resolvedBeforeCache = course.withResolvedImageUrl(token)
        assertEquals("$rawUrl?token=$token", resolvedBeforeCache.imageUrl)

        val key = CourseImageCache.getCacheKey(123, "$rawUrl?token=$token")
        val savedPath = CacheStorage.saveBytes(key, byteArrayOf(10, 20, 30))
        assertNotNull(savedPath)

        val resolvedAfterCache = course.withResolvedImageUrl(token)
        assertEquals(savedPath, resolvedAfterCache.imageUrl)
    }
}
