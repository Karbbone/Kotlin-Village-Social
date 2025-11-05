package com.example.mobile.search

import com.example.mobile.network.CityDto
import java.text.Normalizer
import java.util.Locale

private fun String.normalized(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase(Locale.getDefault())

/**
 * Rank cities for a query. Priorities:
 * - For text queries: exact name > name starts-with > name contains, with small postal bonus for 2-digit dept
 * - For numeric queries: postal code starts-with >> postal code contains; name match gives small tie-breaker
 */
fun rankCities(query: String, cities: List<CityDto>): List<CityDto> {
    val q = query.trim()
    if (q.length < 2) return emptyList<CityDto>()
    val qn = q.normalized()
    val isNumeric = q.all { it.isDigit() }

    return cities.asSequence()
        .map { c ->
            val nameN = c.name.normalized()
            var nameScore = 0
            var postalScore = 0

            if (isNumeric) {
                // Numeric query: prefer postal code
                if (c.postalCode.startsWith(q)) postalScore = 10_000
                else if (c.postalCode.contains(q)) postalScore = 2_000
                // small tie-breaker on name
                if (nameN == qn) nameScore = 50
                else if (nameN.startsWith(qn)) nameScore = 25
                else if (nameN.contains(qn)) nameScore = 10
            } else {
                // Textual query: prefer name
                if (nameN == qn) nameScore = 10_000
                else if (nameN.startsWith(qn)) nameScore = 9_000
                else if (nameN.contains(qn)) nameScore = 8_000
                // small dept bonus for 2-digit department code in query
                if (q.length == 2 && c.postalCode.startsWith(q)) postalScore = 500
            }
            val score = nameScore + postalScore
            Quadruple(c, score, nameN, c.postalCode)
        }
        .filter { it.second > 0 }
        .sortedWith(compareByDescending<Quadruple<CityDto, Int, String, String>> { it.second }
            .thenBy { it.third }
            .thenBy { it.fourth })
        .map { it.first }
        .toList()
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
