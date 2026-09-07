package org.better.urn

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.better.urn.data.ViewableFile
import org.better.urn.data.ViewableFileType
import org.better.urn.ui.MainLayout
import org.better.urn.ui.components.ImageViewerOverlay
import org.better.urn.ui.components.M3DownloadNotificationBanner
import org.better.urn.ui.components.PdfViewerOverlay
import org.better.urn.ui.components.VideoPlayerOverlay
import org.better.urn.ui.navigation.AppScreen
import org.better.urn.ui.navigation.BackHandler
import org.better.urn.ui.universitice.UniversiticeScreen
import org.better.urn.ui.universitice.UniversiticeViewModel

private val LightColors = lightColorScheme(
    primary = Color(0xFF00497D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF0061A4),
    onPrimaryContainer = Color(0xFFC0DBFF),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F8),
    onSecondaryContainer = Color(0xFF596576),
    background = Color(0xFFF9F9FC),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFF9F9FC),
    onSurface = Color(0xFF1A1C1E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FCAFF),
    onPrimary = Color(0xFF001D36),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF101C2B),
    secondaryContainer = Color(0xFF3C4858),
    onSecondaryContainer = Color(0xFFD7E3F8),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E5),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E5)
)

@Composable
fun App(
    deepLink: String? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors
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
                mutableStateListOf(AppScreen.UNIVERSITICE)
            }
            val currentScreen = tabBackstack.lastOrNull() ?: AppScreen.UNIVERSITICE

            BackHandler(enabled = state.activeFileViewer == null && tabBackstack.size > 1) {
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
                                onBackClick = {
                                    if (state.activeFileViewer != null) {
                                        universiticeViewModel.closeFileViewer()
                                    } else {
                                        universiticeViewModel.closeCourse()
                                    }
                                },
                                onRefreshCourse = { universiticeViewModel.refreshCurrentCourse() },
                                onToggleSectionCollapsed = { sectionId -> universiticeViewModel.toggleSectionCollapsed(sectionId) },
                                onSearchQueryChange = { query -> universiticeViewModel.onSearchQueryChange(query) },
                                onOpenFile = { file -> universiticeViewModel.openFileViewer(file) },
                                onDownloadFile = { file -> universiticeViewModel.downloadFile(file) }
                            )
                        }
                        AppScreen.IZLY -> {
                            Text("Écran Izly en construction...", modifier = Modifier.padding(16.dp))
                        }
                        AppScreen.EDT -> {
                            Text("Emploi du temps en construction...", modifier = Modifier.padding(16.dp))
                        }
                        AppScreen.AUTRE -> {
                            Text("Autres options...", modifier = Modifier.padding(16.dp))
                        }
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
