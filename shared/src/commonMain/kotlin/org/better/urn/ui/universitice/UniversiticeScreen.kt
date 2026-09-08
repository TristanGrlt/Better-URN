package org.better.urn.ui.universitice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.better.urn.data.Course
import org.better.urn.data.CourseModule
import org.better.urn.data.ViewableFile
import org.better.urn.ui.components.BetterUrnTopBar
import org.better.urn.ui.components.CourseCard
import org.better.urn.ui.components.M3CoursesLoadingView
import org.better.urn.ui.navigation.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversiticeScreen(
    state: UniversiticeUiState,
    onInitiateLogin: (String) -> String = { "" },
    onAuthInput: (String, String) -> Unit = { _, _ -> },
    onRefresh: () -> Unit,
    onCourseClick: (Int) -> Unit,
    onToggleCourseHidden: (Int) -> Unit = {},
    onToggleHiddenSectionExpanded: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onRefreshCourse: () -> Unit = {},
    onToggleSectionCollapsed: (Int) -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onOpenFile: (ViewableFile) -> Unit = {},
    onDownloadFile: (ViewableFile) -> Unit = {},
    onOpenFolder: (CourseModule) -> Unit = {},
    onCloseFolder: () -> Unit = {},
    onNavigateToSubfolder: (String) -> Unit = {},
    onNavigateFolderUp: () -> Unit = {},
    onFolderSearchQueryChange: (String) -> Unit = {},
    onDownloadAllFilesFolder: (CourseModule, String) -> Unit = { _, _ -> },
    onOpenCourseInBrowser: ((Course) -> Unit)? = null,
    onProfileClick: () -> Unit = {}
) {
    val token = state.token
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val uriHandler = LocalUriHandler.current
    val handleOpenCourseInBrowser = onOpenCourseInBrowser ?: { course: Course ->
        uriHandler.openUri(course.getWebUrl(state.moodleUrl))
    }

    val selectedFolder = state.selectedFolderModule
    if (selectedFolder != null) {
        BackHandler(enabled = state.activeFileViewer == null) {
            onNavigateFolderUp()
        }
        FolderDetailScreen(
            folderModule = selectedFolder,
            treeContent = state.folderTreeContent,
            searchQuery = state.folderSearchQuery,
            downloadState = state.downloadState,
            onBackClick = onNavigateFolderUp,
            onNavigateToSubfolder = onNavigateToSubfolder,
            onSearchQueryChange = onFolderSearchQueryChange,
            onDownloadAllFiles = { onDownloadAllFilesFolder(selectedFolder, state.currentFolderPath) },
            onOpenFile = onOpenFile,
            onDownloadFile = onDownloadFile,
            onRefresh = onRefreshCourse,
            onProfileClick = onProfileClick
        )
        return
    }

    if (state.selectedCourse != null) {
        BackHandler(enabled = state.activeFileViewer == null) {
            onBackClick()
        }
        CourseDetailScreen(
            course = state.selectedCourse,
            sections = state.courseSections,
            collapsedSectionIds = state.collapsedSectionIds,
            isLoading = state.isLoadingCourseContent,
            errorMessage = state.errorMessage,
            token = token,
            onBackClick = onBackClick,
            onRefresh = onRefreshCourse,
            onToggleSectionCollapsed = onToggleSectionCollapsed,
            onOpenFile = onOpenFile,
            onDownloadFile = onDownloadFile,
            onOpenFolder = onOpenFolder,
            downloadState = state.downloadState,
            onProfileClick = onProfileClick
        )
        return
    }

    BackHandler(enabled = state.activeFileViewer == null && state.searchQuery.isNotEmpty()) {
        onSearchQueryChange("")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BetterUrnTopBar(
            title = "Universitice",
            onTitleClick = if (state.isLogged) {
                {
                    coroutineScope.launch {
                        gridState.animateScrollToItem(0)
                    }
                }
            } else null,
            onRefresh = if (state.isLogged) onRefresh else null,
            isRefreshing = state.isLoading,
            refreshContentDescription = "Recharger les cours",
            onProfileClick = onProfileClick
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (!state.isLogged) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LoginScreen(
                        isLoading = state.isLoading,
                        errorMessage = state.errorMessage,
                        onInitiateLogin = onInitiateLogin,
                        onAuthInput = onAuthInput
                    )
                }
            } else {
                if (state.isLoading && state.courses.isEmpty()) {
                    M3CoursesLoadingView()
                } else {
                    val visibleCourses = state.filteredVisibleCourses
                    val hiddenCourses = state.filteredHiddenCourses

                    PullToRefreshBox(
                        isRefreshing = state.isLoading,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 960.dp)
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .align(Alignment.TopCenter)
                        ) {
                            Spacer(modifier = Modifier.height(5.dp))

                            var isFocused by remember { mutableStateOf(false) }

                            Surface(
                                modifier = Modifier
                                    .widthIn(max = 440.dp)
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .align(Alignment.CenterHorizontally),
                                shape = CircleShape,
                                color = if (isFocused) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Rechercher",
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (state.searchQuery.isEmpty()) {
                                            Text(
                                                text = "Rechercher un cours...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                        BasicTextField(
                                            value = state.searchQuery,
                                            onValueChange = onSearchQueryChange,
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onFocusChanged { isFocused = it.isFocused }
                                        )
                                    }
                                    if (state.searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { onSearchQueryChange("") },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Clear,
                                                contentDescription = "Effacer la recherche",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            if (visibleCourses.isEmpty() && hiddenCourses.isEmpty() && state.courses.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.SearchOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Aucun cours ne correspond à « ${state.searchQuery} »",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextButton(onClick = { onSearchQueryChange("") }) {
                                            Text("Effacer la recherche")
                                        }
                                    }
                                }
                            } else {
                                LazyVerticalGrid(
                                    state = gridState,
                                    columns = GridCells.Adaptive(minSize = 280.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    items(visibleCourses, key = { it.id }) { course ->
                                        val onClick = remember(course.id, onCourseClick) {
                                            { onCourseClick(course.id) }
                                        }
                                        val onToggleHide = remember(course.id, onToggleCourseHidden) {
                                            { onToggleCourseHidden(course.id) }
                                        }
                                        val onOpenInBrowser = remember(course, handleOpenCourseInBrowser) {
                                            { handleOpenCourseInBrowser(course) }
                                        }
                                        CourseCard(
                                            course = course,
                                            token = token,
                                            onClick = onClick,
                                            onToggleHide = onToggleHide,
                                            onOpenInBrowser = onOpenInBrowser,
                                            isHidden = false
                                        )
                                    }

                                    if (hiddenCourses.isNotEmpty()) {
                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Surface(
                                                onClick = onToggleHiddenSectionExpanded,
                                                shape = MaterialTheme.shapes.medium,
                                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = if (visibleCourses.isNotEmpty()) 16.dp else 0.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.VisibilityOff,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        text = "Cours masqués (${hiddenCourses.size})",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Icon(
                                                        imageVector = if (state.isHiddenSectionExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                                        contentDescription = if (state.isHiddenSectionExpanded) "Réduire les cours masqués" else "Déplier les cours masqués",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (state.isHiddenSectionExpanded) {
                                            items(hiddenCourses, key = { "hidden_${it.id}" }) { course ->
                                                val onClick = remember(course.id, onCourseClick) {
                                                    { onCourseClick(course.id) }
                                                }
                                                val onToggleHide = remember(course.id, onToggleCourseHidden) {
                                                    { onToggleCourseHidden(course.id) }
                                                }
                                                val onOpenInBrowser = remember(course, handleOpenCourseInBrowser) {
                                                    { handleOpenCourseInBrowser(course) }
                                                }
                                                CourseCard(
                                                    course = course,
                                                    token = token,
                                                    onClick = onClick,
                                                    onToggleHide = onToggleHide,
                                                    onOpenInBrowser = onOpenInBrowser,
                                                    isHidden = true
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
