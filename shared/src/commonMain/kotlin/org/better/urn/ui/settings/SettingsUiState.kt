package org.better.urn.ui.settings

import androidx.compose.runtime.Immutable
import org.better.urn.data.AppTheme
import org.better.urn.data.MoodleUser

/**
 * Immutable UI state representation for the Settings screen.
 */
@Immutable
data class SettingsUiState(
    val currentUser: MoodleUser? = null,
    val moodleUrl: String = "https://universitice.univ-rouen.fr",
    val theme: AppTheme = AppTheme.SYSTEM,
    val isLegalDialogOpen: Boolean = false,
    val isLicenseDialogOpen: Boolean = false,
    val isServerDialogOpen: Boolean = false
)
