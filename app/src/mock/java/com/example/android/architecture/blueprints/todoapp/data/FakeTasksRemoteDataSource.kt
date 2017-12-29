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

package com.example.android.architecture.blueprints.todoapp.data

import android.support.annotation.VisibleForTesting
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.util.LinkedHashMap

/**
 * Implementation of a remote data source with static access to the data for easy testing.
 */
object FakeTasksRemoteDataSource : TasksDataSource {
  private val TasksServiceData = LinkedHashMap<String, Task>()

  override fun getTasks(): Single<List<Task>> {
    return Observable.fromIterable(TasksServiceData.values).toList()
  }

  override fun getTask(taskId: String): Single<Task> {
    return Single.just(TasksServiceData[taskId])
  }

  override fun saveTask(task: Task): Completable {
    TasksServiceData.put(task.id, task)
    return Completable.complete()
  }

  override fun completeTask(task: Task): Completable {
    val completedTask = Task(task.title!!, task.description, task.id, true)
    TasksServiceData.put(task.id, completedTask)
    return Completable.complete()
  }

  override fun completeTask(taskId: String): Completable {
    val task = TasksServiceData[taskId]!!
    val completedTask = Task(task.title!!, task.description, task.id, true)
    TasksServiceData.put(taskId, completedTask)
    return Completable.complete()
  }

  override fun activateTask(task: Task): Completable {
    val activeTask = Task(title = task.title!!, description = task.description!!, id = task.id)
    TasksServiceData.put(task.id, activeTask)
    return Completable.complete()
  }

  override fun activateTask(taskId: String): Completable {
    val task = TasksServiceData[taskId]!!
    val activeTask = Task(title = task.title!!, description = task.description!!, id = task.id)
    TasksServiceData.put(taskId, activeTask)
    return Completable.complete()
  }

  override fun clearCompletedTasks(): Completable {
    val it = TasksServiceData.entries.iterator()
    while (it.hasNext()) {
      val entry = it.next()
      if (entry.value.completed) {
        it.remove()
      }
    }
    return Completable.complete()
  }

  override fun refreshTasks() {
    // Not required because the {@link TasksRepository} handles the logic of refreshing the
    // tasks from all the available data sources.
  }

  override fun deleteTask(taskId: String): Completable {
    TasksServiceData.remove(taskId)
    return Completable.complete()
  }

  override fun deleteAllTasks() {
    TasksServiceData.clear()
  }

  @VisibleForTesting
  fun addTasks(vararg tasks: Task) {
    for (task in tasks) {
      TasksServiceData.put(task.id, task)
    }
  }
}
