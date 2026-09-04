package org.better.urn.ui.universitice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import org.better.urn.data.UserPreferences
import org.better.urn.ui.components.BetterUrnTopBar
import org.better.urn.ui.components.CourseCard
import org.better.urn.ui.components.M3CoursesLoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversiticeScreen(
    state: UniversiticeUiState,
    onLogin: (String, String) -> Unit,
    onRefresh: () -> Unit,
    onCourseClick: (Int) -> Unit,
    onBackClick: () -> Unit = {},
    onRefreshCourse: () -> Unit = {},
    onToggleSectionCollapsed: (Int) -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {}
) {
    val token = UserPreferences().moodleToken

    if (state.selectedCourse != null) {
        CourseDetailScreen(
            course = state.selectedCourse,
            sections = state.courseSections,
            collapsedSectionIds = state.collapsedSectionIds,
            isLoading = state.isLoadingCourseContent,
            errorMessage = state.errorMessage,
            token = token,
            onBackClick = onBackClick,
            onRefresh = onRefreshCourse,
            onToggleSectionCollapsed = onToggleSectionCollapsed
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BetterUrnTopBar(
            title = "Universitice",
            onRefresh = if (state.isLogged) onRefresh else null,
            isRefreshing = state.isLoading,
            refreshContentDescription = "Recharger les cours"
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
                        onLogin = onLogin
                    )
                }
            } else {
                if (state.isLoading && state.courses.isEmpty()) {
                    M3CoursesLoadingView()
                } else {
                    val filteredCourses = state.filteredCourses

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
                            Spacer(modifier = Modifier.height(16.dp))

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

                            if (filteredCourses.isEmpty() && state.courses.isNotEmpty()) {
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
                                    columns = GridCells.Adaptive(minSize = 280.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    items(filteredCourses, key = { it.id }) { course ->
                                        CourseCard(
                                            course = course,
                                            token = token,
                                            onClick = { onCourseClick(course.id) }
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
