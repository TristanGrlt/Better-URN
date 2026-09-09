package org.better.urn.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class QrCodeParserTest {

    @Test
    fun testParseAdeQrCodeUrl() {
        val raw = "https://ade.univ-rouen.fr/jsp/custom/modules/plannings/anonymous_cal.jsp?resources=1234&projectId=2&calType=ical"
        val result = QrCodeParser.parse(raw)

        assertIs<QrScanResult.Success>(result)
        assertEquals(raw, result.url)
        assertEquals("Calendrier ADE (1234)", result.suggestedName)
    }

    @Test
    fun testParseWebcalUrl() {
        val raw = "webcal://ade.univ-rouen.fr/jsp/custom/modules/plannings/anonymous_cal.jsp?resources=5678&calType=ical"
        val result = QrCodeParser.parse(raw)

        assertIs<QrScanResult.Success>(result)
        assertEquals("https://ade.univ-rouen.fr/jsp/custom/modules/plannings/anonymous_cal.jsp?resources=5678&calType=ical", result.url)
        assertEquals("Calendrier ADE (5678)", result.suggestedName)
    }

    @Test
    fun testParseInvalidText() {
        val raw = "Ceci n'est pas une URL"
        val result = QrCodeParser.parse(raw)

        assertIs<QrScanResult.InvalidFormat>(result)
        assertEquals(raw, result.rawText)
    }

    @Test
    fun testParseEmptyText() {
        val result = QrCodeParser.parse("   ")

        assertIs<QrScanResult.InvalidFormat>(result)
    }

    @Test
    fun testExtractSuggestedNameWithTitleParam() {
        val url = "https://example.com/calendar.ics?title=Master+1+Informatique"
        val name = QrCodeParser.extractSuggestedName(url)

        assertEquals("Master 1 Informatique", name)
    }

    @Test
    fun testExtractSuggestedNameFallback() {
        val url = "https://example.com/schedule.ics"
        val name = QrCodeParser.extractSuggestedName(url)

        assertEquals("Emploi du temps", name)
    }
}
