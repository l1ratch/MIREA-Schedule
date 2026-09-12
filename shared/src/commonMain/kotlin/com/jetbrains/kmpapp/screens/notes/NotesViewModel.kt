package com.jetbrains.kmpapp.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.data.ScheduleRepository
import com.jetbrains.kmpapp.data.model.DEFAULT_NOTE_COLOR
import com.jetbrains.kmpapp.data.model.NotePage
import com.jetbrains.kmpapp.data.model.NoteSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class NotesViewModel(
    private val repository: ScheduleRepository
) : ViewModel() {

    val pages: StateFlow<List<NotePage>> = repository.notePages

    val askBeforeNoteDelete: StateFlow<Boolean> = repository.askBeforeNoteDelete

    private val _selectedPageId = MutableStateFlow<String?>(null)
    val selectedPageId: StateFlow<String?> = _selectedPageId.asStateFlow()

    val currentPage: StateFlow<NotePage?> = combine(
        pages,
        _selectedPageId
    ) { pages, selectedId ->
        pages.firstOrNull { it.id == selectedId } ?: pages.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectPage(pageId: String) {
        _selectedPageId.value = pageId
    }

    fun addPage() {
        val newPage = NotePage(
            id = "page-${kotlin.time.Clock.System.now().toEpochMilliseconds()}",
            title = "Страница ${repository.notePages.value.size + 1}"
        )
        repository.updateNotePages(repository.notePages.value + newPage)
        _selectedPageId.value = newPage.id
    }

    fun deletePage(pageId: String) {
        val updated = repository.notePages.value.filterNot { it.id == pageId }
        repository.updateNotePages(updated)
        if (_selectedPageId.value == pageId) {
            _selectedPageId.value = null
        }
    }

    fun renamePage(pageId: String, title: String) {
        updatePage(pageId) { it.copy(title = title) }
    }

    fun addSection(pageId: String) {
        updatePage(pageId) { page ->
            page.copy(sections = page.sections + NoteSection())
        }
    }

    fun removeSection(pageId: String, sectionIndex: Int) {
        updatePage(pageId) { page ->
            page.copy(sections = page.sections.filterIndexed { index, _ -> index != sectionIndex })
        }
    }

    fun setSectionText(pageId: String, sectionIndex: Int, text: String) {
        updateSection(pageId, sectionIndex) { it.copy(text = text) }
    }

    fun setSectionColor(pageId: String, sectionIndex: Int, color: Long) {
        updateSection(pageId, sectionIndex) { it.copy(color = color) }
    }

    private fun updatePage(pageId: String, transform: (NotePage) -> NotePage) {
        repository.updateNotePages(
            repository.notePages.value.map { page ->
                if (page.id == pageId) transform(page) else page
            }
        )
    }

    private fun updateSection(pageId: String, sectionIndex: Int, transform: (NoteSection) -> NoteSection) {
        updatePage(pageId) { page ->
            page.copy(
                sections = page.sections.mapIndexed { index, section ->
                    if (index == sectionIndex) transform(section) else section
                }
            )
        }
    }

    fun currentColor(): Long = currentPage.value?.sections?.lastOrNull()?.color ?: DEFAULT_NOTE_COLOR
}