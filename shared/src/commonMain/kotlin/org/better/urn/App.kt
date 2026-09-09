package org.better.urn

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.better.urn.data.AppTheme
import org.better.urn.data.ViewableFile
import org.better.urn.data.ViewableFileType
import org.better.urn.ui.MainLayout
import org.better.urn.ui.components.BetterUrnTopBar
import org.better.urn.ui.components.ImageViewerOverlay
import org.better.urn.ui.components.M3DownloadNotificationBanner
import org.better.urn.ui.components.PdfViewerOverlay
import org.better.urn.ui.components.VideoPlayerOverlay
import org.better.urn.ui.edt.EdtScreen
import org.better.urn.ui.edt.EdtUiState
import org.better.urn.ui.edt.EdtViewModel
import org.better.urn.ui.edt.components.AddTimetableDialog
import org.better.urn.ui.edt.components.AdeTutorialSheet
import org.better.urn.ui.edt.components.EdtManagementSheet
import org.better.urn.ui.navigation.AppScreen
import org.better.urn.ui.navigation.BackHandler
import org.better.urn.ui.settings.SettingsScreen
import org.better.urn.ui.settings.SettingsViewModel
import org.better.urn.ui.universitice.UniversiticeScreen
import org.better.urn.ui.universitice.UniversiticeViewModel

private val LightColors = lightColorScheme(
    primary = Color(0xFF005AC1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = Color(0xFF001A41),

    secondary = Color(0xFF575E71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDBE2F9),
    onSecondaryContainer = Color(0xFF141B2C),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFF7F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF7F9FF),
    onSurface = Color(0xFF191C20),

    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    scrim = Color(0xFF000000),

    surfaceDim = Color(0xFFD9D9E0),
    surfaceBright = Color(0xFFF7F9FF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F3FA),
    surfaceContainer = Color(0xFFEBEFF7),
    surfaceContainerHigh = Color(0xFFE2E7F0),
    surfaceContainerHighest = Color(0xFFDAE0EA)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFADC6FF),
    onPrimary = Color(0xFF002E69),
    primaryContainer = Color(0xFF004494),
    onPrimaryContainer = Color(0xFFD8E2FF),

    secondary = Color(0xFFBFC6DC),
    onSecondary = Color(0xFF293041),
    secondaryContainer = Color(0xFF3F4759),
    onSecondaryContainer = Color(0xFFDBE2F9),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),

    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF43474E),
    scrim = Color(0xFF000000),

    surfaceDim = Color(0xFF111318),
    surfaceBright = Color(0xFF37393E),
    surfaceContainerLowest = Color(0xFF0C0E12),
    surfaceContainerLow = Color(0xFF191C21),
    surfaceContainer = Color(0xFF1F2228),
    surfaceContainerHigh = Color(0xFF292C33),
    surfaceContainerHighest = Color(0xFF343740)
)

@Composable
fun App(
    deepLink: String? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val settingsViewModel: SettingsViewModel = viewModel { SettingsViewModel() }
    val settingsState by settingsViewModel.uiState.collectAsState()

    val isDark = when (settingsState.theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val universiticeViewModel: UniversiticeViewModel = viewModel { UniversiticeViewModel() }
            val state by universiticeViewModel.uiState.collectAsState()

            LaunchedEffect(deepLink) {
                if (!deepLink.isNullOrBlank()) {
                    universiticeViewModel.handleAuthInput(deepLink)
                    onDeepLinkHandled()
                }
            }

            val tabBackstack = rememberSaveable(
                saver = listSaver(
                    save = { it.map { screen -> screen.name } },
                    restore = { savedNames ->
                        mutableStateListOf<AppScreen>().apply {
                            addAll(savedNames.mapNotNull { name ->
                                runCatching { AppScreen.valueOf(name) }.getOrNull()
                            })
                        }
                    }
                )
            ) {
                mutableStateListOf(settingsState.defaultTab)
            }
            val currentScreen = tabBackstack.lastOrNull() ?: AppScreen.UNIVERSITICE

            var isSettingsOpen by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(isSettingsOpen) {
                if (isSettingsOpen) {
                    settingsViewModel.refreshState()
                }
            }

            LaunchedEffect(state.user) {
                settingsViewModel.refreshState()
            }

            val onNavigateToSettings = {
                settingsViewModel.refreshState()
                isSettingsOpen = true
            }

            BackHandler(enabled = isSettingsOpen) {
                isSettingsOpen = false
            }

            BackHandler(enabled = !isSettingsOpen && state.activeFileViewer == null && tabBackstack.size > 1) {
                tabBackstack.removeAt(tabBackstack.lastIndex)
            }

            Box(modifier = Modifier.fillMaxSize()) {
                MainLayout(
                    currentScreen = currentScreen,
                    onScreenSelected = { selected ->
                        if (selected != currentScreen) {
                            tabBackstack.remove(selected)
                            tabBackstack.add(selected)
                        } else {
                            if (selected == AppScreen.UNIVERSITICE) {
                                if (state.activeFileViewer != null) {
                                    universiticeViewModel.closeFileViewer()
                                } else if (state.selectedFolderModule != null) {
                                    universiticeViewModel.closeFolder()
                                } else if (state.selectedCourse != null) {
                                    universiticeViewModel.closeCourse()
                                } else if (state.searchQuery.isNotEmpty()) {
                                    universiticeViewModel.onSearchQueryChange("")
                                }
                            }
                        }
                    }
                ) {
                    when (currentScreen) {
                        AppScreen.UNIVERSITICE -> {
                            UniversiticeScreen(
                                state = state,
                                onInitiateLogin = { baseUrl -> universiticeViewModel.initiateLogin(baseUrl) },
                                onAuthInput = { input, url -> universiticeViewModel.handleAuthInput(input, url) },
                                onRefresh = { universiticeViewModel.refresh() },
                                onCourseClick = { courseId -> universiticeViewModel.openCourse(courseId) },
                                onToggleCourseHidden = { courseId -> universiticeViewModel.toggleCourseHidden(courseId) },
                                onToggleHiddenSectionExpanded = { universiticeViewModel.toggleHiddenSectionExpanded() },
                                onBackClick = {
                                    if (state.activeFileViewer != null) {
                                        universiticeViewModel.closeFileViewer()
                                    } else if (state.selectedFolderModule != null) {
                                        universiticeViewModel.navigateFolderUp()
                                    } else {
                                        universiticeViewModel.closeCourse()
                                    }
                                },
                                onRefreshCourse = { universiticeViewModel.refreshCurrentCourse() },
                                onToggleSectionCollapsed = { sectionId -> universiticeViewModel.toggleSectionCollapsed(sectionId) },
                                onSearchQueryChange = { query -> universiticeViewModel.onSearchQueryChange(query) },
                                onOpenFile = { file -> universiticeViewModel.openFileViewer(file) },
                                onDownloadFile = { file -> universiticeViewModel.downloadFile(file) },
                                onOpenFolder = { module -> universiticeViewModel.openFolder(module) },
                                onCloseFolder = { universiticeViewModel.closeFolder() },
                                onNavigateToSubfolder = { path -> universiticeViewModel.navigateToSubfolder(path) },
                                onNavigateFolderUp = { universiticeViewModel.navigateFolderUp() },
                                onFolderSearchQueryChange = { query -> universiticeViewModel.onFolderSearchQueryChange(query) },
                                onDownloadAllFilesFolder = { module, path -> universiticeViewModel.downloadAllFilesInFolder(module, path) },
                                onProfileClick = onNavigateToSettings
                            )
                        }
                        AppScreen.IZLY -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                BetterUrnTopBar(
                                    title = "Izly",
                                    onProfileClick = onNavigateToSettings
                                )
                                Text("Écran Izly en construction...", modifier = Modifier.padding(16.dp))
                            }
                        }
                        AppScreen.EDT -> {
                            EdtScreen(
                                onProfileClick = onNavigateToSettings,
                                initialViewMode = settingsState.edtDefaultView,
                                weekDays = settingsState.edtWeekDays
                            )
                        }
                        AppScreen.AUTRE -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                BetterUrnTopBar(
                                    title = "Autre",
                                    onProfileClick = onNavigateToSettings
                                )
                                Text("Écran Autre en construction...", modifier = Modifier.padding(16.dp))
                            }
                        }
                    }
                }

                if (isSettingsOpen) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        SettingsScreen(
                            state = settingsState,
                            onThemeChanged = settingsViewModel::onThemeChanged,
                            onDefaultTabChanged = settingsViewModel::onDefaultTabChanged,
                            onEdtDefaultViewChanged = settingsViewModel::onEdtDefaultViewChanged,
                            onEdtWeekDaysChanged = settingsViewModel::onEdtWeekDaysChanged,
                            onServerUrlChanged = settingsViewModel::onServerUrlChanged,
                            onClearCacheClicked = {
                                settingsViewModel.onClearCacheClicked()
                                universiticeViewModel.refresh()
                            },
                            onLogoutClicked = {
                                settingsViewModel.onLogoutClicked()
                                universiticeViewModel.logout()
                                isSettingsOpen = false
                            },
                            onToggleLegalDialog = settingsViewModel::onToggleLegalDialog,
                            onToggleLicenseDialog = settingsViewModel::onToggleLicenseDialog,
                            onToggleServerDialog = settingsViewModel::onToggleServerDialog,
                            onToggleEdtManager = settingsViewModel::onToggleEdtManager,
                            onBackClick = { isSettingsOpen = false },
                            onProfileClick = null
                        )
                    }
                }

                if (settingsState.isEdtManagerOpen) {
                    val edtViewModel: EdtViewModel = viewModel { EdtViewModel() }
                    val edtUiState by edtViewModel.uiState.collectAsState()
                    val timetables by edtViewModel.timetables.collectAsState()
                    val allRawEvents by edtViewModel.allRawEvents.collectAsState()
                    val hiddenCourseTitles by edtViewModel.hiddenCourseTitles.collectAsState()
                    val hiddenEventIds by edtViewModel.hiddenEventIds.collectAsState()
                    val successState = edtUiState as? EdtUiState.Success

                    var showAddDialogInSettings by remember { mutableStateOf(false) }
                    var showAdeTutorialInSettings by remember { mutableStateOf(false) }

                    EdtManagementSheet(
                        timetables = timetables,
                        allEvents = allRawEvents,
                        hiddenCourseTitles = hiddenCourseTitles,
                        hiddenEventIds = hiddenEventIds,
                        isRefreshing = successState?.isRefreshing ?: false,
                        lastSyncTimestamp = successState?.lastSyncTimestamp,
                        refreshError = successState?.refreshError,
                        onToggleVisibility = { id -> edtViewModel.toggleVisibility(id) },
                        onDeleteTimetable = { id -> edtViewModel.deleteTimetable(id) },
                        onAddTimetableClick = { showAddDialogInSettings = true },
                        onForceRefresh = { edtViewModel.loadEvents() },
                        onToggleCourseTitleVisibility = { title -> edtViewModel.toggleCourseTitleVisibility(title) },
                        onUnhideCourseTitle = { title -> edtViewModel.unhideCourseTitle(title) },
                        onUnhideEvent = { eventId -> edtViewModel.unhideEvent(eventId) },
                        onUnhideAllCoursesForTimetable = { timetableId -> edtViewModel.unhideAllCoursesForTimetable(timetableId) },
                        onUnhideAllHidden = { edtViewModel.unhideAllHidden() },
                        onDismiss = { settingsViewModel.onToggleEdtManager(false) }
                    )

                    if (showAddDialogInSettings) {
                        AddTimetableDialog(
                            onDismiss = { showAddDialogInSettings = false },
                            onConfirm = { name, url ->
                                edtViewModel.addTimetable(name, url)
                                showAddDialogInSettings = false
                            },
                            onOpenAdeTutorial = {
                                showAddDialogInSettings = false
                                showAdeTutorialInSettings = true
                            }
                        )
                    }

                    if (showAdeTutorialInSettings) {
                        AdeTutorialSheet(
                            onDismiss = { showAdeTutorialInSettings = false }
                        )
                    }
                }

                val activeFile = state.activeFileViewer
                if (activeFile != null) {
                    val onDownload = { file: ViewableFile ->
                        universiticeViewModel.downloadFile(file)
                    }
                    when (activeFile.fileType) {
                        ViewableFileType.IMAGE -> {
                            ImageViewerOverlay(
                                file = activeFile,
                                onClose = { universiticeViewModel.closeFileViewer() },
                                onDownloadFile = onDownload
                            )
                        }
                        ViewableFileType.VIDEO -> {
                            VideoPlayerOverlay(
                                file = activeFile,
                                onClose = { universiticeViewModel.closeFileViewer() },
                                onDownloadFile = onDownload
                            )
                        }
                        ViewableFileType.PDF -> {
                            PdfViewerOverlay(
                                file = activeFile,
                                onClose = { universiticeViewModel.closeFileViewer() },
                                onDownloadFile = onDownload
                            )
                        }
                        else -> {
                            // Other file types are handled externally
                        }
                    }
                }

                val downloadState = state.downloadState
                if (downloadState != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        M3DownloadNotificationBanner(
                            downloadState = downloadState,
                            onOpenClick = { universiticeViewModel.openDownloadedFile() },
                            onDismissClick = { universiticeViewModel.dismissDownloadNotification() },
                            onRetryClick = { universiticeViewModel.downloadFile(downloadState.file) }
                        )
                    }
                }
            }
        }
    }
}
