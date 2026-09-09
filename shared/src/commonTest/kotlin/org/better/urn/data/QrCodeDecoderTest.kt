package org.better.urn.data

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class QrCodeDecoderTest {

    @Test
    fun testDecodeRgbPixelsWithValidQrCode() {
        val testUrl = "https://ade.univ-rouen.fr/jsp/custom/modules/plannings/anonymous_cal.jsp?resources=9999&calType=ical"
        val width = 200
        val height = 200

        // Generate synthetic QR code matrix using ZXing
        val bitMatrix = QRCodeWriter().encode(testUrl, BarcodeFormat.QR_CODE, width, height)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (bitMatrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }

        val result = QrCodeDecoder.decodeRgbPixels(pixels, width, height)

        assertIs<QrScanResult.Success>(result)
        assertEquals(testUrl, result.url)
        assertEquals("Calendrier ADE (9999)", result.suggestedName)
    }

    @Test
    fun testDecodeRgbPixelsWithEmptyImage() {
        val width = 100
        val height = 100
        val blankPixels = IntArray(width * height) { 0xFFFFFFFF.toInt() }

        val result = QrCodeDecoder.decodeRgbPixels(blankPixels, width, height)

        assertIs<QrScanResult.InvalidFormat>(result)
    }
}
