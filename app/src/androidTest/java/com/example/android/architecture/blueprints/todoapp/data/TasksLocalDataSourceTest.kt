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

import android.support.test.InstrumentationRegistry
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.data.source.local.TasksDbHelper
import com.example.android.architecture.blueprints.todoapp.data.source.local.TasksLocalDataSource
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import com.example.android.architecture.blueprints.todoapp.util.schedulers.ImmediateSchedulerProvider
import io.reactivex.observers.TestObserver
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsCollectionContaining.hasItems
import org.hamcrest.core.IsNot.not
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for the [TasksDataSource], which uses the [TasksDbHelper].
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TasksLocalDataSourceTest {
  private lateinit var schedulerProvider: BaseSchedulerProvider
  private lateinit var localDataSource: TasksLocalDataSource

  @Before
  fun setup() {
    TasksLocalDataSource.clearInstance()
    schedulerProvider = ImmediateSchedulerProvider()

    localDataSource = TasksLocalDataSource
        .getInstance(InstrumentationRegistry.getTargetContext(), schedulerProvider)
  }

  @After
  fun cleanUp() {
    localDataSource.deleteAllTasks()
  }

  @Test
  fun testPreConditions() {
    assertNotNull(localDataSource)
  }

  @Test
  fun saveTask_retrievesTask() {
    // Given a new task
    val newTask = Task(title = TITLE, description = "")

    // When saved into the persistent repository
    localDataSource.saveTask(newTask)

    // Then the task can be retrieved from the persistent repository
    val testObserver = TestObserver<Task>()
    localDataSource.getTask(newTask.id).subscribe(testObserver)
    testObserver.assertValue(newTask)
  }

  @Test
  fun completeTask_retrievedTaskIsComplete() {
    // Given a new task in the persistent repository
    val newTask = Task(title = TITLE, description = "")
    localDataSource.saveTask(newTask)

    // When completed in the persistent repository
    localDataSource.completeTask(newTask)

    // Then the task can be retrieved from the persistent repository and is complete
    val testObserver = TestObserver<Task>()
    localDataSource.getTask(newTask.id).subscribe(testObserver)
    testObserver.assertValueCount(1)
    val (_, _, _, completed) = testObserver.values()[0]
    assertThat(completed, `is`(true))
  }

  @Test
  fun activateTask_retrievedTaskIsActive() {
    // Given a new completed task in the persistent repository
    val newTask = Task(title = TITLE, description = "")
    localDataSource.saveTask(newTask)
    localDataSource.completeTask(newTask)

    // When activated in the persistent repository
    localDataSource.activateTask(newTask)

    // Then the task can be retrieved from the persistent repository and is active
    val testObserver = TestObserver<Task>()
    localDataSource.getTask(newTask.id).subscribe(testObserver)
    testObserver.assertValueCount(1)
    val result = testObserver.values()[0]
    assertThat(result.active, `is`(true))
    assertThat(result.completed, `is`(false))
  }

  @Test
  fun clearCompletedTask_taskNotRetrievable() {
    // Given 2 new completed tasks and 1 active task in the persistent repository
    val newTask1 = Task(title = TITLE, description = "")
    localDataSource.saveTask(newTask1)
    localDataSource.completeTask(newTask1)
    val newTask2 = Task(title = TITLE2, description = "")
    localDataSource.saveTask(newTask2)
    localDataSource.completeTask(newTask2)
    val newTask3 = Task(title = TITLE3, description = "")
    localDataSource.saveTask(newTask3)

    // When completed tasks are cleared in the repository
    localDataSource.clearCompletedTasks()

    // Then the completed tasks cannot be retrieved and the active one can
    val testObserver = TestObserver<List<Task>>()
    localDataSource.getTasks().subscribe(testObserver)
    val result = testObserver.values()[0]
    assertThat(result, not(hasItems(newTask1, newTask2)))
  }

  @Test
  fun deleteAllTasks_emptyListOfRetrievedTask() {
    // Given a new task in the persistent repository and a mocked callback
    val newTask = Task(title = TITLE, description = "")
    localDataSource.saveTask(newTask)

    // When all tasks are deleted
    localDataSource.deleteAllTasks()

    // Then the retrieved tasks is an empty list
    val testObserver = TestObserver<List<Task>>()
    localDataSource.getTasks().subscribe(testObserver)
    val result = testObserver.values()[0]
    assertThat(result.isEmpty(), `is`(true))
  }

  @Test
  fun getTasks_retrieveSavedTasks() {
    // Given 2 new tasks in the persistent repository
    val newTask1 = Task(title = TITLE, description = "a")
    localDataSource.saveTask(newTask1)
    val newTask2 = Task(title = TITLE, description = "b")
    localDataSource.saveTask(newTask2)

    // Then the tasks can be retrieved from the persistent repository
    val testObserver = TestObserver<List<Task>>()
    localDataSource.getTasks().subscribe(testObserver)
    val result = testObserver.values()[0]
    assertThat(result, hasItems(newTask1, newTask2))
  }

  @Test
  fun getTask_whenTaskNotSaved() {
    //Given that no task has been saved
    //When querying for a task, null is returned.
    val testObserver = TestObserver<Task>()
    localDataSource.getTask("1").subscribe(testObserver)
    testObserver.assertEmpty()
  }

  companion object {
    private const val TITLE = "title"
    private const val TITLE2 = "title2"
    private const val TITLE3 = "title3"
  }
}
