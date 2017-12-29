package com.example.android.architecture.blueprints.todoapp.tasks

import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.mvibase.MviViewState
import com.example.android.architecture.blueprints.todoapp.tasks.TasksFilterType.ALL_TASKS

data class TasksViewState(
    val isLoading: Boolean,
    val tasksFilterType: TasksFilterType,
    val tasks: List<Task>,
    val error: Throwable?,
    val taskComplete: Boolean,
    val taskActivated: Boolean,
    val completedTasksCleared: Boolean
) : MviViewState {
  companion object {
    fun idle(): TasksViewState {
      return TasksViewState(
          isLoading = false,
          tasksFilterType = ALL_TASKS,
          tasks = emptyList(),
          error = null,
          taskComplete = false,
          taskActivated = false,
          completedTasksCleared = false
      )
    }
  }
}
