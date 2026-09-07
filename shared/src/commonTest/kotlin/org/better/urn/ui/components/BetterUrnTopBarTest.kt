package org.better.urn.ui.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BetterUrnTopBarTest {

    @Test
    fun testTitleClickCallback() {
        var clicked = false
        val onTitleClick = { clicked = true }

        assertFalse(clicked)
        onTitleClick()
        assertTrue(clicked)
    }
}
