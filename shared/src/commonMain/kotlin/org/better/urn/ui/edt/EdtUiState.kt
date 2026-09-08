package org.better.urn.ui.edt

import org.better.urn.data.EdtEvent

sealed interface EdtUiState {
    data object Loading : EdtUiState
    data class Success(val events: List<EdtEvent>) : EdtUiState
    data class Error(val message: String) : EdtUiState
}
