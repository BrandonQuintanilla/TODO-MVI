package com.example.android.architecture.blueprints.todoapp.data

import com.google.common.base.Strings
import java.util.UUID

// TODO(benoit) try to unnullablify title and description
data class Task @JvmOverloads constructor(
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
}