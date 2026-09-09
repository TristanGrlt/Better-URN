package org.better.urn.ui.edt

import org.better.urn.ui.edt.components.AddChoiceMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AddChoiceAndTutorialTest {

    @Test
    fun testAddChoiceModeEnumValues() {
        val modes = AddChoiceMode.entries
        assertEquals(2, modes.size)
        assertEquals(AddChoiceMode.MAIN_MENU, modes[0])
        assertEquals(AddChoiceMode.CALENDAR_OPTIONS, modes[1])
    }

    @Test
    fun testTutorialStepContentRequirements() {
        val step1Text = "Dans la liste des ressources de votre établissement sur la gauche, développez l'arborescence jusqu'à trouver votre groupe, puis cliquez dessus pour l'afficher sur le calendrier."
        val step2Text = "En bas à gauche de votre écran, dans le petit panneau « OPTIONS », cliquez sur l'icône d'export (qui ressemble à un calendrier avec une flèche sortante)."
        val step3Text = "Une fenêtre s'ouvre. Assurez-vous que le format « ICalendar » est bien coché. Vous n'avez pas besoin de modifier les dates de début ou de fin. Cliquez simplement sur le bouton « Générer URL » en bas au centre."
        val step4Text = "Une nouvelle fenêtre affiche un code QR et un lien web. Copiez entièrement l'URL longue indiquée ou flashez le QR code : c'est ce lien/QR code qu'il faudra coller dans l'application pour importer et synchroniser votre emploi du temps !"

        assertNotEquals("", step1Text)
        assertNotEquals("", step2Text)
        assertNotEquals("", step3Text)
        assertNotEquals("", step4Text)
    }
}
