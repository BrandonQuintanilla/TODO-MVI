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

import com.example.android.architecture.blueprints.todoapp.data.Task
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import java.util.NoSuchElementException

/**
 * Unit tests for the implementation of the in-memory repository with cache.
 */
class TasksRepositoryTest {
  private lateinit var tasksRepository: TasksRepository
  private lateinit var tasksTestObserver: TestObserver<List<Task>>
  @Mock private lateinit var tasksRemoteDataSource: TasksDataSource
  @Mock private lateinit var tasksLocalDataSource: TasksDataSource

  @Before
  fun setupTasksRepository() {
    // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
    // inject the mocks in the test the initMocks method needs to be called.
    MockitoAnnotations.initMocks(this)

    // Get a reference to the class under test
    tasksRepository = TasksRepository.getInstance(tasksRemoteDataSource, tasksLocalDataSource)

    tasksTestObserver = TestObserver()
  }

  @After
  fun destroyRepositoryInstance() {
    TasksRepository.clearInstance()
  }

  @Test
  fun getTasks_repositoryCachesAfterFirstSubscription_whenTasksAvailableInLocalStorage() {
    // Given that the local data source has data available
    setTasksAvailable(tasksLocalDataSource, TASKS)
    // And the remote data source does not have any data available
    setTasksNotAvailable(tasksRemoteDataSource)

    // When two subscriptions are set
    val testObserver1 = TestObserver<List<Task>>()
    tasksRepository.getTasks().subscribe(testObserver1)

    val testObserver2 = TestObserver<List<Task>>()
    tasksRepository.getTasks().subscribe(testObserver2)

    // Then tasks were only requested once from remote and local sources
    verify<TasksDataSource>(tasksRemoteDataSource).getTasks()
    verify<TasksDataSource>(tasksLocalDataSource).getTasks()
    //
    assertFalse(tasksRepository.cacheIsDirty)
    testObserver1.assertValue(TASKS)
    testObserver2.assertValue(TASKS)
  }

  @Test
  fun getTasks_repositoryCachesAfterFirstSubscription_whenTasksAvailableInRemoteStorage() {
    // Given that the local data source has data available
    setTasksAvailable(tasksRemoteDataSource, TASKS)
    // And the remote data source does not have any data available
    setTasksNotAvailable(tasksLocalDataSource)

    // When two subscriptions are set
    val testObserver1 = TestObserver<List<Task>>()
    tasksRepository.getTasks().subscribe(testObserver1)

    val testObserver2 = TestObserver<List<Task>>()
    tasksRepository.getTasks().subscribe(testObserver2)

    // Then tasks were only requested once from remote and local sources
    verify<TasksDataSource>(tasksRemoteDataSource).getTasks()
    verify<TasksDataSource>(tasksLocalDataSource).getTasks()
    assertFalse(tasksRepository.cacheIsDirty)
    testObserver1.assertValue(TASKS)
    testObserver2.assertValue(TASKS)
  }

  @Test
  fun getTasks_requestsAllTasksFromLocalDataSource() {
    // Given that the local data source has data available
    setTasksAvailable(tasksLocalDataSource, TASKS)
    // And the remote data source does not have any data available
    setTasksNotAvailable(tasksRemoteDataSource)

    // When tasks are requested from the tasks repository
    tasksRepository.getTasks().subscribe(tasksTestObserver)

    // Then tasks are loaded from the local data source
    verify<TasksDataSource>(tasksLocalDataSource).getTasks()
    tasksTestObserver.assertValue(TASKS)
  }

  @Test
  fun saveTask_savesTaskToServiceAPI() {
    // Given a stub task with title and description
    val newTask = Task.invoke(TASK_TITLE, "Some Task Description")

    // When a task is saved to the tasks repository
    tasksRepository.saveTask(newTask)

    // Then the service API and persistent repository are called and the cache is updated
    verify<TasksDataSource>(tasksRemoteDataSource).saveTask(newTask)
    verify<TasksDataSource>(tasksLocalDataSource).saveTask(newTask)
    assertThat(tasksRepository.cachedTasks!!.size, `is`(1))
  }

  @Test
  fun completeTask_completesTaskToServiceAPIUpdatesCache() {
    // Given a stub active task with title and description added in the repository
    val newTask = Task.invoke(TASK_TITLE, "Some Task Description")
    tasksRepository.saveTask(newTask)

    // When a task is completed to the tasks repository
    tasksRepository.completeTask(newTask)

    // Then the service API and persistent repository are called and the cache is updated
    verify<TasksDataSource>(tasksRemoteDataSource).completeTask(newTask)
    verify<TasksDataSource>(tasksLocalDataSource).completeTask(newTask)

    tasksRepository.cachedTasks!!.let { cachedTasks ->
      assertThat(cachedTasks.size, `is`(1))
      assertThat(cachedTasks[newTask.id]!!.active, `is`(false))
    }
  }

  @Test
  fun completeTaskId_completesTaskToServiceAPIUpdatesCache() {
    // Given a stub active task with title and description added in the repository
    val newTask = Task.invoke(TASK_TITLE, "Some Task Description")
    tasksRepository.saveTask(newTask)

    // When a task is completed using its id to the tasks repository
    tasksRepository.completeTask(newTask.id)

    // Then the service API and persistent repository are called and the cache is updated
    verify<TasksDataSource>(tasksRemoteDataSource).completeTask(newTask)
    verify<TasksDataSource>(tasksLocalDataSource).completeTask(newTask)
    tasksRepository.cachedTasks!!.let { cachedTasks ->
      assertThat(cachedTasks.size, `is`(1))
      assertThat(cachedTasks[newTask.id]!!.active, `is`(false))
    }
  }

  @Test
  fun activateTask_activatesTaskToServiceAPIUpdatesCache() {
    // Given a stub completed task with title and description in the repository
    val newTask = Task.invoke(TASK_TITLE, "Some Task Description", true)
    tasksRepository.saveTask(newTask)

    // When a completed task is activated to the tasks repository
    tasksRepository.activateTask(newTask)

    // Then the service API and persistent repository are called and the cache is updated
    verify<TasksDataSource>(tasksRemoteDataSource).activateTask(newTask)
    verify<TasksDataSource>(tasksLocalDataSource).activateTask(newTask)
    tasksRepository.cachedTasks!!.let { cachedTasks ->
      assertThat(cachedTasks.size, `is`(1))
      assertThat(cachedTasks[newTask.id]!!.active, `is`(true))
    }
  }

  @Test
  fun activateTaskId_activatesTaskToServiceAPIUpdatesCache() {
    // Given a stub completed task with title and description in the repository
    val newTask = Task.invoke(TASK_TITLE, "Some Task Description", true)
    tasksRepository.saveTask(newTask)

    // When a completed task is activated with its id to the tasks repository
    tasksRepository.activateTask(newTask.id)

    // Then the service API and persistent repository are called and the cache is updated
    verify<TasksDataSource>(tasksRemoteDataSource).activateTask(newTask)
    verify<TasksDataSource>(tasksLocalDataSource).activateTask(newTask)
    tasksRepository.cachedTasks!!.let { cachedTasks ->
      assertThat(cachedTasks.size, `is`(1))
      assertThat(cachedTasks[newTask.id]!!.active, `is`(true))
    }
  }

  @Test
  fun getTask_requestsSingleTaskFromLocalDataSource() {
    // Given a stub completed task with title and description in the local repository
    val task = Task.invoke(TASK_TITLE, "Some Task Description", true)
    setTaskAvailable(tasksLocalDataSource, task)
    // And the task not available in the remote repository
    setTaskNotAvailable(tasksRemoteDataSource, task.id)

    // When a task is requested from the tasks repository
    val testObserver = TestObserver<Task>()
    tasksRepository.getTask(task.id).subscribe(testObserver)

    // Then the task is loaded from the database
    verify<TasksDataSource>(tasksLocalDataSource).getTask(eq(task.id))
    testObserver.assertValue(task)
  }

  @Test
  fun getTask_whenDataNotLocal_fails() {
    // Given a stub completed task with title and description in the remote repository
    val task = Task.invoke(TASK_TITLE, "Some Task Description", true)
    setTaskAvailable(tasksRemoteDataSource, task)
    // And the task not available in the local repository
    setTaskNotAvailable(tasksLocalDataSource, task.id)

    // When a task is requested from the tasks repository
    val testObserver = TestObserver<Task>()
    tasksRepository.getTask(task.id).subscribe(testObserver)

    // Verify no data is returned
    testObserver.assertError(NoSuchElementException::class.java)
  }

  @Test
  fun deleteCompletedTasks_deleteCompletedTasksToServiceAPIUpdatesCache() {
    // Given 2 stub completed tasks and 1 stub active tasks in the repository
    val newTask = Task.invoke(TASK_TITLE, "Some Task Description", true)
    tasksRepository.saveTask(newTask)
    val newTask2 = Task.invoke(TASK_TITLE2, "Some Task Description")
    tasksRepository.saveTask(newTask2)
    val newTask3 = Task.invoke(TASK_TITLE3, "Some Task Description", true)
    tasksRepository.saveTask(newTask3)

    // When a completed tasks are cleared to the tasks repository
    tasksRepository.clearCompletedTasks()

    // Then the service API and persistent repository are called and the cache is updated
    verify<TasksDataSource>(tasksRemoteDataSource).clearCompletedTasks()
    verify<TasksDataSource>(tasksLocalDataSource).clearCompletedTasks()

    tasksRepository.cachedTasks!!.let { cachedTasks ->
      assertThat(cachedTasks.size, `is`(1))
      assertTrue(cachedTasks[newTask2.id]!!.active)
      assertThat<String>(cachedTasks[newTask2.id]!!.title, `is`(TASK_TITLE2))
    }
  }

  @Test
  fun deleteAllTasks_deleteTasksToServiceAPIUpdatesCache() {
    // Given 2 stub completed tasks and 1 stub active tasks in the repository
    val newTask = Task.invoke(TASK_TITLE, "Some Task Description", true)
    tasksRepository.saveTask(newTask)
    val newTask2 = Task.invoke(TASK_TITLE2, "Some Task Description")
    tasksRepository.saveTask(newTask2)
    val newTask3 = Task.invoke(TASK_TITLE3, "Some Task Description", true)
    tasksRepository.saveTask(newTask3)

    // When all tasks are deleted to the tasks repository
    tasksRepository.deleteAllTasks()

    // Verify the data sources were called
    verify<TasksDataSource>(tasksRemoteDataSource).deleteAllTasks()
    verify<TasksDataSource>(tasksLocalDataSource).deleteAllTasks()

    assertThat(tasksRepository.cachedTasks!!.size, `is`(0))
  }

  @Test
  fun deleteTask_deleteTaskToServiceAPIRemovedFromCache() {
    // Given a task in the repository
    val newTask = Task.invoke(TASK_TITLE, "Some Task Description", true)
    tasksRepository.saveTask(newTask)
    assertThat(tasksRepository.cachedTasks!!.containsKey(newTask.id), `is`(true))

    // When deleted
    tasksRepository.deleteTask(newTask.id)

    // Verify the data sources were called
    verify<TasksDataSource>(tasksRemoteDataSource).deleteTask(newTask.id)
    verify<TasksDataSource>(tasksLocalDataSource).deleteTask(newTask.id)

    // Verify it's removed from repository
    assertThat(tasksRepository.cachedTasks!!.containsKey(newTask.id), `is`(false))
  }

  @Test
  fun getTasksWithDirtyCache_tasksAreRetrievedFromRemote() {
    // Given that the remote data source has data available
    setTasksAvailable(tasksRemoteDataSource, TASKS)

    // When calling getTasks in the repository with dirty cache
    tasksRepository.refreshTasks()
    tasksRepository.getTasks().subscribe(tasksTestObserver)

    // Verify the tasks from the remote data source are returned, not the local
    verify<TasksDataSource>(tasksLocalDataSource, never()).getTasks()
    verify<TasksDataSource>(tasksRemoteDataSource).getTasks()
    tasksTestObserver.assertValue(TASKS)
  }

  @Test
  fun getTasksWithLocalDataSourceUnavailable_tasksAreRetrievedFromRemote() {
    // Given that the local data source has no data available
    setTasksNotAvailable(tasksLocalDataSource)
    // And the remote data source has data available
    setTasksAvailable(tasksRemoteDataSource, TASKS)

    // When calling getTasks in the repository
    tasksRepository.getTasks().subscribe(tasksTestObserver)

    // Verify the tasks from the remote data source are returned
    verify<TasksDataSource>(tasksRemoteDataSource).getTasks()
    tasksTestObserver.assertValue(TASKS)
  }

  @Test
  fun getTasksWithBothDataSourcesUnavailable_firesOnDataUnavailable() {
    // Given that the local data source has no data available
    setTasksNotAvailable(tasksLocalDataSource)
    // And the remote data source has no data available
    setTasksNotAvailable(tasksRemoteDataSource)

    // When calling getTasks in the repository
    tasksRepository.getTasks().subscribe(tasksTestObserver)

    // Verify no data is returned
    tasksTestObserver.assertNoValues()
    // Verify that error is returned
    tasksTestObserver.assertError(NoSuchElementException::class.java)
  }

  @Test
  fun getTaskWithBothDataSourcesUnavailable_firesOnError() {
    // Given a task id
    val taskId = "123"
    // And the local data source has no data available
    setTaskNotAvailable(tasksLocalDataSource, taskId)
    // And the remote data source has no data available
    setTaskNotAvailable(tasksRemoteDataSource, taskId)

    // When calling getTask in the repository
    val testObserver = TestObserver<Task>()
    tasksRepository.getTask(taskId).subscribe(testObserver)

    // Verify that error is returned
    testObserver.assertError(NoSuchElementException::class.java)
  }

  @Test
  fun getTasks_refreshesLocalDataSource() {
    // Given that the remote data source has data available
    setTasksAvailable(tasksRemoteDataSource, TASKS)

    // Mark cache as dirty to force a reload of data from remote data source.
    tasksRepository.refreshTasks()

    // When calling getTasks in the repository
    tasksRepository.getTasks().subscribe(tasksTestObserver)

    // Verify that the data fetched from the remote data source was saved in local.
    verify<TasksDataSource>(tasksLocalDataSource, times(TASKS.size)).saveTask(any())
    tasksTestObserver.assertValue(TASKS)
  }

  private fun setTasksNotAvailable(dataSource: TasksDataSource) {
    `when`(dataSource.getTasks()).thenReturn(Single.just(emptyList()))
  }

  private fun setTasksAvailable(dataSource: TasksDataSource, tasks: List<Task>) {
    // don't allow the data sources to complete.
    `when`(dataSource.getTasks()).thenReturn(
        Single.just(tasks).concatWith(Single.never()).firstOrError())
  }

  private fun setTaskNotAvailable(dataSource: TasksDataSource, taskId: String) {
    `when`(dataSource.getTask(eq(taskId)))
        .thenReturn(Single.error(NoSuchElementException("The MaybeSource is empty")))
  }

  private fun setTaskAvailable(dataSource: TasksDataSource, task: Task) {
    `when`(dataSource.getTask(eq(task.id))).thenReturn(Single.just(task))
  }

  companion object {
    private const val TASK_TITLE = "title"
    private const val TASK_TITLE2 = "title2"
    private const val TASK_TITLE3 = "title3"
    private val TASKS = listOf(
        Task.invoke("Title1", "Description1"),
        Task.invoke("Title2", "Description2"))
  }
}
