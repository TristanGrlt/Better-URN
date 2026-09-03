package org.better.urn.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.better.urn.data.MoodleClient
import org.better.urn.data.UserPreferences

class UniversiticeViewModel {
    private val preferences = UserPreferences()
    private val scope = CoroutineScope(Dispatchers.Main)
    
    // Initialisation synchrone basée sur la présence du token
    private val _uiState = MutableStateFlow(
        UniversiticeUiState(isLogged = preferences.moodleToken.isNotBlank())
    )
    val uiState: StateFlow<UniversiticeUiState> = _uiState.asStateFlow()

    init {
        val currentToken = preferences.moodleToken
        val currentUrl = preferences.moodleUrl
        if (currentToken.isNotBlank()) {
            fetchData(currentUrl, currentToken)
        }
    }

    fun login(url: String, token: String) {
        _uiState.value = _uiState.value.copy(
            isLogged = true,
            errorMessage = null
        )
        fetchData(url, token)
    }

    private fun fetchData(url: String, token: String) {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val client = MoodleClient(url, token)
                val fetchedUser = client.getUserProfile()
                val fetchedCourses = client.getEnrolledCourses(fetchedUser.userid)
                
                preferences.moodleUrl = url
                preferences.moodleToken = token
                
                _uiState.value = _uiState.value.copy(
                    user = fetchedUser,
                    courses = fetchedCourses,
                    isLogged = true,
                    isLoading = false
                )
            } catch (e: Exception) {
                preferences.moodleToken = ""
                _uiState.value = _uiState.value.copy(
                    isLogged = false,
                    isLoading = false,
                    errorMessage = "Session expirée ou erreur réseau. Veuillez vous reconnecter."
                )
            }
        }
    }
}
