package org.better.urn.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.better.urn.ui.navigation.AppScreen
import org.better.urn.ui.navigation.BackDispatcher
import org.better.urn.ui.navigation.ProvideBackDispatcher
import org.better.urn.ui.navigation.handleDesktopBack

@Composable
fun MainLayout(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    content: @Composable () -> Unit,
) {
    val backDispatcher = remember { BackDispatcher() }

    ProvideBackDispatcher(dispatcher = backDispatcher) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .handleDesktopBack(backDispatcher),
        ) {
            val isDesktop = maxWidth >= 800.dp
            if (isDesktop) {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = NavigationRailDefaults.ContainerColor,
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        AppScreen.entries.forEach { screen ->
                            NavigationRailItem(
                                selected = currentScreen == screen,
                                onClick = { onScreenSelected(screen) },
                                icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title) },
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
                            containerColor = NavigationBarDefaults.containerColor,
                        ) {
                            AppScreen.entries.forEach { screen ->
                                NavigationBarItem(
                                    selected = currentScreen == screen,
                                    onClick = { onScreenSelected(screen) },
                                    icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                                    label = { Text(screen.title) },
                                )
                            }
                        }
                    },
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding()).fillMaxSize()) {
                        content()
                    }
                }
            }
        }
    }
}
