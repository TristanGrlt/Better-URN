package org.better.urn.ui.settings

import org.better.urn.data.AppTheme
import org.better.urn.ui.components.FULL_GPLv3_LICENSE_TEXT
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LegalAndThemeTest {

    @Test
    fun testThemeModeResolution() {
        fun resolveIsDark(theme: AppTheme, systemIsDark: Boolean): Boolean {
            return when (theme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> systemIsDark
            }
        }

        // Test Light theme
        assertFalse(resolveIsDark(AppTheme.LIGHT, systemIsDark = false))
        assertFalse(resolveIsDark(AppTheme.LIGHT, systemIsDark = true))

        // Test Dark theme
        assertTrue(resolveIsDark(AppTheme.DARK, systemIsDark = false))
        assertTrue(resolveIsDark(AppTheme.DARK, systemIsDark = true))

        // Test System theme
        assertFalse(resolveIsDark(AppTheme.SYSTEM, systemIsDark = false))
        assertTrue(resolveIsDark(AppTheme.SYSTEM, systemIsDark = true))
    }

    @Test
    fun testLegalSheetState() {
        var isLegalSheetOpen = false
        val toggleLegalSheet = { open: Boolean? ->
            isLegalSheetOpen = open ?: !isLegalSheetOpen
        }

        assertFalse(isLegalSheetOpen)

        toggleLegalSheet(true)
        assertTrue(isLegalSheetOpen)

        toggleLegalSheet(false)
        assertFalse(isLegalSheetOpen)
    }

    @Test
    fun testFullGplLicenseTextIsNotEmpty() {
        assertTrue(FULL_GPLv3_LICENSE_TEXT.isNotBlank())
        assertTrue(FULL_GPLv3_LICENSE_TEXT.contains("GNU GENERAL PUBLIC LICENSE"))
        assertTrue(FULL_GPLv3_LICENSE_TEXT.contains("Version 3, 29 June 2007"))
    }
}
