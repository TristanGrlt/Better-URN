package org.better.urn.data.izly

/**
 * Represents a single financial transaction on an Izly account.
 */
data class IzlyOperation(
    val id: String,
    val amount: Float,
    val date: String,
    val type: String,
    val isCredit: Boolean,
)

/**
 * Sealed interface representing the authentication states of the Izly service.
 */
sealed interface IzlyAuthState {
    data object Idle : IzlyAuthState
    data object Loading : IzlyAuthState
    data class ActivationRequired(val phone: String) : IzlyAuthState
    data object LoggedIn : IzlyAuthState
    data class Error(val message: String) : IzlyAuthState
}
