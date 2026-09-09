package org.better.urn.ui.edt

/**
 * Defines pager navigation policies for the Edt (timetable) screen.
 */
object EdtPagerPolicy {
    /**
     * Determines whether user horizontal drag gestures are enabled on the main tab pager.
     * Swiping is enabled only on the Agenda tab (page 0) to navigate towards the Semaine tab.
     * Once on the Semaine tab (page 1), outer swiping is disabled so that horizontal swipes
     * are exclusively handled by the inner week navigation pager.
     *
     * @param currentPage The current active page index in the main pager.
     * @return true if horizontal swipe gestures are permitted; false otherwise.
     */
    fun isOuterSwipeEnabled(currentPage: Int): Boolean {
        return currentPage == 0
    }
}
