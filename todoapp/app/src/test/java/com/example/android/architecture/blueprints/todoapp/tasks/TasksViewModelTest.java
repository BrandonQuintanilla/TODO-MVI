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

package com.example.android.architecture.blueprints.todoapp.tasks;

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository;
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider;
import com.example.android.architecture.blueprints.todoapp.util.schedulers.ImmediateSchedulerProvider;
import com.google.common.collect.Lists;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the implementation of {@link TasksViewModel}
 */
public class TasksViewModelTest {
  private static List<Task> TASKS;
  @Mock private TasksRepository tasksRepository;
  private BaseSchedulerProvider schedulerProvider;
  private TasksViewModel tasksViewModel;
  private TestObserver<TasksViewState> testObserver;

  @Before public void setupTasksPresenter() {
    // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
    // inject the mocks in the test the initMocks method needs to be called.
    MockitoAnnotations.initMocks(this);

    // Make the sure that all schedulers are immediate.
    schedulerProvider = new ImmediateSchedulerProvider();

    // Get a reference to the class under test
    tasksViewModel = new TasksViewModel(tasksRepository, schedulerProvider);

    // We subscribe the tasks to 3, with one active and two completed
    TASKS = Lists.newArrayList(new Task("Title1", "Description1"),
        new Task("Title2", "Description2", true), new Task("Title3", "Description3", true));

    testObserver = tasksViewModel.states().test();
  }

  @Test public void loadAllTasksFromRepositoryAndLoadIntoView() {
    // Given an initialized TasksViewModel with initialized tasks
    when(tasksRepository.getTasks(anyBoolean())).thenReturn(Single.just(TASKS));
    // When loading of Tasks is requested
    tasksViewModel.processIntents(Observable.just(TasksIntent.InitialIntent.create()));

    // Then progress indicator is shown

    //Then progress indicator is shown
    testObserver.assertValueAt(0, TasksViewState::isLoading);
    // Then progress indicator is hidden and all tasks are shown in UI
    testObserver.assertValueAt(1, tasksViewState -> !tasksViewState.isLoading());
  }

  @Test public void loadActiveTasksFromRepositoryAndLoadIntoView() {
    // Given an initialized TasksViewModel with initialized tasks
    when(tasksRepository.getTasks(anyBoolean())).thenReturn(Single.just(TASKS));
    // When loading of Tasks is requested
    tasksViewModel.processIntents(
        Observable.just(TasksIntent.ChangeFilterIntent.create(TasksFilterType.ACTIVE_TASKS)));

    //Then progress indicator is shown
    testObserver.assertValueAt(0, TasksViewState::isLoading);
    // Then progress indicator is hidden and active tasks are shown in UI
    testObserver.assertValueAt(1, tasksViewState -> !tasksViewState.isLoading());
  }

  @Test public void loadCompletedTasksFromRepositoryAndLoadIntoView() {
    // Given an initialized TasksViewModel with initialized tasks
    when(tasksRepository.getTasks(anyBoolean())).thenReturn(Single.just(TASKS));
    // When loading of Tasks is requested
    tasksViewModel.processIntents(
        Observable.just(TasksIntent.ChangeFilterIntent.create(TasksFilterType.COMPLETED_TASKS)));

    //Then progress indicator is shown
    testObserver.assertValueAt(0, TasksViewState::isLoading);
    // Then progress indicator is hidden and completed tasks are shown in UI
    testObserver.assertValueAt(1, tasksViewState -> !tasksViewState.isLoading());
  }

  @Test public void completeTask_ShowsTaskMarkedComplete() {
    // Given a stubbed task
    Task task = new Task("Details Requested", "For this task");
    // And no tasks available in the repository
    when(tasksRepository.completeTask(task)).thenReturn(Completable.complete());
    when(tasksRepository.getTasks()).thenReturn(Single.just(Collections.emptyList()));

    // When task is marked as complete
    tasksViewModel.processIntents(Observable.just(TasksIntent.CompleteTaskIntent.create(task)));

    // Then repository is called and task marked complete UI is shown
    verify(tasksRepository).completeTask(task);
    verify(tasksRepository).getTasks();
    testObserver.assertValueAt(0, TasksViewState::taskComplete);
  }

  @Test public void activateTask_ShowsTaskMarkedActive() {
    // Given a stubbed completed task
    Task task = new Task("Details Requested", "For this task", true);
    // And no tasks available in the repository
    when(tasksRepository.activateTask(task)).thenReturn(Completable.complete());
    when(tasksRepository.getTasks()).thenReturn(Single.just(Collections.emptyList()));

    // When task is marked as activated
    tasksViewModel.processIntents(Observable.just(TasksIntent.ActivateTaskIntent.create(task)));

    // Then repository is called and task marked active UI is shown
    verify(tasksRepository).activateTask(task);
    verify(tasksRepository).getTasks();
    testObserver.assertValueAt(0, TasksViewState::taskActivated);
  }

  @Test public void errorLoadingTasks_ShowsError() {
    // Given that no tasks are available in the repository
    when(tasksRepository.getTasks(anyBoolean())).thenReturn(Single.error(new Exception()));

    // When tasks are loaded
    tasksViewModel.processIntents(Observable.just(TasksIntent.InitialIntent.create()));

    // Then an error message is shown
    testObserver.assertValueAt(1, state -> state.error() != null);
  }
}
