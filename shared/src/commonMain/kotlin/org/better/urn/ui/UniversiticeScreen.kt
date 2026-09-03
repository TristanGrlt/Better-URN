package org.better.urn.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
            contentAlignment = Alignment.TopCenter
        ) {
            if (!state.isLogged) {
                LoginScreen(
                    isLoading = state.isLoading,
                    errorMessage = state.errorMessage,
                    onLogin = onLogin
                )
            } else {
                val courses = state.courses
                val userName = state.user?.fullname ?: "Étudiant"
                val token = UserPreferences().moodleToken

                Column(
                    modifier = Modifier
                        .widthIn(max = 960.dp)
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Bonjour, $userName 👋",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Prêt à explorer vos cours aujourd'hui ?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Unités d'Enseignement (UE)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 280.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(courses) { course ->
                            CourseCard(course = course, token = token, onClick = { onCourseClick(course.id) })
                        }
                    }
                }
            }
        }
    }
}
