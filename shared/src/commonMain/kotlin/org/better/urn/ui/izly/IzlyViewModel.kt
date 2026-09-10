package org.better.urn.ui.izly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.better.urn.data.izly.IzlyAuthState
import org.better.urn.data.izly.IzlyRepository

/**
 * ViewModel managing presentation logic and authentication state for Izly features.
 */
class IzlyViewModel(
    private val repository: IzlyRepository = IzlyRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(IzlyUiState())
    val uiState: StateFlow<IzlyUiState> = _uiState.asStateFlow()

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        viewModelScope.launch {
            if (repository.hasValidSession()) {
                _uiState.update { it.copy(authState = IzlyAuthState.LoggedIn) }
                fetchHistory()
            }
        }
    }

    fun onPhoneChanged(phone: String) {
        _uiState.update { it.copy(phoneInput = phone, errorMessage = null) }
    }

    fun onPinChanged(pin: String) {
        _uiState.update { it.copy(pinInput = pin, errorMessage = null) }
    }

    fun onActivationLinkChanged(link: String) {
        _uiState.update { it.copy(activationLinkInput = link, errorMessage = null) }
    }

    fun login() {
        val phone = _uiState.value.phoneInput.trim()
        val pin = _uiState.value.pinInput.trim()

        if (phone.isBlank() || pin.isBlank()) {
            val error = "Veuillez remplir tous les champs"
            _uiState.update {
                it.copy(
                    authState = IzlyAuthState.Error(error),
                    errorMessage = error,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    authState = IzlyAuthState.Loading,
                    errorMessage = null,
                )
            }

            repository.login(phone, pin)
                .onSuccess { activationRequired ->
                    if (activationRequired) {
                        _uiState.update { state ->
                            state.copy(
                                authState = IzlyAuthState.ActivationRequired(phone),
                                errorMessage = null,
                            )
                        }
                    } else {
                        _uiState.update { state ->
                            state.copy(
                                authState = IzlyAuthState.LoggedIn,
                                errorMessage = null,
                            )
                        }
                        fetchHistory()
                    }
                }
                .onFailure { throwable ->
                    val message = throwable.message ?: "Échec de la connexion à Izly"
                    _uiState.update { state ->
                        state.copy(
                            authState = IzlyAuthState.Error(message),
                            errorMessage = message,
                        )
                    }
                }
        }
    }

    fun submitActivationLink(link: String = _uiState.value.activationLinkInput) {
        val trimmedLink = link.trim()
        if (trimmedLink.isBlank()) {
            val error = "Veuillez coller ou saisir le lien d'activation SMS"
            _uiState.update {
                it.copy(
                    errorMessage = error,
                )
            }
            return
        }

        val currentPhone = when (val state = _uiState.value.authState) {
            is IzlyAuthState.ActivationRequired -> state.phone
            else -> _uiState.value.phoneInput
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    authState = IzlyAuthState.Loading,
                    errorMessage = null,
                )
            }

            repository.tokenize(trimmedLink)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            authState = IzlyAuthState.LoggedIn,
                            errorMessage = null,
                        )
                    }
                    fetchHistory()
                }
                .onFailure { throwable ->
                    val message = throwable.message ?: "Échec de l'activation de l'appareil"
                    _uiState.update { state ->
                        state.copy(
                            authState = IzlyAuthState.ActivationRequired(currentPhone),
                            errorMessage = message,
                        )
                    }
                }
        }
    }

    fun fetchHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingHistory = true) }

            repository.getBalance().onSuccess { bal ->
                _uiState.update { it.copy(balance = bal) }
            }

            repository.getHistory()
                .onSuccess { list ->
                    _uiState.update { state ->
                        state.copy(
                            operations = list,
                            isRefreshingHistory = false,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isRefreshingHistory = false,
                            errorMessage = throwable.message ?: "Impossible de charger l'historique",
                        )
                    }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.update {
                IzlyUiState(
                    phoneInput = "",
                    pinInput = "",
                    activationLinkInput = "",
                    authState = IzlyAuthState.Idle,
                )
            }
        }
    }
}
