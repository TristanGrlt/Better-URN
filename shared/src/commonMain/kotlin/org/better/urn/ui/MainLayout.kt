package org.better.urn.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.better.urn.ui.navigation.AppScreen

@Composable
fun MainLayout(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    content: @Composable () -> Unit
) {
    BoxWithConstraints {
        val isDesktop = maxWidth >= 800.dp
        if (isDesktop) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    AppScreen.entries.forEach { screen ->
                        NavigationRailItem(
                            selected = currentScreen == screen,
                            onClick = { onScreenSelected(screen) },
                            icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) }
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar(
                      containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        AppScreen.entries.forEach { screen ->
                            NavigationBarItem(
                                selected = currentScreen == screen,
                                onClick = { onScreenSelected(screen) },
                                icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title) }
                            )
                        }
                    }
                }
            ) { paddingValues ->
              Box(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding()).fillMaxSize()) {
                    content()
                }
            }
        }
    }
}
