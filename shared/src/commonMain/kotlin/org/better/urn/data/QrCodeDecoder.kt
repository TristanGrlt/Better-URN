package org.better.urn.data

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * ZXing wrapper for decoding QR Codes from raw pixel arrays or camera luminance buffers.
 */
object QrCodeDecoder {

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(com.google.zxing.BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true,
            )
        )
    }

    /**
     * Decodes a QR code from an ARGB pixel buffer (e.g. decoded bitmap or image file).
     */
    fun decodeRgbPixels(pixels: IntArray, width: Int, height: Int): QrScanResult {
        return try {
            val source = RGBLuminanceSource(width, height, pixels)
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decodeWithState(bitmap)
            reader.reset()
            QrCodeParser.parse(result.text)
        } catch (_: Exception) {
            reader.reset()
            QrScanResult.InvalidFormat("", "Aucun code QR détecté dans l'image.")
        }
    }

    /**
     * Decodes a QR code from a raw YUV Y-plane luminance buffer, handling row stride and rotation.
     */
    fun decodeYuvPlane(
        yPlane: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int = width,
        rotationDegrees: Int = 0
    ): QrScanResult {
        return try {
            val (effectiveData, effectiveWidth, effectiveHeight, cropWidth, cropHeight) = when (rotationDegrees) {
                90 -> {
                    val rotated = ByteArray(width * height)
                    for (y in 0 until height) {
                        val rowOffset = y * rowStride
                        for (x in 0 until width) {
                            rotated[(x * height) + (height - 1 - y)] = yPlane[rowOffset + x]
                        }
                    }
                    Quintuple(rotated, height, width, height, width)
                }
                180 -> {
                    val rotated = ByteArray(width * height)
                    for (y in 0 until height) {
                        val rowOffset = y * rowStride
                        val targetRowOffset = (height - 1 - y) * width
                        for (x in 0 until width) {
                            rotated[targetRowOffset + (width - 1 - x)] = yPlane[rowOffset + x]
                        }
                    }
                    Quintuple(rotated, width, height, width, height)
                }
                270 -> {
                    val rotated = ByteArray(width * height)
                    for (y in 0 until height) {
                        val rowOffset = y * rowStride
                        for (x in 0 until width) {
                            rotated[((width - 1 - x) * height) + y] = yPlane[rowOffset + x]
                        }
                    }
                    Quintuple(rotated, height, width, height, width)
                }
                else -> {
                    Quintuple(yPlane, rowStride, height, width, height)
                }
            }

            val source = PlanarYUVLuminanceSource(
                effectiveData,
                effectiveWidth,
                effectiveHeight,
                0,
                0,
                cropWidth,
                cropHeight,
                false
            )
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decodeWithState(bitmap)
            reader.reset()
            QrCodeParser.parse(result.text)
        } catch (_: Exception) {
            reader.reset()
            QrScanResult.InvalidFormat("", "Aucun code QR détecté.")
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
