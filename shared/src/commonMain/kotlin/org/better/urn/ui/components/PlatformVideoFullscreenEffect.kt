package org.better.urn.ui.components

import androidx.compose.runtime.Composable

/**
 * Handles platform-specific video full-screen effects such as screen rotation
 * and system bars visibility (status bar/clock, navigation bar) on mobile.
 */
@Composable
expect fun PlatformVideoFullscreenEffect(isFullscreen: Boolean, controlsVisible: Boolean)
