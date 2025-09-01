package com.example.moodtunes_v1.history

fun generateIdFromInput(input: String): String {
    val trimmed = input.trim()
    val words = trimmed.split("\\s+".toRegex()).take(5)
    val base = words.joinToString("_").lowercase()
    val safe = base.replace(Regex("[^a-z0-9_]+"), "")
    val timestamp = System.currentTimeMillis()
    return "${safe}_$timestamp"
}