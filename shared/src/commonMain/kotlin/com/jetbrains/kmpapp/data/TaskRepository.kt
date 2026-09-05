package com.jetbrains.kmpapp.data

import com.jetbrains.kmpapp.data.model.StudyTask
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
import kotlin.random.Random

class TaskRepository(
    private val storage: PlatformStorage
) {
    private val keyStudyTasks = "saved_study_tasks_list"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _tasks = MutableStateFlow<List<StudyTask>>(emptyList())
    val tasks: StateFlow<List<StudyTask>> = _tasks.asStateFlow()

    init {
        loadTasks()
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

    fun createBatchLabs(subjectTitle: String, count: Int, defaultPriority: TaskPriority = TaskPriority.MEDIUM) {
        val newTasks = (1..count).map { num ->
            StudyTask(
                id = generateId(),
                subjectTitle = subjectTitle.trim(),
                title = "Лабораторная работа №$num",
                taskDescription = "Выполнение и защита лабораторной работы №$num по дисциплине $subjectTitle",
                category = TaskCategory.LAB,
                priority = defaultPriority,
                status = TaskStatus.PENDING,
                subtasks = listOf(
                    Subtask(id = generateId(), title = "Изучение теории и методички", isCompleted = false),
                    Subtask(id = generateId(), title = "Выполнение практической части", isCompleted = false),
                    Subtask(id = generateId(), title = "Оформление отчёта", isCompleted = false),
                    Subtask(id = generateId(), title = "Защита у преподавателя", isCompleted = false)
                ),
                createdAtIso = ""
            )
        }
        val combined = newTasks + _tasks.value
        persistTasks(combined)
    }

    fun clearAllTasks() {
        persistTasks(emptyList())
    }

    private fun generateId(): String {
        val randomPart = Random.nextInt(100000, 999999)
        val timePart = kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds
        return "task_${timePart}_$randomPart"
    }
}
