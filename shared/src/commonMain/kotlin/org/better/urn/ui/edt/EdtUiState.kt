package org.better.urn.ui.edt

import org.better.urn.data.EdtEvent

sealed interface EdtUiState {
    data object Loading : EdtUiState
    data class Success(
        val events: List<EdtEvent>,
        val isRefreshing: Boolean = false,
        val lastSyncTimestamp: Long? = null,
        val refreshError: String? = null
    ) : EdtUiState
    data class Error(val message: String) : EdtUiState
}
