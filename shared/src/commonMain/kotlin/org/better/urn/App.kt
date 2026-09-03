package org.better.urn

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState 
import androidx.compose.runtime.getValue       
import androidx.compose.runtime.setValue       
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.better.urn.ui.universitice.UniversiticeViewModel
import org.better.urn.ui.universitice.UniversiticeScreen
import org.better.urn.ui.navigation.AppScreen
import org.better.urn.ui.MainLayout

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
            val universiticeViewModel = remember { UniversiticeViewModel() }
            val state by universiticeViewModel.uiState.collectAsState()
            
            var currentScreen by remember { mutableStateOf(AppScreen.UNIVERSITICE) }

            MainLayout(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            ) {
                when (currentScreen) {
                    AppScreen.UNIVERSITICE -> {
                        UniversiticeScreen(
                            state = state,
                            onLogin = { url, token -> universiticeViewModel.login(url, token) },
                            onRefresh = { universiticeViewModel.refresh() },
                            onCourseClick = { courseId -> println("Ouverture du cours $courseId") }
                        )
                    }
                    AppScreen.IZLY -> {
                        // Plus tard : IzlyScreen()
                        Text("Écran Izly en construction...", modifier = Modifier.padding(16.dp))
                    }
                    AppScreen.EDT -> {
                        // Plus tard : EdtScreen()
                        Text("Emploi du temps en construction...", modifier = Modifier.padding(16.dp))
                    }
                    AppScreen.AUTRE -> {
                        Text("Autres options...", modifier = Modifier.padding(16.dp))
                    }
                }
            }
            
        }
    }
}
