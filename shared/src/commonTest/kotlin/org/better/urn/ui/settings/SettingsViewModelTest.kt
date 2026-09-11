package org.better.urn.ui.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.better.urn.data.AppTheme
import org.better.urn.data.CacheStorage
import org.better.urn.data.EdtViewMode
import org.better.urn.data.EdtWeekDays
import org.better.urn.data.MoodleUser
import org.better.urn.data.UserPreferences
import org.better.urn.data.izly.IzlyOperation
import org.better.urn.data.izly.IzlyRepository
import org.better.urn.ui.navigation.AppScreen
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeSettingsIzlyRepository(
    var isSessionValid: Boolean = false,
    var phone: String? = null
) : IzlyRepository {
    override suspend fun hasValidSession(): Boolean = isSessionValid
    override suspend fun login(phone: String, pin: String): Result<Boolean> = Result.success(true)
    override suspend fun tokenize(smsLink: String): Result<Unit> = Result.success(Unit)
    override suspend fun getBalance(): Result<Float> = Result.success(0f)
    override suspend fun getHistory(): Result<List<IzlyOperation>> = Result.success(emptyList())
    override suspend fun logout() {
        isSessionValid = false
        phone = null
    }
    override fun getSavedPhone(): String? = phone
    override fun savePhone(phone: String) { this.phone = phone }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val preferences = UserPreferences()
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        cleanup()
    }

    @AfterTest
    fun tearDown() {
        cleanup()
        Dispatchers.resetMain()
    }

    private fun cleanup() {
        preferences.logout()
        preferences.appTheme = AppTheme.SYSTEM
        preferences.defaultTab = AppScreen.UNIVERSITICE
        preferences.edtDefaultView = EdtViewMode.AGENDA
        preferences.edtWeekDays = EdtWeekDays.SEVEN
        preferences.moodleUrl = "https://universitice.univ-rouen.fr"
        CacheStorage.clear()
    }

    @Test
    fun testInitialStateReadsFromPreferencesAndIzly() = runTest {
        val user = MoodleUser(userid = 100, fullname = "Alice Dupont", userpictureurl = "https://example.com/pic.jpg")
        preferences.cachedUser = user
        preferences.moodleUrl = "https://custom.moodle.org"
        preferences.appTheme = AppTheme.DARK
        preferences.defaultTab = AppScreen.EDT
        preferences.edtDefaultView = EdtViewMode.SEMAINE
        preferences.edtWeekDays = EdtWeekDays.FIVE

        val fakeIzlyRepo = FakeSettingsIzlyRepository(isSessionValid = true, phone = "0601020304")
        val viewModel = SettingsViewModel(preferences, fakeIzlyRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(user, state.currentUser)
        assertTrue(state.isIzlyLoggedIn)
        assertEquals("0601020304", state.izlyPhone)
        assertEquals("https://custom.moodle.org", state.moodleUrl)
        assertEquals(AppTheme.DARK, state.theme)
        assertEquals(AppScreen.EDT, state.defaultTab)
        assertEquals(EdtViewMode.SEMAINE, state.edtDefaultView)
        assertEquals(EdtWeekDays.FIVE, state.edtWeekDays)
        assertFalse(state.isLegalDialogOpen)
        assertFalse(state.isServerDialogOpen)
    }

    @Test
    fun testOnIzlyLogoutClickedClearsIzlyState() = runTest {
        val fakeIzlyRepo = FakeSettingsIzlyRepository(isSessionValid = true, phone = "0601020304")
        val viewModel = SettingsViewModel(preferences, fakeIzlyRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isIzlyLoggedIn)

        viewModel.onIzlyLogoutClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isIzlyLoggedIn)
        assertNull(viewModel.uiState.value.izlyPhone)
    }

    @Test
    fun testOnThemeChangedUpdatesStateAndPreferences() {
        val viewModel = SettingsViewModel(preferences, FakeSettingsIzlyRepository())

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
    fun testOnDefaultTabChangedUpdatesStateAndPreferences() {
        val viewModel = SettingsViewModel(preferences, FakeSettingsIzlyRepository())

        viewModel.onDefaultTabChanged(AppScreen.EDT)
        assertEquals(AppScreen.EDT, viewModel.uiState.value.defaultTab)
        assertEquals(AppScreen.EDT, preferences.defaultTab)

        viewModel.onDefaultTabChanged(AppScreen.IZLY)
        assertEquals(AppScreen.IZLY, viewModel.uiState.value.defaultTab)
        assertEquals(AppScreen.IZLY, preferences.defaultTab)

        viewModel.onDefaultTabChanged(AppScreen.AUTRE)
        assertEquals(AppScreen.AUTRE, viewModel.uiState.value.defaultTab)
        assertEquals(AppScreen.AUTRE, preferences.defaultTab)

        viewModel.onDefaultTabChanged(AppScreen.UNIVERSITICE)
        assertEquals(AppScreen.UNIVERSITICE, viewModel.uiState.value.defaultTab)
        assertEquals(AppScreen.UNIVERSITICE, preferences.defaultTab)
    }

    @Test
    fun testOnEdtDefaultViewChangedUpdatesStateAndPreferences() {
        val viewModel = SettingsViewModel(preferences, FakeSettingsIzlyRepository())

        viewModel.onEdtDefaultViewChanged(EdtViewMode.SEMAINE)
        assertEquals(EdtViewMode.SEMAINE, viewModel.uiState.value.edtDefaultView)
        assertEquals(EdtViewMode.SEMAINE, preferences.edtDefaultView)

        viewModel.onEdtDefaultViewChanged(EdtViewMode.AGENDA)
        assertEquals(EdtViewMode.AGENDA, viewModel.uiState.value.edtDefaultView)
        assertEquals(EdtViewMode.AGENDA, preferences.edtDefaultView)
    }

    @Test
    fun testOnEdtWeekDaysChangedUpdatesStateAndPreferences() {
        val viewModel = SettingsViewModel(preferences, FakeSettingsIzlyRepository())

        viewModel.onEdtWeekDaysChanged(EdtWeekDays.FIVE)
        assertEquals(EdtWeekDays.FIVE, viewModel.uiState.value.edtWeekDays)
        assertEquals(EdtWeekDays.FIVE, preferences.edtWeekDays)

        viewModel.onEdtWeekDaysChanged(EdtWeekDays.SIX)
        assertEquals(EdtWeekDays.SIX, viewModel.uiState.value.edtWeekDays)
        assertEquals(EdtWeekDays.SIX, preferences.edtWeekDays)

        viewModel.onEdtWeekDaysChanged(EdtWeekDays.SEVEN)
        assertEquals(EdtWeekDays.SEVEN, viewModel.uiState.value.edtWeekDays)
        assertEquals(EdtWeekDays.SEVEN, preferences.edtWeekDays)
    }

    @Test
    fun testOnServerUrlChangedUpdatesStateAndPreferences() {
        val viewModel = SettingsViewModel(preferences, FakeSettingsIzlyRepository())

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

        val viewModel = SettingsViewModel(preferences, FakeSettingsIzlyRepository())
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

        val viewModel = SettingsViewModel(preferences, FakeSettingsIzlyRepository())
        assertEquals(user, viewModel.uiState.value.currentUser)

        viewModel.onLogoutClicked()

        assertEquals("", preferences.moodleToken)
        assertNull(preferences.cachedUser)
        assertNull(CacheStorage.getString("custom_cache_key"))
        assertNull(viewModel.uiState.value.currentUser)
    }

    @Test
    fun testOnToggleLegalDialogTogglesAndExplicitlySetsState() {
        val viewModel = SettingsViewModel(preferences, FakeSettingsIzlyRepository())

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
        val viewModel = SettingsViewModel(preferences, FakeSettingsIzlyRepository())

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
        val viewModel = SettingsViewModel(preferences, FakeSettingsIzlyRepository())

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
    fun testOnToggleEdtManagerTogglesAndExplicitlySetsState() {
        val viewModel = SettingsViewModel(preferences, FakeSettingsIzlyRepository())

        assertFalse(viewModel.uiState.value.isEdtManagerOpen)

        viewModel.onToggleEdtManager()
        assertTrue(viewModel.uiState.value.isEdtManagerOpen)

        viewModel.onToggleEdtManager()
        assertFalse(viewModel.uiState.value.isEdtManagerOpen)

        viewModel.onToggleEdtManager(true)
        assertTrue(viewModel.uiState.value.isEdtManagerOpen)

        viewModel.onToggleEdtManager(false)
        assertFalse(viewModel.uiState.value.isEdtManagerOpen)
    }

    @Test
    fun testRefreshStateResyncsWithPreferencesAndIzly() = runTest {
        val fakeIzlyRepo = FakeSettingsIzlyRepository(isSessionValid = false, phone = null)
        val viewModel = SettingsViewModel(preferences, fakeIzlyRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        preferences.appTheme = AppTheme.DARK
        preferences.defaultTab = AppScreen.EDT
        preferences.edtDefaultView = EdtViewMode.SEMAINE
        preferences.edtWeekDays = EdtWeekDays.SIX
        preferences.moodleUrl = "https://updated.moodle.com"
        val user = MoodleUser(userid = 400, fullname = "David Guetta", userpictureurl = "")
        preferences.cachedUser = user

        fakeIzlyRepo.isSessionValid = true
        fakeIzlyRepo.phone = "0699887766"

        viewModel.refreshState()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AppTheme.DARK, state.theme)
        assertEquals(AppScreen.EDT, state.defaultTab)
        assertEquals(EdtViewMode.SEMAINE, state.edtDefaultView)
        assertEquals(EdtWeekDays.SIX, state.edtWeekDays)
        assertEquals("https://updated.moodle.com", state.moodleUrl)
        assertEquals(user, state.currentUser)
        assertTrue(state.isIzlyLoggedIn)
        assertEquals("0699887766", state.izlyPhone)
    }
}
