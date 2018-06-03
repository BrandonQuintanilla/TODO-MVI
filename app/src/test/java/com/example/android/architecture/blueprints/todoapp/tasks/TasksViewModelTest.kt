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

package com.example.android.architecture.blueprints.todoapp.tasks

import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.tasks.TasksViewState.UiNotification.TASK_ACTIVATED
import com.example.android.architecture.blueprints.todoapp.tasks.TasksViewState.UiNotification.TASK_COMPLETE
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import com.example.android.architecture.blueprints.todoapp.util.schedulers.ImmediateSchedulerProvider
import com.nhaarman.mockito_kotlin.any
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

/**
 * Unit tests for the implementation of [TasksViewModel]
 */
class TasksViewModelTest {
  @Mock
  private lateinit var tasksRepository: TasksRepository
  private lateinit var schedulerProvider: BaseSchedulerProvider
  private lateinit var tasksViewModel: TasksViewModel
  private lateinit var testObserver: TestObserver<TasksViewState>
  private lateinit var tasks: List<Task>

  @Before
  fun setupTasksViewModel() {
    // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
    // inject the mocks in the test the initMocks method needs to be called.
    MockitoAnnotations.initMocks(this)

    // Make the sure that all schedulers are immediate.
    schedulerProvider = ImmediateSchedulerProvider()

    // Get a reference to the class under test
    tasksViewModel = TasksViewModel(TasksActionProcessorHolder(tasksRepository, schedulerProvider))

    // We subscribe the tasks to 3, with one active and two completed
    tasks = listOf(
        Task(title = "Title1", description = "Description1", completed = false),
        Task(title = "Title2", description = "Description2", completed = true),
        Task(title = "Title3", description = "Description3", completed = true)
    )

    testObserver = tasksViewModel.states()
        .test()
  }

  @Test
  fun loadAllTasksFromRepositoryAndLoadIntoView() {
    // Given an initialized TasksViewModel with initialized tasks
    `when`(tasksRepository.getTasks(any())).thenReturn(Single.just(tasks))
    // When loading of Tasks is initiated
    tasksViewModel.processIntents(Observable.just(TasksIntent.InitialIntent))

    // Then progress indicator state is emitted
    testObserver.assertValueAt(1, TasksViewState::isLoading)
    // Then progress indicator state is canceled and all tasks are emitted
    testObserver.assertValueAt(2) { tasksViewState -> !tasksViewState.isLoading }
  }

  @Test
  fun loadActiveTasksFromRepositoryAndLoadIntoView() {
    // Given an initialized TasksViewModel with initialized tasks
    `when`(tasksRepository.getTasks(any())).thenReturn(Single.just(tasks))
    // When loading of Tasks is initiated
    tasksViewModel.processIntents(
        Observable.just(TasksIntent.ChangeFilterIntent(TasksFilterType.ACTIVE_TASKS))
    )

    // Then progress indicator state is emitted
    testObserver.assertValueAt(1, TasksViewState::isLoading)
    // Then progress indicator state is canceled and all tasks are emitted
    testObserver.assertValueAt(2) { tasksViewState -> !tasksViewState.isLoading }
  }

  @Test
  fun loadCompletedTasksFromRepositoryAndLoadIntoView() {
    // Given an initialized TasksViewModel with initialized tasks
    `when`(tasksRepository.getTasks(any())).thenReturn(Single.just(tasks))
    // When loading of Tasks is requested
    tasksViewModel.processIntents(
        Observable.just(TasksIntent.ChangeFilterIntent(TasksFilterType.COMPLETED_TASKS))
    )

    // Then progress indicator state is emitted
    testObserver.assertValueAt(1, TasksViewState::isLoading)
    // Then progress indicator state is canceled and all tasks are emitted
    testObserver.assertValueAt(2) { tasksViewState -> !tasksViewState.isLoading }
  }

  @Test
  fun completeTask_ShowsTaskMarkedComplete() {
    // Given a stubbed task
    val task = Task(title = "Details Requested", description = "For this task")
    // And no tasks available in the repository
    `when`(tasksRepository.completeTask(task)).thenReturn(Completable.complete())
    `when`(tasksRepository.getTasks()).thenReturn(Single.just(emptyList()))

    // When task is marked as complete
    tasksViewModel.processIntents(Observable.just(TasksIntent.CompleteTaskIntent(task)))

    // Then repository is called and task marked complete state is emitted
    verify(tasksRepository).completeTask(task)
    verify(tasksRepository).getTasks()
    testObserver.assertValueAt(1) { it.uiNotification == TASK_COMPLETE }
  }

  @Test
  fun activateTask_ShowsTaskMarkedActive() {
    // Given a stubbed completed task
    val task = Task(title = "Details Requested", description = "For this task", completed = true)
    // And no tasks available in the repository
    `when`(tasksRepository.activateTask(task)).thenReturn(Completable.complete())
    `when`(tasksRepository.getTasks()).thenReturn(Single.just(emptyList()))

    // When task is marked as activated
    tasksViewModel.processIntents(Observable.just(TasksIntent.ActivateTaskIntent(task)))

    // Then repository is called and task marked active state is emitted
    verify(tasksRepository).activateTask(task)
    verify(tasksRepository).getTasks()
    testObserver.assertValueAt(1) { it.uiNotification == TASK_ACTIVATED }
  }

  @Test
  fun errorLoadingTasks_ShowsError() {
    // Given that no tasks are available in the repository
    `when`(tasksRepository.getTasks(any())).thenReturn(Single.error(Exception()))

    // When tasks are loaded
    tasksViewModel.processIntents(Observable.just(TasksIntent.InitialIntent))

    // Then an error containing state is emitted
    testObserver.assertValueAt(2) { state -> state.error != null }
  }
}
