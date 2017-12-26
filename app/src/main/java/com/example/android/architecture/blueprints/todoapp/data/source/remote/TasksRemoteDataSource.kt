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

package com.example.android.architecture.blueprints.todoapp.data.source.remote

import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.util.LinkedHashMap
import java.util.concurrent.TimeUnit

/**
 * Implementation of the data source that adds a latency simulating network.
 */
object TasksRemoteDataSource : TasksDataSource {

  private const val SERVICE_LATENCY_IN_MILLIS = 5000
  private val tasksServiceData: MutableMap<String, Task>

  init {
    tasksServiceData = LinkedHashMap(2)
    addTask("Build tower in Pisa", "Ground looks good, no foundation work required.")
    addTask("Finish bridge in Tacoma", "Found awesome girders at half the cost!")
  }

  private fun addTask(title: String, description: String) {
    val newTask = Task.invoke(title, description)
    tasksServiceData.put(newTask.id, newTask)
  }

  override fun getTasks(): Single<List<Task>> {
    return Observable.fromIterable(tasksServiceData.values)
        .delay(SERVICE_LATENCY_IN_MILLIS.toLong(), TimeUnit.MILLISECONDS)
        .toList()
  }

  override fun getTask(taskId: String): Single<Task> {
    return Single.just<Task>(tasksServiceData[taskId])
        .delay(SERVICE_LATENCY_IN_MILLIS.toLong(), TimeUnit.MILLISECONDS)
  }

  override fun saveTask(task: Task): Completable {
    tasksServiceData.put(task.id, task)
    return Completable.complete()
  }

  override fun completeTask(task: Task): Completable {
    val completedTask = Task(task.title!!, task.description, task.id, true)
    tasksServiceData.put(task.id, completedTask)
    return Completable.complete()
  }

  override fun completeTask(taskId: String): Completable {
    // Not required for the remote data source because the {@link TasksRepository} handles
    // converting from a {@code taskId} to a {@link task} using its cached data.
    return Completable.complete()
  }

  override fun activateTask(task: Task): Completable {
    val activeTask = Task.invoke(task.title!!, task.description!!, task.id)
    tasksServiceData.put(task.id, activeTask)
    return Completable.complete()
  }

  override fun activateTask(taskId: String): Completable {
    // Not required for the remote data source because the {@link TasksRepository} handles
    // converting from a {@code taskId} to a {@link task} using its cached data.
    return Completable.complete()
  }

  override fun clearCompletedTasks(): Completable {
    val it = tasksServiceData.entries.iterator()
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

  override fun deleteAllTasks() {
    tasksServiceData.clear()
  }

  override fun deleteTask(taskId: String): Completable {
    tasksServiceData.remove(taskId)
    return Completable.complete()
  }
}
