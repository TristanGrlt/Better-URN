package org.better.urn.data

private val BR_REGEX = Regex("(?i)<br\\s*/?>")
private val BLOCK_TAG_REGEX = Regex("(?i)</(p|div|li|h[1-6]|tr)>")
private val ALL_TAG_REGEX = Regex("<[^>]*>")
private val MULTI_NEWLINE_REGEX = Regex("\n{3,}")

/**
 * Fast and memory-friendly HTML tag stripper that preserves paragraph line breaks.
 */
fun String.cleanHtml(): String {
    if (!contains('<')) return trim()
    return replace(BR_REGEX, "\n")
        .replace(BLOCK_TAG_REGEX, "\n")
        .replace(ALL_TAG_REGEX, "")
        .replace(MULTI_NEWLINE_REGEX, "\n\n")
        .trim()
}
