package org.better.urn.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.better.urn.data.UserPreferences
import org.better.urn.ui.components.CourseCard
import org.better.urn.ui.components.BetterUrnTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversiticeScreen(
    state: UniversiticeUiState,
    onLogin: (String, String) -> Unit,
    onCourseClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BetterUrnTopBar(title = "Universitice")
        
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
                    // Utilisation du composant Material 3 Expressive morphing
                    LoadingIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                } else {
                    val courses = state.courses
                    val token = UserPreferences().moodleToken
                    
                    Column(
                        modifier = Modifier
                            .widthIn(max = 960.dp)
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 280.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(courses) { course ->
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
