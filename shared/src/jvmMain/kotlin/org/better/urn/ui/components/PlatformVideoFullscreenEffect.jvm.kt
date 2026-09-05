package org.better.urn.ui.components

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformVideoFullscreenEffect(isFullscreen: Boolean, controlsVisible: Boolean) {
    // Desktop JVM handles window layout bounds via Compose overlays
}
