package com.example.android.architecture.blueprints.todoapp.taskdetail

import com.example.android.architecture.blueprints.todoapp.mvibase.MviViewState

data class TaskDetailViewState(
    val title: String,
    val description: String,
    val active: Boolean,
    val loading: Boolean,
    val error: Throwable?,
    val taskComplete: Boolean,
    val taskActivated: Boolean,
    val taskDeleted: Boolean
) : MviViewState {
  companion object {
    fun idle(): TaskDetailViewState {
      return TaskDetailViewState(
          title = "",
          description = "",
          active = false,
          loading = false,
          error = null,
          taskComplete = false,
          taskActivated = false,
          taskDeleted = false)
    }
  }
}
