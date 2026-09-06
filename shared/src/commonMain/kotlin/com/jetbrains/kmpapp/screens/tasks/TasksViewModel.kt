package com.jetbrains.kmpapp.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.data.TaskRepository
import com.jetbrains.kmpapp.data.model.AssessmentType
import com.jetbrains.kmpapp.data.model.StudyTask
import com.jetbrains.kmpapp.data.model.Subject
import com.jetbrains.kmpapp.data.model.SubjectImportance
import com.jetbrains.kmpapp.data.model.Subtask
import com.jetbrains.kmpapp.data.model.TaskCategory
import com.jetbrains.kmpapp.data.model.TaskPriority
import com.jetbrains.kmpapp.data.model.TaskStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.random.Random

class TasksViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {

    val subjects: StateFlow<List<Subject>> = taskRepository.subjects
    val tasks: StateFlow<List<StudyTask>> = taskRepository.tasks

    val activeCount: StateFlow<Int> = taskRepository.tasks.combine(MutableStateFlow(Unit)) { list, _ ->
        list.count { it.status != TaskStatus.COMPLETED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedCount: StateFlow<Int> = taskRepository.tasks.combine(MutableStateFlow(Unit)) { list, _ ->
        list.count { it.status == TaskStatus.COMPLETED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val overallProgress: StateFlow<Int> = taskRepository.tasks.combine(MutableStateFlow(Unit)) { list, _ ->
        if (list.isEmpty()) 0 else {
            val total = list.sumOf { it.completionRatio }
            ((total / list.size) * 100).toInt()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun createSubject(
        name: String,
        shortCode: String,
        colorHex: String,
        importance: SubjectImportance,
        assessmentType: AssessmentType,
        teacherName: String = "",
        roomOrLink: String = "",
        notes: String = ""
    ) {
        val subject = Subject(
            id = "subj_${Random.nextInt(100000, 999999)}",
            name = name.trim(),
            shortCode = if (shortCode.isNotBlank()) shortCode.trim().uppercase() else name.take(3).uppercase(),
            colorHex = colorHex,
            importance = importance,
            assessmentType = assessmentType,
            teacherName = teacherName.trim(),
            roomOrLink = roomOrLink.trim(),
            notes = notes.trim()
        )
        taskRepository.addSubject(subject)
    }

    fun updateSubject(subject: Subject) {
        taskRepository.updateSubject(subject)
    }

    fun deleteSubject(subjectId: String) {
        taskRepository.deleteSubject(subjectId)
    }

    fun createTask(
        subjectId: String,
        title: String,
        description: String,
        category: TaskCategory,
        priority: TaskPriority,
        subtaskTitles: List<String>,
        dueDate: String? = null
    ) {
        val id = "task_${Random.nextInt(100000, 999999)}"
        val subtasks = subtaskTitles.filter { it.isNotBlank() }.map {
            Subtask(id = "sub_${Random.nextInt(100000, 999999)}", title = it.trim(), isCompleted = false)
        }
        val subj = taskRepository.subjects.value.find { it.id == subjectId }
        val task = StudyTask(
            id = id,
            subjectId = subjectId,
            subjectTitle = subj?.name ?: "",
            title = title.trim(),
            taskDescription = description.trim(),
            category = category,
            priority = priority,
            status = TaskStatus.PENDING,
            subtasks = subtasks,
            dueDateIso = dueDate,
            createdAtIso = ""
        )
        taskRepository.addTask(task)
    }

    fun updateTask(task: StudyTask) {
        taskRepository.updateTask(task)
    }

    fun deleteTask(taskId: String) {
        taskRepository.deleteTask(taskId)
    }

    fun toggleSubtask(taskId: String, subtaskId: String) {
        taskRepository.toggleSubtask(taskId, subtaskId)
    }

    fun toggleTaskCompletion(taskId: String) {
        taskRepository.toggleTaskCompletion(taskId)
    }

    fun setTaskStatus(taskId: String, status: TaskStatus) {
        taskRepository.setTaskStatus(taskId, status)
    }

    fun clearAllData() {
        taskRepository.clearAllData()
    }
}
