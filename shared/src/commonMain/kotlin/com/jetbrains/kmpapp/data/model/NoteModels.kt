package com.jetbrains.kmpapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NoteSection(
    @SerialName("text") val text: String = "",
    // ARGB-значение цвета написания (Long для сериализации)
    @SerialName("color") val color: Long = DEFAULT_NOTE_COLOR
)

@Serializable
data class NotePage(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String = "Страница",
    @SerialName("sections") val sections: List<NoteSection> = listOf(NoteSection())
) {
    val isEmpty: Boolean get() = sections.all { it.text.isBlank() }
}

const val DEFAULT_NOTE_COLOR: Long = 0xFF1E5BB0

fun defaultNotePages(): List<NotePage> = listOf(
    NotePage(id = "page-default", title = "Страница 1")
)