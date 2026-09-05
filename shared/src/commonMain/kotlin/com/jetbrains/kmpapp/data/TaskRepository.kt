package com.jetbrains.kmpapp.data

import com.jetbrains.kmpapp.data.model.StudyTask
import com.jetbrains.kmpapp.data.model.Subject
import com.jetbrains.kmpapp.data.model.Subtask
import com.jetbrains.kmpapp.data.model.TaskCategory
import com.jetbrains.kmpapp.data.model.TaskPriority
import com.jetbrains.kmpapp.data.model.TaskStatus
import com.jetbrains.kmpapp.data.storage.PlatformStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TaskRepository(
    private val storage: PlatformStorage
) {
    private val keySubjects = "saved_study_subjects_list"
    private val keyStudyTasks = "saved_study_tasks_list"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _tasks = MutableStateFlow<List<StudyTask>>(emptyList())
    val tasks: StateFlow<List<StudyTask>> = _tasks.asStateFlow()

    init {
        loadSubjects()
        loadTasks()
    }

    private fun loadSubjects() {
        val raw = storage.getString(keySubjects)
        if (!raw.isNullOrEmpty()) {
            try {
                _subjects.value = json.decodeFromString<List<Subject>>(raw)
            } catch (e: Exception) {
                println("TaskRepository: failed to decode subjects: ${e.message}")
            }
        }
    }

    private fun persistSubjects(list: List<Subject>) {
        _subjects.value = list
        try {
            val encoded = json.encodeToString(list)
            storage.saveString(keySubjects, encoded)
        } catch (e: Exception) {
            println("TaskRepository: failed to save subjects: ${e.message}")
        }
    }

    fun addSubject(subject: Subject) {
        val current = _subjects.value.toMutableList()
        current.add(0, subject)
        persistSubjects(current)
    }

    fun updateSubject(updated: Subject) {
        val current = _subjects.value.map { if (it.id == updated.id) updated else it }
        persistSubjects(current)
    }

    fun deleteSubject(subjectId: String) {
        val current = _subjects.value.filterNot { it.id == subjectId }
        persistSubjects(current)
        // Also remove tasks belonging to this subject
        val updatedTasks = _tasks.value.filterNot { it.subjectId == subjectId }
        persistTasks(updatedTasks)
    }

    private fun loadTasks() {
        val raw = storage.getString(keyStudyTasks)
        if (!raw.isNullOrEmpty()) {
            try {
                _tasks.value = json.decodeFromString<List<StudyTask>>(raw)
            } catch (e: Exception) {
                println("TaskRepository: failed to decode tasks: ${e.message}")
            }
        }
    }

    private fun persistTasks(list: List<StudyTask>) {
        _tasks.value = list
        try {
            val encoded = json.encodeToString(list)
            storage.saveString(keyStudyTasks, encoded)
        } catch (e: Exception) {
            println("TaskRepository: failed to save tasks: ${e.message}")
        }
    }

    fun addTask(task: StudyTask) {
        val current = _tasks.value.toMutableList()
        current.add(0, task)
        persistTasks(current)
    }

    fun updateTask(updated: StudyTask) {
        val current = _tasks.value.map { if (it.id == updated.id) updated else it }
        persistTasks(current)
    }

    fun deleteTask(taskId: String) {
        val current = _tasks.value.filterNot { it.id == taskId }
        persistTasks(current)
    }

    fun toggleSubtask(taskId: String, subtaskId: String) {
        val current = _tasks.value.map { task ->
            if (task.id == taskId) {
                val newSubtasks = task.subtasks.map { sub ->
                    if (sub.id == subtaskId) sub.copy(isCompleted = !sub.isCompleted) else sub
                }
                val allDone = newSubtasks.isNotEmpty() && newSubtasks.all { it.isCompleted }
                val newStatus = if (allDone) TaskStatus.COMPLETED else if (newSubtasks.any { it.isCompleted } && task.status == TaskStatus.PENDING) TaskStatus.IN_PROGRESS else task.status
                task.copy(subtasks = newSubtasks, status = newStatus)
            } else {
                task
            }
        }
        persistTasks(current)
    }

    fun setTaskStatus(taskId: String, status: TaskStatus) {
        val current = _tasks.value.map { task ->
            if (task.id == taskId) {
                val updatedSubtasks = if (status == TaskStatus.COMPLETED) {
                    task.subtasks.map { it.copy(isCompleted = true) }
                } else {
                    task.subtasks
                }
                task.copy(status = status, subtasks = updatedSubtasks)
            } else {
                task
            }
        }
        persistTasks(current)
    }

    fun toggleTaskCompletion(taskId: String) {
        val current = _tasks.value.map { task ->
            if (task.id == taskId) {
                val isCompleted = task.status == TaskStatus.COMPLETED
                val nextStatus = if (isCompleted) TaskStatus.PENDING else TaskStatus.COMPLETED
                val updatedSubtasks = if (nextStatus == TaskStatus.COMPLETED) {
                    task.subtasks.map { it.copy(isCompleted = true) }
                } else {
                    task.subtasks.map { it.copy(isCompleted = false) }
                }
                task.copy(status = nextStatus, subtasks = updatedSubtasks)
            } else {
                task
            }
        }
        persistTasks(current)
    }

    fun clearAllData() {
        persistTasks(emptyList())
        persistSubjects(emptyList())
    }
}
