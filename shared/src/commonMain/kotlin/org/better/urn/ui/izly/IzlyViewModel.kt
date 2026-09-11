package org.better.urn.ui.izly

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.better.urn.data.izly.IzlyAuthState
import org.better.urn.data.izly.IzlyRepository

/**
 * ViewModel managing presentation logic and authentication state for Izly features.
 * Supports state preservation across system-initiated process death via SavedStateHandle
 * and fallback restoration from persistent SecureStorage in IzlyRepository.
 */
class IzlyViewModel(
    private val repository: IzlyRepository = IzlyRepository(),
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    companion object {
        private const val KEY_PHONE_INPUT = "izly_phone_input"
        private const val KEY_PIN_INPUT = "izly_pin_input"
        private const val KEY_ACTIVATION_LINK_INPUT = "izly_activation_link_input"
        private const val KEY_IS_ACTIVATION_REQUIRED = "izly_is_activation_required"
        private const val KEY_ACTIVATION_PHONE = "izly_activation_phone"
    }

    private var isTokenizing = false

    private val _uiState = MutableStateFlow(
        IzlyUiState(
            phoneInput = savedStateHandle.get<String>(KEY_PHONE_INPUT) ?: "",
            pinInput = savedStateHandle.get<String>(KEY_PIN_INPUT) ?: "",
            activationLinkInput = savedStateHandle.get<String>(KEY_ACTIVATION_LINK_INPUT) ?: "",
        )
    )
    val uiState: StateFlow<IzlyUiState> = _uiState.asStateFlow()

    init {
        restoreStateAndCheckSession()
    }

    private fun restoreStateAndCheckSession() {
        viewModelScope.launch {
            if (repository.hasValidSession()) {
                _uiState.update { it.copy(authState = IzlyAuthState.LoggedIn) }
                fetchHistory()
                return@launch
            }

            // 1. Try restoring from SavedStateHandle
            val savedIsActivationRequired = savedStateHandle.get<Boolean>(KEY_IS_ACTIVATION_REQUIRED) == true
            val savedActivationPhone = savedStateHandle.get<String>(KEY_ACTIVATION_PHONE)
            val savedPhoneInput = savedStateHandle.get<String>(KEY_PHONE_INPUT)

            // 2. Fallback: query repository persistent storage
            val persistentPhone = repository.getSavedPhone()

            val effectivePhone = savedActivationPhone
                ?.takeIf { it.isNotBlank() }
                ?: savedPhoneInput?.takeIf { it.isNotBlank() }
                ?: persistentPhone
                ?: ""

            if (savedIsActivationRequired || persistentPhone != null) {
                if (effectivePhone.isNotBlank()) {
                    _uiState.update { state ->
                        state.copy(
                            phoneInput = state.phoneInput.ifBlank { effectivePhone },
                            authState = IzlyAuthState.ActivationRequired(effectivePhone),
                        )
                    }
                }
            }
        }
    }

    fun onPhoneChanged(phone: String) {
        savedStateHandle[KEY_PHONE_INPUT] = phone
        _uiState.update { it.copy(phoneInput = phone, errorMessage = null) }
    }

    fun onPinChanged(pin: String) {
        savedStateHandle[KEY_PIN_INPUT] = pin
        _uiState.update { it.copy(pinInput = pin, errorMessage = null) }
    }

    fun onActivationLinkChanged(link: String) {
        savedStateHandle[KEY_ACTIVATION_LINK_INPUT] = link
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
                        savedStateHandle[KEY_IS_ACTIVATION_REQUIRED] = true
                        savedStateHandle[KEY_ACTIVATION_PHONE] = phone
                        savedStateHandle[KEY_PHONE_INPUT] = phone
                        repository.savePhone(phone)

                        _uiState.update { state ->
                            state.copy(
                                authState = IzlyAuthState.ActivationRequired(phone),
                                phoneInput = phone,
                                errorMessage = null,
                            )
                        }
                    } else {
                        savedStateHandle.remove<Boolean>(KEY_IS_ACTIVATION_REQUIRED)
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
        if (isTokenizing) return
        isTokenizing = true

        val trimmedLink = link.trim()
        if (trimmedLink.isBlank()) {
            isTokenizing = false
            val error = "Veuillez coller ou saisir le lien d'activation SMS"
            _uiState.update {
                it.copy(
                    errorMessage = error,
                )
            }
            return
        }

        var currentPhone = when (val state = _uiState.value.authState) {
            is IzlyAuthState.ActivationRequired -> state.phone
            else -> savedStateHandle.get<String>(KEY_ACTIVATION_PHONE)
                ?: savedStateHandle.get<String>(KEY_PHONE_INPUT)
                ?: _uiState.value.phoneInput
        }.trim()

        if (currentPhone.isBlank()) {
            currentPhone = extractPhoneFromLink(trimmedLink)
                ?: repository.getSavedPhone()
                ?: ""
        }

        val phoneToPreserve = currentPhone

        if (phoneToPreserve.isNotBlank()) {
            savedStateHandle[KEY_IS_ACTIVATION_REQUIRED] = true
            savedStateHandle[KEY_ACTIVATION_PHONE] = phoneToPreserve
            savedStateHandle[KEY_PHONE_INPUT] = phoneToPreserve
            repository.savePhone(phoneToPreserve)
        }

        viewModelScope.launch {
            try {
                _uiState.update { state ->
                    state.copy(
                        authState = IzlyAuthState.Loading,
                        phoneInput = state.phoneInput.ifBlank { phoneToPreserve },
                        errorMessage = null,
                    )
                }

                repository.tokenize(trimmedLink)
                    .onSuccess {
                        savedStateHandle.remove<Boolean>(KEY_IS_ACTIVATION_REQUIRED)
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
                                authState = IzlyAuthState.ActivationRequired(phoneToPreserve),
                                phoneInput = state.phoneInput.ifBlank { phoneToPreserve },
                                errorMessage = message,
                            )
                        }
                    }
            } finally {
                delay(2000L)
                isTokenizing = false
            }
        }
    }

    private fun extractPhoneFromLink(link: String): String? {
        val cleanLink = link.trim().substringBefore("?").trimEnd('/')
        val pathSegments = cleanLink.split("/")
        if (pathSegments.size >= 2) {
            val candidate = pathSegments[pathSegments.size - 2]
            if (candidate.all { it.isDigit() || it == '+' } && candidate.length >= 8) {
                return candidate
            }
        }
        return null
    }

    fun fetchHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingHistory = true, errorMessage = null) }

            val balanceResult = repository.getBalance()
            val historyResult = repository.getHistory()

            var newBalance = _uiState.value.balance
            var balanceError: String? = null

            balanceResult
                .onSuccess { bal -> newBalance = bal }
                .onFailure { err -> balanceError = err.message }

            var newOperations = _uiState.value.operations
            var historyError: String? = null

            historyResult
                .onSuccess { list -> newOperations = list }
                .onFailure { err -> historyError = err.message }

            val combinedError = balanceError ?: historyError

            val isSessionExpired = combinedError?.let { msg ->
                msg.contains("Non autorisé", ignoreCase = true) ||
                msg.contains("session", ignoreCase = true) ||
                msg.contains("expir", ignoreCase = true) ||
                msg.contains("token", ignoreCase = true) ||
                msg.contains("invalide", ignoreCase = true)
            } == true

            if (isSessionExpired) {
                logout()
            } else {
                _uiState.update { state ->
                    state.copy(
                        balance = newBalance,
                        operations = newOperations,
                        isRefreshingHistory = false,
                        errorMessage = combinedError
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            savedStateHandle.remove<String>(KEY_PHONE_INPUT)
            savedStateHandle.remove<String>(KEY_PIN_INPUT)
            savedStateHandle.remove<String>(KEY_ACTIVATION_LINK_INPUT)
            savedStateHandle.remove<Boolean>(KEY_IS_ACTIVATION_REQUIRED)
            savedStateHandle.remove<String>(KEY_ACTIVATION_PHONE)
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
