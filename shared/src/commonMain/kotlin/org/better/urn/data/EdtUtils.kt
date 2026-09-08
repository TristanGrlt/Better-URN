package org.better.urn.data

import kotlin.math.abs

fun cleanSubjectName(summary: String): String {
    val regex = Regex("""(?i)\b(TD[0-9]*|TP[0-9]*|CM|CC|CT|Examen|Gr[ ]?[A-Z0-9]+)\b""")
    val withoutKeywords = regex.replace(summary, "")
    val withoutSpecialChars = withoutKeywords
        .replace('-', ' ')
        .replace('(', ' ')
        .replace(')', ' ')
    return withoutSpecialChars.replace(Regex("""\s+"""), " ").trim()
}

fun generateColorFromSubject(subjectName: String, isDarkTheme: Boolean): String {
    val cleaned = cleanSubjectName(subjectName)
    val absHash = abs(cleaned.hashCode())
    val hue = (absHash % 360).toFloat()
    val saturation = 0.6f
    val lightness = if (isDarkTheme) 0.7f else 0.35f
    return hslToHex(hue, saturation, lightness)
}

private fun hslToHex(hue: Float, saturation: Float, lightness: Float): String {
    val c = (1f - abs(2f * lightness - 1f)) * saturation
    val x = c * (1f - abs((hue / 60f) % 2f - 1f))
    val m = lightness - c / 2f

    val (rPrime, gPrime, bPrime) = when {
        hue < 60f -> Triple(c, x, 0f)
        hue < 120f -> Triple(x, c, 0f)
        hue < 180f -> Triple(0f, c, x)
        hue < 240f -> Triple(0f, x, c)
        hue < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    val r = ((rPrime + m) * 255f).toInt().coerceIn(0, 255)
    val g = ((gPrime + m) * 255f).toInt().coerceIn(0, 255)
    val b = ((bPrime + m) * 255f).toInt().coerceIn(0, 255)

    fun Int.toTwoHexDigits(): String {
        val hex = this.toString(16).uppercase()
        return if (hex.length < 2) "0$hex" else hex
    }

    return "#${r.toTwoHexDigits()}${g.toTwoHexDigits()}${b.toTwoHexDigits()}"
}
