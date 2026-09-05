package com.jetbrains.kmpapp.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.data.ScheduleRepository
import com.jetbrains.kmpapp.data.TaskRepository
import com.jetbrains.kmpapp.data.model.StudyTask
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
    private val taskRepository: TaskRepository,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    val tasks: StateFlow<List<StudyTask>> = taskRepository.tasks

    private val _selectedSubjectFilter = MutableStateFlow<String?>(null)
    val selectedSubjectFilter: StateFlow<String?> = _selectedSubjectFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<TaskStatus?>(null)
    val selectedStatusFilter: StateFlow<TaskStatus?> = _selectedStatusFilter.asStateFlow()

    // Subjects list automatically populated from current schedule + existing tasks
    val availableSubjects: StateFlow<List<String>> = combine(
        scheduleRepository.currentLessons,
        taskRepository.tasks
    ) { lessons, currentTasks ->
        val fromSchedule = lessons.map { it.subject.trim() }.filter { it.isNotEmpty() }
        val fromTasks = currentTasks.map { it.subjectTitle.trim() }.filter { it.isNotEmpty() }
        (fromSchedule + fromTasks).distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filteredTasks: StateFlow<List<StudyTask>> = combine(
        taskRepository.tasks,
        _selectedSubjectFilter,
        _selectedStatusFilter
    ) { allTasks, subject, status ->
        allTasks.filter { task ->
            val matchSubject = subject == null || task.subjectTitle.equals(subject, ignoreCase = true)
            val matchStatus = status == null || task.status == status
            matchSubject && matchStatus
        }.sortedWith(
            compareBy<StudyTask> { it.status == TaskStatus.COMPLETED }
                .thenBy { it.priority.order }
                .thenBy { it.dueDateIso ?: "9999" }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeCount: StateFlow<Int> = taskRepository.tasks.combine(MutableStateFlow(Unit)) { list, _ ->
        list.count { it.status != TaskStatus.COMPLETED }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val completedCount: StateFlow<Int> = taskRepository.tasks.combine(MutableStateFlow(Unit)) { list, _ ->
        list.count { it.status == TaskStatus.COMPLETED }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val overallProgress: StateFlow<Int> = taskRepository.tasks.combine(MutableStateFlow(Unit)) { list, _ ->
        if (list.isEmpty()) 0 else {
            val total = list.sumOf { it.completionRatio }
            ((total / list.size) * 100).toInt()
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun selectSubjectFilter(subject: String?) {
        _selectedSubjectFilter.value = subject
    }

    fun selectStatusFilter(status: TaskStatus?) {
        _selectedStatusFilter.value = status
    }

    fun toggleSubtask(taskId: String, subtaskId: String) {
        taskRepository.toggleSubtask(taskId, subtaskId)
    }

    fun setTaskStatus(taskId: String, status: TaskStatus) {
        taskRepository.setTaskStatus(taskId, status)
    }

    fun deleteTask(taskId: String) {
        taskRepository.deleteTask(taskId)
    }

    fun createTask(
        subjectTitle: String,
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
        val task = StudyTask(
            id = id,
            subjectTitle = subjectTitle.trim(),
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

    fun createBatchLabs(subject: String, count: Int, priority: TaskPriority) {
        taskRepository.createBatchLabs(subject, count, priority)
    }

    fun clearAllTasks() {
        taskRepository.clearAllTasks()
    }
}
