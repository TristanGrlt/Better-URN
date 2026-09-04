package org.better.urn

import org.better.urn.data.UserPreferences
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopStorageTest {

    @Test
    fun testTokenPersistenceAcrossInstances() {
        val prefs1 = UserPreferences()
        prefs1.moodleUrl = "https://test.moodle.fr"
        prefs1.moodleToken = "desktop_token_987654321"

        // Create a new instance representing a new app launch
        val prefs2 = UserPreferences()
        assertEquals("https://test.moodle.fr", prefs2.moodleUrl)
        assertEquals("desktop_token_987654321", prefs2.moodleToken)
    }
}
