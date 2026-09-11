package org.better.urn.ui.settings

import androidx.compose.runtime.Immutable
import org.better.urn.data.AppTheme
import org.better.urn.data.EdtViewMode
import org.better.urn.data.EdtWeekDays
import org.better.urn.data.MoodleUser
import org.better.urn.ui.navigation.AppScreen

/**
 * Immutable UI state representation for the Settings screen.
 */
@Immutable
data class SettingsUiState(
    val currentUser: MoodleUser? = null,
    val isIzlyLoggedIn: Boolean = false,
    val izlyPhone: String? = null,
    val moodleUrl: String = "https://universitice.univ-rouen.fr",
    val theme: AppTheme = AppTheme.SYSTEM,
    val defaultTab: AppScreen = AppScreen.UNIVERSITICE,
    val edtDefaultView: EdtViewMode = EdtViewMode.AGENDA,
    val edtWeekDays: EdtWeekDays = EdtWeekDays.SEVEN,
    val isLegalDialogOpen: Boolean = false,
    val isLicenseDialogOpen: Boolean = false,
    val isServerDialogOpen: Boolean = false,
    val isEdtManagerOpen: Boolean = false
)
