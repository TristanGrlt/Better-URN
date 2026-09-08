package org.better.urn.ui.universitice

import androidx.lifecycle.ViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.better.urn.data.CourseModule
import org.better.urn.data.FileDownloader
import org.better.urn.data.MoodleClient
import org.better.urn.data.MoodleFolderUtils
import org.better.urn.data.MoodleTokenExpiredException
import org.better.urn.data.UserPreferences
import org.better.urn.data.ViewableFile
import org.better.urn.data.auth.MoodleAuthInitiator
import org.better.urn.data.auth.MoodleAuthNormalizer
import org.better.urn.data.auth.MoodleAuthParser
import org.better.urn.data.auth.MoodleAuthValidator
import org.better.urn.data.createPlatformFileDownloader
import org.better.urn.data.toViewableFile

class UniversiticeViewModel(
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val downloader: FileDownloader = createPlatformFileDownloader()
) : ViewModel() {
    private val preferences = UserPreferences()
    private val scope = CoroutineScope(mainDispatcher)

    private val _uiState = MutableStateFlow(
        UniversiticeUiState(
            isLogged = preferences.moodleToken.isNotBlank(),
            token = preferences.moodleToken,
            moodleUrl = preferences.moodleUrl,
            hiddenCourseIds = preferences.getHiddenCourseIds().toImmutableSet(),
            isHiddenSectionExpanded = preferences.isHiddenSectionExpanded
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

    /**
     * Initiates direct downloading for a [file] with real-time state tracking.
     */
    fun downloadFile(file: ViewableFile) {
        scope.launch {
            downloader.downloadFile(file).collect { downloadState ->
                _uiState.value = _uiState.value.copy(downloadState = downloadState)
            }
        }
    }

    /**
     * Opens the completed download file using platform file viewer.
     */
    fun openDownloadedFile() {
        val currentDownload = _uiState.value.downloadState ?: return
        val path = currentDownload.filePath ?: return
        downloader.openFile(path, currentDownload.file.mimeType)
    }

    /**
     * Dismisses the active in-app download banner.
     */
    fun dismissDownloadNotification() {
        _uiState.value = _uiState.value.copy(downloadState = null)
    }

    /**
     * Initiates the Web SSO authentication flow by generating an ephemeral passport
     * and creating the launch URL.
     */
    fun initiateLogin(baseUrl: String): String {
        val targetUrl = baseUrl.ifBlank { preferences.moodleUrl }
        preferences.moodleUrl = targetUrl
        val passport = MoodleAuthInitiator.generatePassport()
        preferences.moodlePassport = passport
        return MoodleAuthInitiator.createLaunchUrl(targetUrl, passport)
    }

    /**
     * Handles incoming deep links or raw token input, parses the payload, validates security rules,
     * and authenticates the user if valid.
     */
    fun handleAuthInput(input: String, overrideUrl: String? = null): Boolean {
        if (input.isBlank()) return false
        val storedPassport = preferences.moodlePassport
        val normalized = MoodleAuthNormalizer.normalize(input)
        val payload = MoodleAuthParser.parse(normalized)
        val isValid = MoodleAuthValidator.validate(payload, storedPassport)

        if (!isValid || payload == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Authentification échouée : jeton invalide ou échec de vérification de sécurité."
            )
            return false
        }

        // Cleanup temporary passport after successful validation
        preferences.moodlePassport = null
        val targetUrl = overrideUrl?.ifBlank { null } ?: preferences.moodleUrl
        login(targetUrl, payload.token)
        return true
    }

    fun login(url: String, token: String) {
        preferences.moodleUrl = url
        preferences.moodleToken = token
        _uiState.value = _uiState.value.copy(
            isLogged = true,
            token = token,
            moodleUrl = url,
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

    fun openFolder(module: CourseModule) {
        _uiState.value = _uiState.value.copy(
            selectedFolderModule = module,
            currentFolderPath = "/",
            folderSearchQuery = ""
        )
    }

    fun closeFolder() {
        _uiState.value = _uiState.value.copy(
            selectedFolderModule = null,
            currentFolderPath = "/",
            folderSearchQuery = ""
        )
    }

    fun navigateToSubfolder(path: String) {
        val normalized = MoodleFolderUtils.normalizePath(path)
        _uiState.value = _uiState.value.copy(
            currentFolderPath = normalized,
            folderSearchQuery = ""
        )
    }

    fun navigateFolderUp() {
        val currentPath = _uiState.value.currentFolderPath
        if (currentPath == "/") {
            closeFolder()
        } else {
            val parentPath = MoodleFolderUtils.getParentPath(currentPath)
            _uiState.value = _uiState.value.copy(
                currentFolderPath = parentPath,
                folderSearchQuery = ""
            )
        }
    }

    fun onFolderSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(folderSearchQuery = query)
    }

    fun downloadAllFilesInFolder(folderModule: CourseModule, folderPath: String = "/") {
        val token = _uiState.value.token
        val filesToDownload = MoodleFolderUtils.extractAllFiles(
            contents = folderModule.contents,
            moduleId = folderModule.id,
            folderPath = folderPath,
            token = token
        )
        filesToDownload.forEach { file ->
            downloadFile(file)
        }
    }

    fun openFileViewer(file: ViewableFile) {
        _uiState.value = _uiState.value.copy(activeFileViewer = file)
    }

    fun openModuleFile(module: CourseModule): Boolean {
        if (module.modname == "folder") {
            openFolder(module)
            return true
        }

        val token = _uiState.value.token
        val viewable = module.toViewableFile(token) ?: return false
        val isDownloadable = !viewable.isViewableInApp &&
                module.modname != "forum" &&
                module.modname != "url" &&
                module.modname != "page" &&
                module.modname != "quiz"

        if (viewable.isViewableInApp) {
            openFileViewer(viewable)
            return true
        } else if (isDownloadable) {
            downloadFile(viewable)
            return true
        }
        return false
    }

    fun closeFileViewer() {
        _uiState.value = _uiState.value.copy(activeFileViewer = null)
    }

    fun closeCourse() {
        _uiState.value = _uiState.value.copy(
            selectedCourse = null,
            courseSections = persistentListOf(),
            collapsedSectionIds = persistentSetOf(),
            selectedFolderModule = null,
            currentFolderPath = "/",
            folderSearchQuery = "",
            activeFileViewer = null
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

    /**
     * Toggles the hidden status of a course, updating local preferences and syncing asynchronously with Moodle.
     */
    fun toggleCourseHidden(courseId: Int) {
        val currentHiddenIds = _uiState.value.hiddenCourseIds.toMutableSet()
        val isNowHidden = if (currentHiddenIds.contains(courseId)) {
            currentHiddenIds.remove(courseId)
            false
        } else {
            currentHiddenIds.add(courseId)
            true
        }

        preferences.setHiddenCourseIds(currentHiddenIds)
        val newImmutableSet = currentHiddenIds.toImmutableSet()
        _uiState.value = _uiState.value.copy(hiddenCourseIds = newImmutableSet)

        val token = preferences.moodleToken
        val url = preferences.moodleUrl
        val userId = _uiState.value.user?.userid
        if (token.isNotBlank()) {
            scope.launch {
                try {
                    val client = MoodleClient(url, token)
                    client.setCourseHidden(courseId, isNowHidden, userId)
                } catch (_: Exception) {
                    // Local preference persists even if Moodle REST call fails
                }
            }
        }
    }

    /**
     * Toggles the collapse/expansion state of the hidden courses bottom section.
     */
    fun toggleHiddenSectionExpanded() {
        val newExpanded = !_uiState.value.isHiddenSectionExpanded
        preferences.isHiddenSectionExpanded = newExpanded
        _uiState.value = _uiState.value.copy(isHiddenSectionExpanded = newExpanded)
    }

    fun refreshCurrentCourse() {
        val courseId = _uiState.value.selectedCourse?.id ?: return
        if (_uiState.value.isLoadingCourseContent) return
        fetchCourseContent(courseId)
    }

    private fun handleTokenExpiration(message: String?) {
        preferences.logout()

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
            selectedFolderModule = null,
            currentFolderPath = "/",
            folderSearchQuery = "",
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
                cachedCourses.map { it.sanitized().withResolvedImageUrl(token) }
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
                token = token,
                moodleUrl = url
            )

            if (token.isNotBlank() && processedCachedCourses.isNotEmpty()) {
                preloadCourseImages(MoodleClient(url, token), processedCachedCourses, token)
            }


            try {
                val client = MoodleClient(url, token)
                val fetchedUser = client.getUserProfile()
                val fetchedCourses = client.getEnrolledCourses(fetchedUser.userid)

                val processedCourses = withContext(Dispatchers.Default) {
                    fetchedCourses.map { it.sanitized().withResolvedImageUrl(token) }
                }

                val serverHiddenIds = fetchedCourses.filter { it.isHidden }.map { it.id }.toSet()
                val mergedHiddenIds = (preferences.getHiddenCourseIds() + serverHiddenIds).toSet()
                preferences.setHiddenCourseIds(mergedHiddenIds)

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
                    hiddenCourseIds = mergedHiddenIds.toImmutableSet(),
                    filteredCourses = filtered.toImmutableList(),
                    isLogged = true,
                    token = token,
                    moodleUrl = url,
                    isLoading = false,
                    errorMessage = null
                )

                preloadCourseImages(client, processedCourses, token)
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
                    moodleUrl = preferences.moodleUrl,
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

    private fun preloadCourseImages(client: MoodleClient, courses: List<org.better.urn.data.Course>, token: String) {
        scope.launch(Dispatchers.Default) {
            var anyUpdated = false
            val updatedList = courses.map { course ->
                val fileUrl = course.overviewfiles.firstOrNull()?.fileurl
                val rawUrl = if (fileUrl != null) {
                    if (token.isBlank() || fileUrl.contains("token=")) fileUrl
                    else if (fileUrl.contains("?")) "$fileUrl&token=$token"
                    else "$fileUrl?token=$token"
                } else course.imageUrl ?: return@map course

                if (!rawUrl.startsWith("file:") && !rawUrl.startsWith("content:")) {
                    val resolvedUri = org.better.urn.data.CourseImageCache.getOrFetchCourseImage(course.id, rawUrl, client)
                    if (resolvedUri != rawUrl) {
                        anyUpdated = true
                        course.copy(imageUrl = resolvedUri)
                    } else course
                } else course
            }

            if (anyUpdated) {
                preferences.cachedCourses = updatedList
                val currentCourses = _uiState.value.courses
                val mergedCourses = currentCourses.map { existing ->
                    updatedList.find { it.id == existing.id } ?: existing
                }.toImmutableList()
                val filtered = filterCourses(mergedCourses, _uiState.value.searchQuery).toImmutableList()
                _uiState.value = _uiState.value.copy(
                    courses = mergedCourses,
                    filteredCourses = filtered
                )
            }
        }
    }
}

