package org.better.urn

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import org.better.urn.data.Course
import org.better.urn.data.MoodleClient
import org.better.urn.data.MoodleUser
import org.better.urn.data.UserPreferences
import org.better.urn.ui.HomeScreen
import org.better.urn.ui.LoginScreen

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
fun App() {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val coroutineScope = rememberCoroutineScope()
            val preferences = remember { UserPreferences() }

            var isLogged by remember { mutableStateOf(preferences.moodleToken.isNotBlank()) }
            var isLoading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            var user by remember { mutableStateOf<MoodleUser?>(null) }
            var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
            val currentToken = preferences.moodleToken

            // Connexion automatique au lancement si le token est enregistré
            LaunchedEffect(currentToken) {
                if (currentToken.isNotBlank()) {
                    isLoading = true
                    try {
                        val client = MoodleClient(preferences.moodleUrl, currentToken)
                        val fetchedUser = client.getUserProfile()
                        val fetchedCourses = client.getEnrolledCourses(fetchedUser.userid)
                        user = fetchedUser
                        courses = fetchedCourses
                        isLogged = true
                    } catch (e: Exception) {
                        preferences.moodleToken = "" // Réinitialise si le token est invalide
                        isLogged = false
                    } finally {
                        isLoading = false
                    }
                }
            }

            if (!isLogged) {
                LoginScreen(
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onLogin = { url, token ->
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                val client = MoodleClient(url, token)
                                val fetchedUser = client.getUserProfile()
                                val fetchedCourses = client.getEnrolledCourses(fetchedUser.userid)
                                
                                // Sauvegarde locale
                                preferences.moodleUrl = url
                                preferences.moodleToken = token

                                user = fetchedUser
                                courses = fetchedCourses
                                isLogged = true
                            } catch (e: Exception) {
                                errorMessage = "Erreur de connexion, vérifiez l'URL et le token."
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                )
            } else {
                HomeScreen(
                    userName = user?.fullname ?: "Étudiant",
                    courses = courses,
                    token = currentToken, // Passage du token pour les images
                    onCourseClick = { courseId ->
                        println("Ouverture du cours $courseId")
                    }
                )
            }
        }
    }
}
