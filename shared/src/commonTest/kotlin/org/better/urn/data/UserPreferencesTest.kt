package org.better.urn.data

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserPreferencesTest {

    private val preferences = UserPreferences()

    @BeforeTest
    @AfterTest
    fun cleanup() {
        preferences.logout()
        preferences.appTheme = AppTheme.SYSTEM
    }

    @Test
    fun testAppThemePersistence() {
        assertEquals(AppTheme.SYSTEM, preferences.appTheme)

        preferences.appTheme = AppTheme.DARK
        assertEquals(AppTheme.DARK, preferences.appTheme)

        val newPrefs = UserPreferences()
        assertEquals(AppTheme.DARK, newPrefs.appTheme)

        preferences.appTheme = AppTheme.LIGHT
        assertEquals(AppTheme.LIGHT, preferences.appTheme)
        assertEquals(AppTheme.LIGHT, newPrefs.appTheme)
    }

    @Test
    fun testLogoutClearsSensitiveDataAndCache() {
        preferences.moodleToken = "secret_moodle_token_999"
        preferences.moodlePassport = "passport_12345"

        val user = MoodleUser(
            userid = 42,
            fullname = "John Doe",
            userpictureurl = "https://example.com/avatar.jpg"
        )
        preferences.cachedUser = user

        val courses = listOf(
            Course(id = 101, fullname = "Mathematics", shortname = "MATH101"),
            Course(id = 102, fullname = "Physics", shortname = "PHYS101")
        )
        preferences.cachedCourses = courses

        CacheStorage.saveString("custom_cache_entry", "sensitive_data")
        preferences.setCachedCourseSections(101, listOf(CourseSection(id = 1, name = "Section 1")))

        assertEquals("secret_moodle_token_999", preferences.moodleToken)
        assertEquals("secret_moodle_token_999", SecureStorage.getSecureString("moodle_token"))
        assertEquals("passport_12345", preferences.moodlePassport)
        assertEquals(user.userid, preferences.cachedUser?.userid)
        assertEquals(2, preferences.cachedCourses.size)
        assertEquals("sensitive_data", CacheStorage.getString("custom_cache_entry"))

        preferences.logout()

        assertEquals("", preferences.moodleToken)
        assertNull(SecureStorage.getSecureString("moodle_token"))
        assertNull(preferences.moodlePassport)
        assertNull(preferences.cachedUser)
        assertTrue(preferences.cachedCourses.isEmpty())
        assertNull(CacheStorage.getString("cached_user"))
        assertNull(CacheStorage.getString("cached_courses"))
        assertNull(CacheStorage.getString("custom_cache_entry"))
        assertTrue(preferences.getCachedCourseSections(101).isEmpty())
    }

    @Test
    fun testLogoutPreservesAppTheme() {
        preferences.appTheme = AppTheme.DARK
        preferences.moodleToken = "token_to_clear"

        preferences.logout()

        assertEquals("", preferences.moodleToken)
        assertEquals(AppTheme.DARK, preferences.appTheme)
    }
}
