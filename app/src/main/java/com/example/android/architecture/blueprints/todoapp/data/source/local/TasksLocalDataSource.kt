/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.architecture.blueprints.todoapp.data.source.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.text.TextUtils
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.data.source.local.TasksPersistenceContract.TaskEntry
import com.example.android.architecture.blueprints.todoapp.util.SingletonHolderDoubleArg
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import com.squareup.sqlbrite2.BriteDatabase
import com.squareup.sqlbrite2.SqlBrite
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.Function

/**
 * Concrete implementation of a data source as a db.
 */
class TasksLocalDataSource private constructor(
    context: Context,
    schedulerProvider: BaseSchedulerProvider
) : TasksDataSource {

  private val databaseHelper: BriteDatabase
  private val taskMapperFunction: Function<Cursor, Task>

  init {
    val dbHelper = TasksDbHelper(context)
    val sqlBrite = SqlBrite.Builder().build()
    databaseHelper = sqlBrite.wrapDatabaseHelper(dbHelper, schedulerProvider.io())
    taskMapperFunction = Function { this.getTask(it) }
  }

  private fun getTask(c: Cursor): Task {
    val itemId = c.getString(c.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_ENTRY_ID))
    val title = c.getString(c.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_TITLE))
    val description = c.getString(c.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_DESCRIPTION))
    val completed = c.getInt(c.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_COMPLETED)) == 1
    return Task(title, description, itemId, completed)
  }

  override fun getTasks(): Single<List<Task>> {
    val projection = arrayOf(
        TaskEntry.COLUMN_NAME_ENTRY_ID, TaskEntry.COLUMN_NAME_TITLE,
        TaskEntry.COLUMN_NAME_DESCRIPTION, TaskEntry.COLUMN_NAME_COMPLETED)

    val sql = String.format("SELECT %s FROM %s",
        TextUtils.join(",", projection),
        TaskEntry.TABLE_NAME)

    return databaseHelper
        .createQuery(TaskEntry.TABLE_NAME, sql)
        .mapToList(taskMapperFunction)
        .firstOrError()
  }

  override fun getTask(taskId: String): Single<Task> {
    val projection = arrayOf(
        TaskEntry.COLUMN_NAME_ENTRY_ID, TaskEntry.COLUMN_NAME_TITLE,
        TaskEntry.COLUMN_NAME_DESCRIPTION, TaskEntry.COLUMN_NAME_COMPLETED)

    val sql = String.format("SELECT %s FROM %s WHERE %s LIKE ?",
        TextUtils.join(",", projection),
        TaskEntry.TABLE_NAME, TaskEntry.COLUMN_NAME_ENTRY_ID)

    return databaseHelper
        .createQuery(TaskEntry.TABLE_NAME, sql, taskId)
        .mapToOne(taskMapperFunction)
        .firstOrError()
  }

  override fun saveTask(task: Task): Completable {
    val values = ContentValues()
    values.put(TaskEntry.COLUMN_NAME_ENTRY_ID, task.id)
    values.put(TaskEntry.COLUMN_NAME_TITLE, task.title)
    values.put(TaskEntry.COLUMN_NAME_DESCRIPTION, task.description)
    values.put(TaskEntry.COLUMN_NAME_COMPLETED, task.completed)
    databaseHelper.insert(TaskEntry.TABLE_NAME, values, SQLiteDatabase.CONFLICT_REPLACE)
    return Completable.complete()
  }

  override fun completeTask(task: Task): Completable {
    completeTask(task.id)
    return Completable.complete()
  }

  override fun completeTask(taskId: String): Completable {
    val values = ContentValues()
    values.put(TaskEntry.COLUMN_NAME_COMPLETED, true)

    val selection = TaskEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?"
    val selectionArgs = arrayOf(taskId)
    databaseHelper.update(TaskEntry.TABLE_NAME, values, selection, *selectionArgs)
    return Completable.complete()
  }

  override fun activateTask(task: Task): Completable {
    activateTask(task.id)
    return Completable.complete()
  }

  override fun activateTask(taskId: String): Completable {
    val values = ContentValues()
    values.put(TaskEntry.COLUMN_NAME_COMPLETED, false)

    val selection = TaskEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?"
    val selectionArgs = arrayOf(taskId)
    databaseHelper.update(TaskEntry.TABLE_NAME, values, selection, *selectionArgs)
    return Completable.complete()
  }

  override fun clearCompletedTasks(): Completable {
    val selection = TaskEntry.COLUMN_NAME_COMPLETED + " LIKE ?"
    val selectionArgs = arrayOf("1")
    databaseHelper.delete(TaskEntry.TABLE_NAME, selection, *selectionArgs)
    return Completable.complete()
  }

  override fun refreshTasks() {
    // Not required because the [TasksRepository] handles the logic of refreshing the
    // tasks from all the available data sources.
  }

  override fun deleteAllTasks() {
    databaseHelper.delete(TaskEntry.TABLE_NAME, null)
  }

  override fun deleteTask(taskId: String): Completable {
    val selection = TaskEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?"
    val selectionArgs = arrayOf(taskId)
    databaseHelper.delete(TaskEntry.TABLE_NAME, selection, *selectionArgs)
    return Completable.complete()
  }

  companion object : SingletonHolderDoubleArg<TasksLocalDataSource, Context, BaseSchedulerProvider>(
      ::TasksLocalDataSource
  )
}
