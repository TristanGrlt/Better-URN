package org.better.urn.ui.universitice

import org.better.urn.data.Course
import kotlin.math.abs

private val WHITESPACE_REGEX = Regex("\\s+")

fun filterCourses(courses: List<Course>, query: String): List<Course> {
    if (query.isBlank()) return courses
    return courses.filter { course ->
        fuzzyMatches(course.fullname, query) ||
        fuzzyMatches(course.shortname, query)
    }
}

/**
 * High-performance fuzzy matching with fast-path substring check and single-row Levenshtein distance.
 */
fun fuzzyMatches(target: String, query: String): Boolean {
    val normalizedTarget = target.normalizeForSearch()
    val normalizedQuery = query.normalizeForSearch()

    if (normalizedQuery.isBlank()) return true

    // Fast Path: Direct substring match for instant evaluation
    if (normalizedTarget.contains(normalizedQuery)) return true

    val queryTokens = normalizedQuery.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
    if (queryTokens.isEmpty()) return true

    val targetTokens = normalizedTarget.split(WHITESPACE_REGEX).filter { it.isNotBlank() }

    return queryTokens.all { qToken ->
        val maxDist = when {
            qToken.length <= 3 -> 0
            qToken.length <= 5 -> 1
            else -> 2
        }

        targetTokens.any { tToken ->
            if (tToken.contains(qToken)) return@any true
            if (maxDist == 0) return@any false

            val tSub = if (tToken.length >= qToken.length) {
                tToken.take(qToken.length)
            } else {
                tToken
            }
            levDistance(tSub, qToken, maxDist) <= maxDist
        }
    }
}

/**
 * Single-row Levenshtein distance with row-level early termination.
 */
private fun levDistance(s1: String, s2: String, maxAllowed: Int): Int {
    if (s1 == s2) return 0
    val len1 = s1.length
    val len2 = s2.length

    if (abs(len1 - len2) > maxAllowed) return maxAllowed + 1
    if (len1 == 0) return len2
    if (len2 == 0) return len1

    var prev = IntArray(len2 + 1) { it }
    var curr = IntArray(len2 + 1)

    for (i in 1..len1) {
        curr[0] = i
        var minInRow = curr[0]

        for (j in 1..len2) {
            val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
            val insertion = curr[j - 1] + 1
            val deletion = prev[j] + 1
            val substitution = prev[j - 1] + cost

            val valJ = minOf(insertion, minOf(deletion, substitution))
            curr[j] = valJ
            if (valJ < minInRow) minInRow = valJ
        }

        if (minInRow > maxAllowed) return maxAllowed + 1

        val temp = prev
        prev = curr
        curr = temp
    }

    return prev[len2]
}

/**
 * Strips accents and converts string to lowercase for accent-insensitive search matching.
 */
private fun String.normalizeForSearch(): String {
    val builder = StringBuilder(length)
    for (c in this.lowercase()) {
        val replacement = when (c) {
            'à', 'á', 'â', 'ã', 'ä', 'å' -> 'a'
            'è', 'é', 'ê', 'ë' -> 'e'
            'ì', 'í', 'î', 'ï' -> 'i'
            'ò', 'ó', 'ô', 'õ', 'ö' -> 'o'
            'ù', 'ú', 'û', 'ü' -> 'u'
            'ç' -> 'c'
            'ñ' -> 'n'
            'ÿ' -> 'y'
            else -> c
        }
        builder.append(replacement)
    }
    return builder.toString()
}
