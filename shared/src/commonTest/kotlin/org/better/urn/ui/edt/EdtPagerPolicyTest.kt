package org.better.urn.ui.edt

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EdtPagerPolicyTest {

    @Test
    fun testOuterSwipeEnabledOnAgendaPage() {
        assertTrue(
            EdtPagerPolicy.isOuterSwipeEnabled(0),
            "Outer swipe gesture should be enabled on Agenda page (index 0) to allow swiping to Semaine",
        )
    }

    @Test
    fun testOuterSwipeDisabledOnSemainePage() {
        assertFalse(
            EdtPagerPolicy.isOuterSwipeEnabled(1),
            "Outer swipe gesture should be disabled on Semaine page (index 1) to allow inner week pager gestures",
        )
    }

    @Test
    fun testOuterSwipeDisabledOnUnknownPage() {
        assertFalse(
            EdtPagerPolicy.isOuterSwipeEnabled(2),
            "Outer swipe gesture should be disabled on any out-of-bounds page index",
        )
        assertFalse(
            EdtPagerPolicy.isOuterSwipeEnabled(-1),
            "Outer swipe gesture should be disabled on negative page index",
        )
    }
}
