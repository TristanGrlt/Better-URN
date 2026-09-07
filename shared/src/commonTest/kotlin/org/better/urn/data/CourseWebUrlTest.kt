package org.better.urn.data

import kotlin.test.Test
import kotlin.test.assertEquals

class CourseWebUrlTest {

    @Test
    fun testGetWebUrlStandardBaseUrl() {
        val course = Course(id = 1234, fullname = "Algorithmique", shortname = "ALG101")
        val url = course.getWebUrl("https://universitice.univ-rouen.fr")
        assertEquals("https://universitice.univ-rouen.fr/course/view.php?id=1234", url)
    }

    @Test
    fun testGetWebUrlTrailingSlashInBaseUrl() {
        val course = Course(id = 5678, fullname = "Réseaux", shortname = "RES201")
        val url = course.getWebUrl("https://universitice.univ-rouen.fr/")
        assertEquals("https://universitice.univ-rouen.fr/course/view.php?id=5678", url)
    }

    @Test
    fun testGetWebUrlCustomBaseUrlWithSpaces() {
        val course = Course(id = 99, fullname = "Base de données", shortname = "BDD1")
        val url = course.getWebUrl("  https://moodle.univ.fr/  ")
        assertEquals("https://moodle.univ.fr/course/view.php?id=99", url)
    }
}
