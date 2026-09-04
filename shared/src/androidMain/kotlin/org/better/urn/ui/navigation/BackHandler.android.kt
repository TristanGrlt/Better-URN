package org.better.urn.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)
}

actual fun Modifier.handleDesktopBack(dispatcher: BackDispatcher): Modifier = this
