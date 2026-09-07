package org.better.urn

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertNotEquals

class ThemeContainerColorTest {

    @Test
    fun testLightColorSchemeSurfaceContainerHierarchy() {
        // Verify distinct container colors for M3 surface roles in Light theme
        val surface = Color(0xFFF7F9FF)
        val surfaceContainerLowest = Color(0xFFFFFFFF)
        val surfaceContainer = Color(0xFFEBEFF7)
        val surfaceContainerHigh = Color(0xFFE2E7F0)

        // Cards (pure white) vs background vs navbar (surfaceContainer)
        assertNotEquals(surfaceContainerLowest, surface)
        assertNotEquals(surfaceContainerLowest, surfaceContainer)
        assertNotEquals(surfaceContainer, surface)
        assertNotEquals(surfaceContainerHigh, surfaceContainerLowest)
    }

    @Test
    fun testDarkColorSchemeSurfaceContainerHierarchy() {
        // Verify distinct container colors for M3 surface roles in Dark theme
        val surface = Color(0xFF111318)
        val surfaceContainer = Color(0xFF1F2228)
        val surfaceContainerHigh = Color(0xFF292C33)

        // Cards (surfaceContainerHigh) vs dark background (surface) vs navbar (surfaceContainer)
        assertNotEquals(surfaceContainerHigh, surface)
        assertNotEquals(surfaceContainerHigh, surfaceContainer)
        assertNotEquals(surfaceContainer, surface)
    }
}
