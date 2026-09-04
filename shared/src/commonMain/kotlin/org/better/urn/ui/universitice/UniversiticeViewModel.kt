package org.better.urn.ui.universitice

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.better.urn.data.MoodleClient
import org.better.urn.data.MoodleTokenExpiredException
import org.better.urn.data.UserPreferences

class UniversiticeViewModel {
    private val preferences = UserPreferences()
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow(
        UniversiticeUiState(
            isLogged = preferences.moodleToken.isNotBlank(),
            token = preferences.moodleToken
        )
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
        preferences.moodleUrl = url
        preferences.moodleToken = token
        _uiState.value = _uiState.value.copy(
            isLogged = true,
            token = token,
            errorMessage = null,
            searchQuery = ""
        )
        fetchData(url, token)
    }

    fun onSearchQueryChange(query: String) {
        val currentCourses = _uiState.value.courses
        _uiState.value = _uiState.value.copy(searchQuery = query)
        scope.launch(Dispatchers.Default) {
            val filtered = filterCourses(currentCourses, query).toImmutableList()
            _uiState.value = _uiState.value.copy(filteredCourses = filtered)
        }
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

        scope.launch {
            val cachedSections = withContext(Dispatchers.Default) {
                preferences.getCachedCourseSections(courseId).map { it.sanitized() }
            }
            val collapsedIds = preferences.getCollapsedSectionIds(courseId)

            _uiState.value = _uiState.value.copy(
                selectedCourse = course,
                courseSections = cachedSections.toImmutableList(),
                collapsedSectionIds = collapsedIds.toImmutableSet(),
                errorMessage = null
            )

            fetchCourseContent(courseId)
        }
    }

    fun closeCourse() {
        _uiState.value = _uiState.value.copy(
            selectedCourse = null,
            courseSections = persistentListOf(),
            collapsedSectionIds = persistentSetOf()
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
        _uiState.value = _uiState.value.copy(collapsedSectionIds = currentCollapsed.toImmutableSet())
    }

    fun refreshCurrentCourse() {
        val courseId = _uiState.value.selectedCourse?.id ?: return
        if (_uiState.value.isLoadingCourseContent) return
        fetchCourseContent(courseId)
    }

    private fun handleTokenExpiration(message: String?) {
        preferences.moodleToken = ""
        preferences.cachedUser = null
        preferences.cachedCourses = emptyList()

        val displayMsg = if (!message.isNullOrBlank()) {
            "Votre session Moodle a expiré ($message). Veuillez vous reconnecter."
        } else {
            "Votre session Moodle a expiré. Veuillez vous reconnecter."
        }

        _uiState.value = _uiState.value.copy(
            isLogged = false,
            token = "",
            isLoading = false,
            isLoadingCourseContent = false,
            user = null,
            courses = persistentListOf(),
            filteredCourses = persistentListOf(),
            selectedCourse = null,
            courseSections = persistentListOf(),
            collapsedSectionIds = persistentSetOf(),
            errorMessage = displayMsg
        )
    }

    private fun fetchCourseContent(courseId: Int) {
        val token = preferences.moodleToken
        val url = preferences.moodleUrl
        if (token.isBlank()) return

        scope.launch {
            _uiState.value = _uiState.value.copy(isLoadingCourseContent = true)
            try {
                val client = MoodleClient(url, token)
                val rawSections = client.getCourseContents(courseId)
                val sections = withContext(Dispatchers.Default) {
                    rawSections.map { it.sanitized() }
                }

                try {
                    preferences.setCachedCourseSections(courseId, sections)
                } catch (_: Exception) {
                    // Ignore cache write error to ensure UI renders fetched course
                }

                _uiState.value = _uiState.value.copy(
                    courseSections = sections.toImmutableList(),
                    isLoadingCourseContent = false,
                    errorMessage = null
                )
            } catch (e: MoodleTokenExpiredException) {
                handleTokenExpiration(e.message)
            } catch (e: Exception) {
                val cachedSections = withContext(Dispatchers.Default) {
                    preferences.getCachedCourseSections(courseId).map { it.sanitized() }
                }
                val displaySections = cachedSections.ifEmpty { _uiState.value.courseSections }
                _uiState.value = _uiState.value.copy(
                    courseSections = displaySections.toImmutableList(),
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

            val processedCachedCourses = withContext(Dispatchers.Default) {
                cachedCourses.map { it.withResolvedImageUrl(token) }
            }
            val currentQuery = _uiState.value.searchQuery
            val filteredCached = withContext(Dispatchers.Default) {
                filterCourses(processedCachedCourses, currentQuery)
            }

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                user = _uiState.value.user ?: cachedUser,
                courses = processedCachedCourses.toImmutableList(),
                filteredCourses = filteredCached.toImmutableList(),
                isLogged = true,
                token = token
            )

            try {
                val client = MoodleClient(url, token)
                val fetchedUser = client.getUserProfile()
                val fetchedCourses = client.getEnrolledCourses(fetchedUser.userid)

                val processedCourses = withContext(Dispatchers.Default) {
                    fetchedCourses.map { it.withResolvedImageUrl(token) }
                }

                preferences.moodleUrl = url
                preferences.moodleToken = token
                preferences.cachedUser = fetchedUser
                preferences.cachedCourses = processedCourses

                val filtered = withContext(Dispatchers.Default) {
                    filterCourses(processedCourses, _uiState.value.searchQuery)
                }

                _uiState.value = _uiState.value.copy(
                    user = fetchedUser,
                    courses = processedCourses.toImmutableList(),
                    filteredCourses = filtered.toImmutableList(),
                    isLogged = true,
                    token = token,
                    isLoading = false,
                    errorMessage = null
                )
            } catch (e: MoodleTokenExpiredException) {
                handleTokenExpiration(e.message)
            } catch (e: Exception) {
                val displayCourses = if (_uiState.value.courses.isNotEmpty()) {
                    _uiState.value.courses
                } else {
                    processedCachedCourses.toImmutableList()
                }
                val displayFiltered = withContext(Dispatchers.Default) {
                    filterCourses(displayCourses, _uiState.value.searchQuery)
                }.toImmutableList()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLogged = true,
                    token = preferences.moodleToken,
                    courses = displayCourses,
                    filteredCourses = displayFiltered,
                    errorMessage = if (displayCourses.isEmpty()) {
                        (e.message ?: "Impossible de se connecter au serveur Moodle.")
                    } else {
                        "Mode hors-ligne actif. Données potentiellement obsolètes."
                    }
                )
            }
        }
    }
}
