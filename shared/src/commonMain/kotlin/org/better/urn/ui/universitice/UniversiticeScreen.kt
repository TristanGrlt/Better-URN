package org.better.urn.ui.universitice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.better.urn.data.UserPreferences
import org.better.urn.ui.components.BetterUrnTopBar
import org.better.urn.ui.components.CourseCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversiticeScreen(
    state: UniversiticeUiState,
    onLogin: (String, String) -> Unit,
    onRefresh: () -> Unit,
    onCourseClick: (Int) -> Unit,
    onSearchQueryChange: (String) -> Unit = {}
) {
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
                    LoadingIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                } else {
                    val filteredCourses = state.filteredCourses
                    val token = UserPreferences().moodleToken

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

                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = onSearchQueryChange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                placeholder = { Text("Rechercher un cours...") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Rechercher",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    if (state.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onSearchQueryChange("") }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Clear,
                                                contentDescription = "Effacer la recherche"
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = CircleShape,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )

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
