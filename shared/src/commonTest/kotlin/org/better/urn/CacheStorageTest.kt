package org.better.urn

import org.better.urn.data.CacheStorage
import org.better.urn.data.Course
import org.better.urn.data.CourseModule
import org.better.urn.data.CourseSection
import org.better.urn.data.UserPreferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CacheStorageTest {

    @AfterTest
    fun cleanup() {
        CacheStorage.clear()
    }

    @Test
    fun testCacheStorageHandlesLargePayload() {
        // Create a large string exceeding 8 KB (100 KB string)
        val largePayload = "A".repeat(100 * 1024)
        val key = "test_large_key"

        CacheStorage.saveString(key, largePayload)
        val retrieved = CacheStorage.getString(key)

        assertNotNull(retrieved)
        assertEquals(largePayload.length, retrieved.length)
        assertEquals(largePayload, retrieved)

        CacheStorage.remove(key)
        assertNull(CacheStorage.getString(key))
    }

    @Test
    fun testUserPreferencesLargeCourseCaching() {
        val userPrefs = UserPreferences()

        // Generate a list of 50 courses with long titles and shortnames
        val largeCourseList = (1..50).map { id ->
            Course(
                id = id,
                fullname = "Cours Très Long Nom #$id - " + "Info ".repeat(50),
                shortname = "CS$id - " + "Short ".repeat(20),
            )
        }

        userPrefs.cachedCourses = largeCourseList
        val loadedCourses = userPrefs.cachedCourses

        assertEquals(50, loadedCourses.size)
        assertEquals(largeCourseList.first().fullname, loadedCourses.first().fullname)

        // Generate sections for a course exceeding 8 KB
        val largeSections = (1..20).map { secId ->
            CourseSection(
                id = secId,
                name = "Section $secId - " + "Titre ".repeat(20),
                summary = "Résumé de section " + "Texte ".repeat(100),
                modules = (1..10).map { modId ->
                    CourseModule(
                        id = (secId * 100) + modId,
                        name = "Module #$modId " + "Description ".repeat(30),
                        modname = "resource",
                        url = "https://universitice.univ-rouen.fr/mod/resource/view.php?id=${secId * 100 + modId}"
                    )
                }
            )
        }

        userPrefs.setCachedCourseSections(101, largeSections)
        val loadedSections = userPrefs.getCachedCourseSections(101)

        assertEquals(20, loadedSections.size)
        assertEquals(largeSections.first().name, loadedSections.first().name)
        assertEquals(10, loadedSections.first().modules.size)
    }
}
