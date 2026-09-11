package org.better.urn.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.better.urn.data.AppTheme
import org.better.urn.data.CacheStorage
import org.better.urn.data.EdtViewMode
import org.better.urn.data.EdtWeekDays
import org.better.urn.data.UserPreferences
import org.better.urn.data.izly.IzlyRepository
import org.better.urn.ui.navigation.AppScreen

/**
 * ViewModel managing presentation logic and user intent processing for the Settings screen.
 */
class SettingsViewModel(
    private val preferences: UserPreferences = UserPreferences(),
    private val izlyRepository: IzlyRepository = IzlyRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            currentUser = preferences.cachedUser,
            isIzlyLoggedIn = false,
            izlyPhone = izlyRepository.getSavedPhone(),
            moodleUrl = preferences.moodleUrl,
            theme = preferences.appTheme,
            defaultTab = preferences.defaultTab,
            edtDefaultView = preferences.edtDefaultView,
            edtWeekDays = preferences.edtWeekDays,
            isLegalDialogOpen = false,
            isServerDialogOpen = false
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshState()
    }

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

    fun onEdtWeekDaysChanged(weekDays: EdtWeekDays) {
        preferences.edtWeekDays = weekDays
        _uiState.value = _uiState.value.copy(edtWeekDays = weekDays)
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

    fun onIzlyLogoutClicked() {
        viewModelScope.launch {
            izlyRepository.logout()
            _uiState.value = _uiState.value.copy(
                isIzlyLoggedIn = false,
                izlyPhone = null
            )
        }
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

    fun onToggleEdtManager(isOpen: Boolean? = null) {
        val nextState = isOpen ?: !_uiState.value.isEdtManagerOpen
        _uiState.value = _uiState.value.copy(isEdtManagerOpen = nextState)
    }

    fun refreshState() {
        viewModelScope.launch {
            val isIzlyValid = izlyRepository.hasValidSession()
            val phone = izlyRepository.getSavedPhone()
            _uiState.value = _uiState.value.copy(
                currentUser = preferences.cachedUser,
                isIzlyLoggedIn = isIzlyValid,
                izlyPhone = phone,
                moodleUrl = preferences.moodleUrl,
                theme = preferences.appTheme,
                defaultTab = preferences.defaultTab,
                edtDefaultView = preferences.edtDefaultView,
                edtWeekDays = preferences.edtWeekDays
            )
        }
    }
}
