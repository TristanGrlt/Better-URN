package org.better.urn.data

private val BR_REGEX = Regex("(?i)<br\\s*/?>")
private val BLOCK_TAG_REGEX = Regex("(?i)</(p|div|li|h[1-6]|tr)>")
private val LIST_ITEM_REGEX = Regex("(?i)<li[^>]*>")
private val ALL_TAG_REGEX = Regex("<[^>]*>")
private val MULTI_NEWLINE_REGEX = Regex("\n{3,}")

private val ENTITY_REGEX = Regex("&(#?[a-zA-Z0-9]+);")

private val NAMED_ENTITIES: Map<String, String> = mapOf(
    "nbsp" to " ",
    "gt" to ">",
    "lt" to "<",
    "amp" to "&",
    "quot" to "\"",
    "apos" to "'",
    "rsquo" to "’",
    "lsquo" to "‘",
    "rdquo" to "”",
    "ldquo" to "“",
    "mdash" to "—",
    "ndash" to "–",
    "hellip" to "…",
    "bull" to "•",
    "copy" to "©",
    "reg" to "®",
    "trade" to "™",
    "euro" to "€",
    "pound" to "£",
    "deg" to "°",
    "plusmn" to "±",
    "times" to "×",
    "divide" to "÷",
    "micro" to "µ",
    "para" to "¶",
    "sect" to "§",
    // French & European accents
    "eacute" to "é", "Eacute" to "É",
    "egrave" to "è", "Egrave" to "È",
    "ecirc" to "ê", "Ecirc" to "Ê",
    "euml" to "ë", "Euml" to "Ë",
    "agrave" to "à", "Agrave" to "À",
    "acirc" to "â", "Acirc" to "Â",
    "auml" to "ä", "Auml" to "Ä",
    "icirc" to "î", "Icirc" to "Î",
    "iuml" to "ï", "Iuml" to "Ï",
    "ocirc" to "ô", "Ocirc" to "Ô",
    "ouml" to "ö", "Ouml" to "Ö",
    "ugrave" to "ù", "Ugrave" to "Ù",
    "ucirc" to "û", "Ucirc" to "Û",
    "uuml" to "ü", "Uuml" to "Ü",
    "ccedil" to "ç", "Ccedil" to "Ç",
    "ntilde" to "ñ", "Ntilde" to "Ñ",
    "aelig" to "æ", "AElig" to "Æ",
    "oelig" to "œ", "OElig" to "Œ"
)

/**
 * Decodes HTML entities (e.g., &nbsp;, &gt;, &lt;, &eacute;, &#39;, &#xE9;).
 * Supports multi-pass decoding for double-encoded strings.
 */
fun String.decodeHtmlEntities(): String {
    if (!contains('&')) return this
    var current = this
    var previous: String
    var iterations = 0
    do {
        previous = current
        current = ENTITY_REGEX.replace(previous) { matchResult ->
            val entity = matchResult.groupValues[1]
            when {
                entity.startsWith("#x", ignoreCase = true) -> {
                    val hexCode = entity.substring(2)
                    try {
                        val codePoint = hexCode.toInt(16)
                        Char(codePoint).toString()
                    } catch (_: Exception) {
                        matchResult.value
                    }
                }
                entity.startsWith("#") -> {
                    val decCode = entity.substring(1)
                    try {
                        val codePoint = decCode.toInt(10)
                        Char(codePoint).toString()
                    } catch (_: Exception) {
                        matchResult.value
                    }
                }
                else -> NAMED_ENTITIES[entity] ?: matchResult.value
            }
        }
        iterations++
    } while (current != previous && current.contains('&') && iterations < 3)
    return current.replace('\u00A0', ' ')
}

/**
 * Fast and memory-friendly HTML tag stripper that preserves line breaks and decodes HTML entities.
 */
fun String.cleanHtml(): String {
    if (isBlank()) return ""

    var text = this
    if (text.contains('<')) {
        text = text
            .replace(BR_REGEX, "\n")
            .replace(BLOCK_TAG_REGEX, "\n")
            .replace(LIST_ITEM_REGEX, "• ")
            .replace(ALL_TAG_REGEX, "")
    }

    text = text.decodeHtmlEntities()

    return text
        .replace(MULTI_NEWLINE_REGEX, "\n\n")
        .trim()
}
