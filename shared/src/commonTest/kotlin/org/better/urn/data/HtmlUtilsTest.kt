package org.better.urn.data

import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlUtilsTest {

    @Test
    fun testDecodeHtmlEntitiesBasicAndArrows() {
        val input = "&nbsp;&nbsp;&nbsp; VPN DPI Linux.pdf =&gt; Tutoriel &lt;Test&gt;"
        val decoded = input.decodeHtmlEntities()
        val expected = "    VPN DPI Linux.pdf => Tutoriel <Test>"
        assertEquals(expected, decoded)
    }

    @Test
    fun testDecodeFrenchAccents() {
        val input = "&eacute;&egrave;&ecirc;&euml;&agrave;&acirc;&icirc;&ocirc;&ugrave;&ucirc;&ccedil; &Eacute;&Agrave;"
        val decoded = input.decodeHtmlEntities()
        assertEquals("éèêëàâîôùûç ÉÀ", decoded)
    }

    @Test
    fun testDecodeNumericEntitiesDecimalAndHex() {
        val input = "&#233;&#160;&#x00E9;&#x0020;&#39;"
        val decoded = input.decodeHtmlEntities()
        assertEquals("é é '", decoded)
    }

    @Test
    fun testCleanHtmlWithComplexMoodleFolderText() {
        val rawInput = "&nbsp;&nbsp;&nbsp; Vous trouverez donc, dans ce dossier les fichiers qui vous permettront de configurer la connexion au VPN sur votre machine:\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; - VPN DPI Linux.pdf =&gt; Tutoriel de connexion au VPN depuis un client Linux (de type Ubuntu et dérivés)\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; - VPN DPI MacOSX.pdf =&gt; Tutoriel de connexion au VPN depuis un client MacOSX\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; - VPN DPI W10.pdf =&gt; Tutoriel de connexion au VPN\n" +
                "depuis un Windows 10\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; W10_L2TP_Correctif.reg =&gt; Patch créé par mes soins\n" +
                "pour permettre à Windows 10 de se connecter au VPN: Doit impérativement être téléchargé sur le client puis exécuté sur le client comme indiqué dans le tutoriel correspondant.\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; - DPIRootCA.cacert.pem =&gt; L'autorité de certification du"

        val cleaned = rawInput.cleanHtml()

        val expected = "Vous trouverez donc, dans ce dossier les fichiers qui vous permettront de configurer la connexion au VPN sur votre machine:\n" +
                "        - VPN DPI Linux.pdf => Tutoriel de connexion au VPN depuis un client Linux (de type Ubuntu et dérivés)\n" +
                "        - VPN DPI MacOSX.pdf => Tutoriel de connexion au VPN depuis un client MacOSX\n" +
                "        - VPN DPI W10.pdf => Tutoriel de connexion au VPN\n" +
                "depuis un Windows 10\n" +
                "        W10_L2TP_Correctif.reg => Patch créé par mes soins\n" +
                "pour permettre à Windows 10 de se connecter au VPN: Doit impérativement être téléchargé sur le client puis exécuté sur le client comme indiqué dans le tutoriel correspondant.\n" +
                "        - DPIRootCA.cacert.pem => L'autorité de certification du"

        assertEquals(expected, cleaned)
    }

    @Test
    fun testCleanHtmlStripsTagsAndPreservesLineBreaks() {
        val htmlInput = "<p>&nbsp;&nbsp;Titre principal</p><br/><ul><li>Module 1 =&gt; &eacute;l&eacute;ment</li><li>Module 2</li></ul>"
        val cleaned = htmlInput.cleanHtml()
        val expected = "Titre principal\n\n• Module 1 => élément\n• Module 2"
        assertEquals(expected, cleaned)
    }
}
