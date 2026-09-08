package com.jetbrains.kmpapp.screens.schedule

internal fun abbreviateSubjectName(subject: String): String {
    val words = subject.split(' ', '-').filter { it.isNotBlank() }
    if (words.size <= 2) return subject
    return words.joinToString("") { word ->
        val first = word.first()
        if (word.length > 2) first.uppercaseChar().toString() else first.toString()
    }
}
