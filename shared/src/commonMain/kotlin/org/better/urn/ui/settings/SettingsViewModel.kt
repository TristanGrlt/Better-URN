package org.better.urn.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.better.urn.data.AppTheme
import org.better.urn.data.CacheStorage
import org.better.urn.data.EdtViewMode
import org.better.urn.data.UserPreferences
import org.better.urn.ui.navigation.AppScreen

/**
 * ViewModel managing presentation logic and user intent processing for the Settings screen.
 */
class SettingsViewModel(
    private val preferences: UserPreferences = UserPreferences()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            currentUser = preferences.cachedUser,
            moodleUrl = preferences.moodleUrl,
            theme = preferences.appTheme,
            defaultTab = preferences.defaultTab,
            edtDefaultView = preferences.edtDefaultView,
            isLegalDialogOpen = false,
            isServerDialogOpen = false
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onThemeChanged(theme: AppTheme) {
        preferences.appTheme = theme
        _uiState.value = _uiState.value.copy(theme = theme)
    }

    fun onDefaultTabChanged(tab: AppScreen) {
        preferences.defaultTab = tab
        _uiState.value = _uiState.value.copy(defaultTab = tab)
    }

    fun onEdtDefaultViewChanged(mode: EdtViewMode) {
        preferences.edtDefaultView = mode
        _uiState.value = _uiState.value.copy(edtDefaultView = mode)
    }

    fun onServerUrlChanged(url: String) {
        preferences.moodleUrl = url
        _uiState.value = _uiState.value.copy(moodleUrl = url)
    }

    fun onClearCacheClicked() {
        CacheStorage.clear()
        _uiState.value = _uiState.value.copy(currentUser = preferences.cachedUser)
    }

    fun onLogoutClicked() {
        preferences.logout()
        _uiState.value = _uiState.value.copy(currentUser = null)
    }

    fun onToggleLegalDialog(isOpen: Boolean? = null) {
        val nextState = isOpen ?: !_uiState.value.isLegalDialogOpen
        _uiState.value = _uiState.value.copy(isLegalDialogOpen = nextState)
    }

    fun onToggleLicenseDialog(isOpen: Boolean? = null) {
        val nextState = isOpen ?: !_uiState.value.isLicenseDialogOpen
        _uiState.value = _uiState.value.copy(isLicenseDialogOpen = nextState)
    }

    fun onToggleServerDialog(isOpen: Boolean? = null) {
        val nextState = isOpen ?: !_uiState.value.isServerDialogOpen
        _uiState.value = _uiState.value.copy(isServerDialogOpen = nextState)
    }

    fun refreshState() {
        _uiState.value = _uiState.value.copy(
            currentUser = preferences.cachedUser,
            moodleUrl = preferences.moodleUrl,
            theme = preferences.appTheme,
            defaultTab = preferences.defaultTab,
            edtDefaultView = preferences.edtDefaultView
        )
    }
}
