package org.better.urn.ui.settings

import org.better.urn.data.AppTheme
import org.better.urn.data.CacheStorage
import org.better.urn.data.MoodleUser
import org.better.urn.data.UserPreferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsViewModelTest {

    private val preferences = UserPreferences()

    @BeforeTest
    @AfterTest
    fun cleanup() {
        preferences.logout()
        preferences.appTheme = AppTheme.SYSTEM
        preferences.moodleUrl = "https://universitice.univ-rouen.fr"
        CacheStorage.clear()
    }

    @Test
    fun testInitialStateReadsFromPreferences() {
        val user = MoodleUser(userid = 100, fullname = "Alice Dupont", userpictureurl = "https://example.com/pic.jpg")
        preferences.cachedUser = user
        preferences.moodleUrl = "https://custom.moodle.org"
        preferences.appTheme = AppTheme.DARK

        val viewModel = SettingsViewModel(preferences)
        val state = viewModel.uiState.value

        assertEquals(user, state.currentUser)
        assertEquals("https://custom.moodle.org", state.moodleUrl)
        assertEquals(AppTheme.DARK, state.theme)
        assertFalse(state.isLegalDialogOpen)
        assertFalse(state.isServerDialogOpen)
    }

    @Test
    fun testOnThemeChangedUpdatesStateAndPreferences() {
        val viewModel = SettingsViewModel(preferences)

        viewModel.onThemeChanged(AppTheme.DARK)
        assertEquals(AppTheme.DARK, viewModel.uiState.value.theme)
        assertEquals(AppTheme.DARK, preferences.appTheme)

        viewModel.onThemeChanged(AppTheme.LIGHT)
        assertEquals(AppTheme.LIGHT, viewModel.uiState.value.theme)
        assertEquals(AppTheme.LIGHT, preferences.appTheme)

        viewModel.onThemeChanged(AppTheme.SYSTEM)
        assertEquals(AppTheme.SYSTEM, viewModel.uiState.value.theme)
        assertEquals(AppTheme.SYSTEM, preferences.appTheme)
    }

    @Test
    fun testOnServerUrlChangedUpdatesStateAndPreferences() {
        val viewModel = SettingsViewModel(preferences)

        val newUrl = "https://moodle.univ-rouen.fr"
        viewModel.onServerUrlChanged(newUrl)

        assertEquals(newUrl, viewModel.uiState.value.moodleUrl)
        assertEquals(newUrl, preferences.moodleUrl)
    }

    @Test
    fun testOnClearCacheClickedClearsCacheStorageAndUpdatesState() {
        CacheStorage.saveString("test_cache_entry", "data_content")
        val user = MoodleUser(userid = 200, fullname = "Bob Martin", userpictureurl = "")
        preferences.cachedUser = user

        val viewModel = SettingsViewModel(preferences)
        assertEquals(user, viewModel.uiState.value.currentUser)

        viewModel.onClearCacheClicked()

        assertNull(CacheStorage.getString("test_cache_entry"))
        assertNull(preferences.cachedUser)
        assertNull(viewModel.uiState.value.currentUser)
    }

    @Test
    fun testOnLogoutClickedClearsPreferencesAndUpdatesState() {
        preferences.moodleToken = "token_abc_123"
        val user = MoodleUser(userid = 300, fullname = "Charlie Brown", userpictureurl = "")
        preferences.cachedUser = user
        CacheStorage.saveString("custom_cache_key", "cached_value")

        val viewModel = SettingsViewModel(preferences)
        assertEquals(user, viewModel.uiState.value.currentUser)

        viewModel.onLogoutClicked()

        assertEquals("", preferences.moodleToken)
        assertNull(preferences.cachedUser)
        assertNull(CacheStorage.getString("custom_cache_key"))
        assertNull(viewModel.uiState.value.currentUser)
    }

    @Test
    fun testOnToggleLegalDialogTogglesAndExplicitlySetsState() {
        val viewModel = SettingsViewModel(preferences)

        assertFalse(viewModel.uiState.value.isLegalDialogOpen)

        viewModel.onToggleLegalDialog()
        assertTrue(viewModel.uiState.value.isLegalDialogOpen)

        viewModel.onToggleLegalDialog()
        assertFalse(viewModel.uiState.value.isLegalDialogOpen)

        viewModel.onToggleLegalDialog(true)
        assertTrue(viewModel.uiState.value.isLegalDialogOpen)

        viewModel.onToggleLegalDialog(false)
        assertFalse(viewModel.uiState.value.isLegalDialogOpen)
    }

    @Test
    fun testOnToggleLicenseDialogTogglesAndExplicitlySetsState() {
        val viewModel = SettingsViewModel(preferences)

        assertFalse(viewModel.uiState.value.isLicenseDialogOpen)

        viewModel.onToggleLicenseDialog()
        assertTrue(viewModel.uiState.value.isLicenseDialogOpen)

        viewModel.onToggleLicenseDialog()
        assertFalse(viewModel.uiState.value.isLicenseDialogOpen)

        viewModel.onToggleLicenseDialog(true)
        assertTrue(viewModel.uiState.value.isLicenseDialogOpen)

        viewModel.onToggleLicenseDialog(false)
        assertFalse(viewModel.uiState.value.isLicenseDialogOpen)
    }

    @Test
    fun testOnToggleServerDialogTogglesAndExplicitlySetsState() {
        val viewModel = SettingsViewModel(preferences)

        assertFalse(viewModel.uiState.value.isServerDialogOpen)

        viewModel.onToggleServerDialog()
        assertTrue(viewModel.uiState.value.isServerDialogOpen)

        viewModel.onToggleServerDialog()
        assertFalse(viewModel.uiState.value.isServerDialogOpen)

        viewModel.onToggleServerDialog(true)
        assertTrue(viewModel.uiState.value.isServerDialogOpen)

        viewModel.onToggleServerDialog(false)
        assertFalse(viewModel.uiState.value.isServerDialogOpen)
    }

    @Test
    fun testRefreshStateResyncsWithPreferences() {
        val viewModel = SettingsViewModel(preferences)

        preferences.appTheme = AppTheme.DARK
        preferences.moodleUrl = "https://updated.moodle.com"
        val user = MoodleUser(userid = 400, fullname = "David Guetta", userpictureurl = "")
        preferences.cachedUser = user

        viewModel.refreshState()

        val state = viewModel.uiState.value
        assertEquals(AppTheme.DARK, state.theme)
        assertEquals("https://updated.moodle.com", state.moodleUrl)
        assertEquals(user, state.currentUser)
    }
}
