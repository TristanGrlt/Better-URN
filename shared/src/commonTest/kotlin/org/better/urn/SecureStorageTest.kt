package org.better.urn

import org.better.urn.data.SecureStorage
import org.better.urn.data.UserPreferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecureStorageTest {

    @BeforeTest
    @AfterTest
    fun cleanup() {
        SecureStorage.clear()
        UserPreferences().moodleToken = ""
    }

    @Test
    fun testSaveAndRetrieveSecureToken() {
        val testToken = "test_moodle_token_1234567890abcdef"
        val key = "moodle_token"

        SecureStorage.saveSecureString(key, testToken)
        val retrieved = SecureStorage.getSecureString(key)

        assertEquals(testToken, retrieved)
    }

    @Test
    fun testRemoveSecureToken() {
        val testToken = "test_token_to_remove"
        val key = "sample_key"

        SecureStorage.saveSecureString(key, testToken)
        assertEquals(testToken, SecureStorage.getSecureString(key))

        SecureStorage.removeSecureString(key)
        assertNull(SecureStorage.getSecureString(key))
    }

    @Test
    fun testUserPreferencesTokenIntegration() {
        val prefs = UserPreferences()
        assertEquals("", prefs.moodleToken)

        val token = "secure_token_abc_xyz_999"
        prefs.moodleToken = token
        assertEquals(token, prefs.moodleToken)

        // Verify token is saved in SecureStorage
        assertEquals(token, SecureStorage.getSecureString("moodle_token"))

        // Reset token
        prefs.moodleToken = ""
        assertEquals("", prefs.moodleToken)
        assertTrue(SecureStorage.getSecureString("moodle_token").isNullOrEmpty())
    }

    @Test
    fun testClearSecureStorage() {
        SecureStorage.saveSecureString("key1", "val1")
        SecureStorage.saveSecureString("key2", "val2")

        SecureStorage.clear()

        assertNull(SecureStorage.getSecureString("key1"))
        assertNull(SecureStorage.getSecureString("key2"))
    }
}
