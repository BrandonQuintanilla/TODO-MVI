package com.example.android.architecture.blueprints.todoapp.data

import com.google.common.base.Strings
import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String?,
    val description: String?,
    val completed: Boolean = false
) {
  val titleForList = if (!Strings.isNullOrEmpty(title)) {
    title
  } else {
    description
  }

  val active = !completed

  val empty = Strings.isNullOrEmpty(title) && Strings.isNullOrEmpty(description)

  // TODO(benoit) remove those when Java is gone
  companion object {
    operator fun invoke(title: String, description: String): Task {
      return Task(title = title, description = description)
    }

    operator fun invoke(title: String, description: String, id: String): Task {
      return Task(title = title, description = description, id = id)
    }

    operator fun invoke(title: String, description: String, completed: Boolean): Task {
      return Task(title = title, description = description, completed = completed)
    }
  }
}