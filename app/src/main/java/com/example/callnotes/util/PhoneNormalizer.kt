package com.example.callnotes.util

object PhoneNormalizer {
    /** Keep digits; last 10 (simple for India). For global, switch to E.164. */
    fun normalize(raw: String?): String {
        val d = raw?.filter { it.isDigit() } ?: ""
        return if (d.length > 10) d.takeLast(10) else d
    }
}
