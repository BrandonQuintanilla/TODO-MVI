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

package com.example.android.architecture.blueprints.todoapp.data.source

import android.support.annotation.VisibleForTesting
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.util.SingletonHolderDoubleArg
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.util.LinkedHashMap

/**
 * Concrete implementation to load tasks from the data sources into a cache.
 *
 *
 * For simplicity, this implements a dumb synchronisation between locally persisted data and data
 * obtained from the server, by using the remote data source only if the local database doesn't
 * exist or is empty.
 *
 * The class is open to allow mocking.
 */
open class TasksRepository private constructor(
    private val tasksRemoteDataSource: TasksDataSource,
    private val tasksLocalDataSource: TasksDataSource
) : TasksDataSource {

  /**
   * This variable has package local visibility so it can be accessed from tests.
   */
  @VisibleForTesting
  var cachedTasks: MutableMap<String, Task>? = null

  /**
   * Marks the cache as invalid, to force an update the next time data is requested. This variable
   * has package local visibility so it can be accessed from tests.
   */
  @VisibleForTesting
  var cacheIsDirty = false

  private fun getAndCacheLocalTasks(): Single<List<Task>> {
    return tasksLocalDataSource.getTasks()
        .flatMap { tasks ->
          Observable.fromIterable(tasks)
              .doOnNext { task -> cachedTasks!!.put(task.id, task) }
              .toList()
        }
  }

  private fun getAndSaveRemoteTasks(): Single<List<Task>> {
    return tasksRemoteDataSource.getTasks()
        .flatMap { tasks ->
          Observable.fromIterable(tasks).doOnNext { task ->
            tasksLocalDataSource.saveTask(task)
            cachedTasks!!.put(task.id, task)
          }.toList()
        }
        .doOnSuccess { cacheIsDirty = false }
  }

  /**
   * Gets tasks from cache, local data source (SQLite) or remote data source, whichever is
   * available first.
   */
  override fun getTasks(): Single<List<Task>> {
    // Respond immediately with cache if available and not dirty
    if (cachedTasks != null && !cacheIsDirty) {
      return Observable.fromIterable(cachedTasks!!.values).toList()
    } else if (cachedTasks == null) {
      cachedTasks = LinkedHashMap()
    }

    val remoteTasks = getAndSaveRemoteTasks()

    return if (cacheIsDirty) {
      remoteTasks
    } else {
      // Query the local storage if available. If not, query the network.
      val localTasks = getAndCacheLocalTasks()
      Single.concat(localTasks, remoteTasks)
          .filter { tasks -> !tasks.isEmpty() }
          .firstOrError()
    }
  }

  override fun saveTask(task: Task): Completable {
    tasksRemoteDataSource.saveTask(task)
    tasksLocalDataSource.saveTask(task)

    // Do in memory cache update to keep the app UI up to date
    if (cachedTasks == null) {
      cachedTasks = LinkedHashMap()
    }
    cachedTasks!!.put(task.id, task)
    return Completable.complete()
  }

  override fun completeTask(task: Task): Completable {
    tasksRemoteDataSource.completeTask(task)
    tasksLocalDataSource.completeTask(task)

    val completedTask =
        Task(title = task.title!!, description = task.description, id = task.id, completed = true)

    // Do in memory cache update to keep the app UI up to date
    if (cachedTasks == null) {
      cachedTasks = LinkedHashMap()
    }
    cachedTasks!!.put(task.id, completedTask)
    return Completable.complete()
  }

  override fun completeTask(taskId: String): Completable {
    val taskWithId = getTaskWithId(taskId)
    return if (taskWithId != null) {
      completeTask(taskWithId)
    } else {
      Completable.complete()
    }
  }

  override fun activateTask(task: Task): Completable {
    tasksRemoteDataSource.activateTask(task)
    tasksLocalDataSource.activateTask(task)

    val activeTask =
        Task(title = task.title!!, description = task.description, id = task.id, completed = false)

    // Do in memory cache update to keep the app UI up to date
    if (cachedTasks == null) {
      cachedTasks = LinkedHashMap()
    }
    cachedTasks!!.put(task.id, activeTask)
    return Completable.complete()
  }

  override fun activateTask(taskId: String): Completable {
    val taskWithId = getTaskWithId(taskId)
    return if (taskWithId != null) {
      activateTask(taskWithId)
    } else {
      Completable.complete()
    }
  }

  override fun deleteTask(taskId: String): Completable {
    tasksRemoteDataSource.deleteTask(checkNotNull(taskId))
    tasksLocalDataSource.deleteTask(checkNotNull(taskId))

    cachedTasks!!.remove(taskId)
    return Completable.complete()
  }

  override fun clearCompletedTasks(): Completable {
    tasksRemoteDataSource.clearCompletedTasks()
    tasksLocalDataSource.clearCompletedTasks()

    // Do in memory cache update to keep the app UI up to date
    if (cachedTasks == null) {
      cachedTasks = LinkedHashMap()
    }

    val it = cachedTasks!!.entries.iterator()
    while (it.hasNext()) {
      val entry = it.next()
      if (entry.value.completed) {
        it.remove()
      }
    }
    return Completable.complete()
  }

  /**
   * Gets tasks from local data source (sqlite) unless the table is new or empty. In that case it
   * uses the network data source. This is done to simplify the sample.
   */
  override fun getTask(taskId: String): Single<Task> {
    val cachedTask = getTaskWithId(taskId)

    // Respond immediately with cache if available
    if (cachedTask != null) {
      return Single.just(cachedTask)
    }

    // LoadAction from server/persisted if needed.

    // Do in memory cache update to keep the app UI up to date
    if (cachedTasks == null) {
      cachedTasks = LinkedHashMap()
    }

    // Is the task in the local data source? If not, query the network.
    val localTask = getTaskWithIdFromLocalRepository(taskId)
    val remoteTask = tasksRemoteDataSource.getTask(taskId)
        .doOnSuccess { task ->
          tasksLocalDataSource.saveTask(task)
          cachedTasks!!.put(task.id, task)
        }

    return Single.concat(localTask, remoteTask).firstOrError()
  }

  override fun refreshTasks() {
    cacheIsDirty = true
  }

  override fun deleteAllTasks() {
    tasksRemoteDataSource.deleteAllTasks()
    tasksLocalDataSource.deleteAllTasks()

    if (cachedTasks == null) {
      cachedTasks = LinkedHashMap()
    }
    cachedTasks!!.clear()
  }

  private fun getTaskWithId(id: String): Task? = cachedTasks?.get(id)

  fun getTaskWithIdFromLocalRepository(taskId: String): Single<Task> {
    return tasksLocalDataSource.getTask(taskId)
        .doOnSuccess { task -> cachedTasks!!.put(taskId, task) }
  }

  companion object : SingletonHolderDoubleArg<TasksRepository, TasksDataSource, TasksDataSource>(
      ::TasksRepository
  )
}
