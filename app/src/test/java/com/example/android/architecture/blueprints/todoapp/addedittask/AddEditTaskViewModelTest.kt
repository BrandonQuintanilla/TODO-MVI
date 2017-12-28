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

package com.example.android.architecture.blueprints.todoapp.addedittask

import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import com.example.android.architecture.blueprints.todoapp.util.schedulers.ImmediateSchedulerProvider
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import java.util.NoSuchElementException

/**
 * Unit tests for the implementation of [AddEditTaskViewModel].
 */
class AddEditTaskViewModelTest {

  @Mock
  private lateinit var tasksRepository: TasksRepository
  private lateinit var schedulerProvider: BaseSchedulerProvider
  private lateinit var addEditTaskViewModel: AddEditTaskViewModel
  private lateinit var testObserver: TestObserver<AddEditTaskViewState>

  @Before
  fun setupMocksAndView() {
    // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
    // inject the mocks in the test the initMocks method needs to be called.
    MockitoAnnotations.initMocks(this)

    schedulerProvider = ImmediateSchedulerProvider()

    addEditTaskViewModel = AddEditTaskViewModel(
        AddEditTaskActionProcessorHolder(tasksRepository, schedulerProvider)
    )
    testObserver = addEditTaskViewModel.states().test()
  }

  @Test
  fun saveNewTaskToRepository_showsSuccessMessageUi() {
    // When task saving intent is emitted by the view
    addEditTaskViewModel.processIntents(Observable.just(
        AddEditTaskIntent.SaveTask(
            taskId = null,
            title = "New Task Title",
            description = "Some Task Description")
    ))

    // Then a task is saved in the repository and the view updates
    verify<TasksRepository>(tasksRepository).saveTask(any())
    // saved to the model
    testObserver.assertValueAt(1) { (isEmpty, isSaved) -> isSaved && !isEmpty }
  }

  @Test
  fun saveTask_emptyTaskShowsErrorUi() {
    // When an empty task's saving intent is emitted by the view
    addEditTaskViewModel.processIntents(
        Observable.just(AddEditTaskIntent.SaveTask(
            taskId = null,
            title = "",
            description = ""))
    )

    // Then an empty task state is emitted back to the view
    verify<TasksRepository>(tasksRepository, never()).saveTask(any()) // saved to the model
    testObserver.assertValueAt(1, AddEditTaskViewState::isEmpty)
  }

  @Test
  fun saveExistingTaskToRepository_showsSuccessMessageUi() {
    `when`(tasksRepository.saveTask(any())).thenReturn(Completable.complete())

    // When an existing task saving intent is emitted by the view
    addEditTaskViewModel.processIntents(Observable.just(
        AddEditTaskIntent.SaveTask(
            taskId = "1",
            title = "Existing Task Title",
            description = "Some Task Description")
    ))

    // Then a task is saved in the repository and the view updates
    verify(tasksRepository).saveTask(any()) // saved to the model
    testObserver.assertValueAt(1) { (isEmpty, isSaved) -> isSaved && !isEmpty }
  }

  @Test
  fun populateTask_callsRepoAndUpdatesViewOnSuccess() {
    val testTask = Task(title = "TITLE", description = "DESCRIPTION")
    `when`(tasksRepository.getTask(testTask.id)).thenReturn(Single.just(testTask))

    // When populating a task is initiated by an initial intent
    addEditTaskViewModel.processIntents(Observable.just(
        AddEditTaskIntent.InitialIntent(testTask.id)
    ))

    // Then the task repository is queried and a stated is emitted back
    verify(tasksRepository).getTask(eq(testTask.id))
    testObserver.assertValueAt(2) { (_, _, title, description) ->
      title == testTask.title && description == testTask.description
    }
  }

  @Test
  fun populateTask_callsRepoAndUpdatesViewOnError() {
    val (id) = Task(title = "TITLE", description = "DESCRIPTION")
    `when`(tasksRepository.getTask(id)).thenReturn(
        Single.error(NoSuchElementException("The MaybeSource is empty")))

    // When populating a task is initiated by an initial intent
    addEditTaskViewModel.processIntents(Observable.just(
        AddEditTaskIntent.InitialIntent(id)
    ))

    // Then the task repository is queried and a stated is emitted back
    verify(tasksRepository).getTask(eq(id))
    testObserver.assertValueAt(2) { (_, _, title, description, error) ->
      error != null && title.isEmpty() && description.isEmpty()
    }
  }
}
