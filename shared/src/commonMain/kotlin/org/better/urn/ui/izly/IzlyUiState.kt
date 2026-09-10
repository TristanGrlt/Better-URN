package org.better.urn.ui.izly

import org.better.urn.data.izly.IzlyAuthState
import org.better.urn.data.izly.IzlyOperation

/**
 * Immutable UI state model for the Izly screen.
 */
data class IzlyUiState(
    val authState: IzlyAuthState = IzlyAuthState.Idle,
    val phoneInput: String = "",
    val pinInput: String = "",
    val activationLinkInput: String = "",
    val balance: Float = 0f,
    val operations: List<IzlyOperation> = emptyList(),
    val isRefreshingHistory: Boolean = false,
    val errorMessage: String? = null,
) {
    val totalBalance: Float
        get() = balance
}
