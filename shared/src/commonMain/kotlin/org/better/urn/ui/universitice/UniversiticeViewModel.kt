package org.better.urn.ui.universitice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.better.urn.data.Course
import org.better.urn.data.MoodleClient
import org.better.urn.data.UserPreferences

class UniversiticeViewModel {
    private val preferences = UserPreferences()
    private val scope = CoroutineScope(Dispatchers.Main)

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
            errorMessage = null,
            searchQuery = ""
        )
        fetchData(url, token)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun refresh() {
        if (_uiState.value.isLoading) return
        val currentToken = preferences.moodleToken
        val currentUrl = preferences.moodleUrl
        if (currentToken.isNotBlank()) {
            fetchData(currentUrl, currentToken)
            if (_uiState.value.selectedCourse != null) {
                refreshCurrentCourse()
            }
        }
    }

    fun openCourse(courseId: Int) {
        val course = _uiState.value.courses.find { it.id == courseId }
            ?: preferences.cachedCourses.find { it.id == courseId }
            ?: return

        val cachedSections = preferences.getCachedCourseSections(courseId)
        val collapsedIds = preferences.getCollapsedSectionIds(courseId)

        _uiState.value = _uiState.value.copy(
            selectedCourse = course,
            courseSections = cachedSections,
            collapsedSectionIds = collapsedIds,
            errorMessage = null
        )

        fetchCourseContent(courseId)
    }

    fun closeCourse() {
        _uiState.value = _uiState.value.copy(
            selectedCourse = null,
            courseSections = emptyList(),
            collapsedSectionIds = emptySet()
        )
    }

    fun toggleSectionCollapsed(sectionId: Int) {
        val courseId = _uiState.value.selectedCourse?.id ?: return
        val currentCollapsed = _uiState.value.collapsedSectionIds.toMutableSet()

        if (currentCollapsed.contains(sectionId)) {
            currentCollapsed.remove(sectionId)
        } else {
            currentCollapsed.add(sectionId)
        }

        preferences.setCollapsedSectionIds(courseId, currentCollapsed)
        _uiState.value = _uiState.value.copy(collapsedSectionIds = currentCollapsed)
    }

    fun refreshCurrentCourse() {
        val courseId = _uiState.value.selectedCourse?.id ?: return
        if (_uiState.value.isLoadingCourseContent) return
        fetchCourseContent(courseId)
    }

    private fun fetchCourseContent(courseId: Int) {
        val token = preferences.moodleToken
        val url = preferences.moodleUrl
        if (token.isBlank()) return

        scope.launch {
            _uiState.value = _uiState.value.copy(isLoadingCourseContent = true)
            try {
                val client = MoodleClient(url, token)
                val sections = client.getCourseContents(courseId)

                try {
                    preferences.setCachedCourseSections(courseId, sections)
                } catch (_: Exception) {
                    // Ignore cache write error to ensure UI renders fetched course
                }

                _uiState.value = _uiState.value.copy(
                    courseSections = sections,
                    isLoadingCourseContent = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                val cachedSections = preferences.getCachedCourseSections(courseId)
                val displaySections = cachedSections.ifEmpty { _uiState.value.courseSections }
                _uiState.value = _uiState.value.copy(
                    courseSections = displaySections,
                    isLoadingCourseContent = false,
                    errorMessage = if (displaySections.isEmpty()) (e.message ?: "Impossible de charger le cours.") else "Mode hors-ligne : affichage des sections sauvegardées."
                )
            }
        }
    }

    private fun fetchData(url: String, token: String) {
        scope.launch {
            val cachedUser = preferences.cachedUser
            val cachedCourses = preferences.cachedCourses

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                user = _uiState.value.user ?: cachedUser,
                courses = _uiState.value.courses.ifEmpty { cachedCourses },
                isLogged = true
            )

            try {
                val client = MoodleClient(url, token)
                val fetchedUser = client.getUserProfile()
                val fetchedCourses = client.getEnrolledCourses(fetchedUser.userid)

                preferences.moodleUrl = url
                preferences.moodleToken = token
                preferences.cachedUser = fetchedUser
                preferences.cachedCourses = fetchedCourses

                _uiState.value = _uiState.value.copy(
                    user = fetchedUser,
                    courses = fetchedCourses,
                    isLogged = true,
                    isLoading = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLogged = cachedUser != null && cachedCourses.isNotEmpty(),
                    errorMessage = if (cachedUser == null) "Erreur réseau. Veuillez vous reconnecter." else "Mode hors-ligne actif. Données potentiellement obsolètes."
                )
                if (cachedUser == null) preferences.moodleToken = ""
            }
        }
    }
}
