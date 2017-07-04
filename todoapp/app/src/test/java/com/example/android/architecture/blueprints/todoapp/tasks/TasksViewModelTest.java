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
import io.reactivex.Single;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the implementation of {@link TasksViewModel}
 */
public class TasksViewModelTest {

  private static List<Task> TASKS;

  @Mock private TasksRepository mTasksRepository;

  @Mock private TasksContract.View mTasksView;

  private BaseSchedulerProvider mSchedulerProvider;

  private TasksViewModel mTasksViewModel;

  @Before public void setupTasksPresenter() {
    // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
    // inject the mocks in the test the initMocks method needs to be called.
    MockitoAnnotations.initMocks(this);

    // Make the sure that all schedulers are immediate.
    mSchedulerProvider = new ImmediateSchedulerProvider();

    // Get a reference to the class under test
    mTasksViewModel = new TasksViewModel(mTasksRepository, mTasksView, mSchedulerProvider);

    // The presenter won't update the view unless it's active.
    when(mTasksView.isActive()).thenReturn(true);

    // We subscribe the tasks to 3, with one active and two completed
    TASKS = Lists.newArrayList(new Task("Title1", "Description1"),
        new Task("Title2", "Description2", true), new Task("Title3", "Description3", true));
  }

  @Test public void createPresenter_setsThePresenterToView() {
    // Get a reference to the class under test
    mTasksViewModel = new TasksViewModel(mTasksRepository, mTasksView, mSchedulerProvider);

    // Then the presenter is set to the view
    verify(mTasksView).setPresenter(mTasksViewModel);
  }

  @Test public void loadAllTasksFromRepositoryAndLoadIntoView() {
    // Given an initialized TasksViewModel with initialized tasks
    when(mTasksRepository.getTasks()).thenReturn(Single.just(TASKS));
    // When loading of Tasks is requested
    mTasksViewModel.setFiltering(TasksFilterType.ALL_TASKS);
    mTasksViewModel.loadTasks(true);

    // Then progress indicator is shown
    verify(mTasksView).setLoadingIndicator(true);
    // Then progress indicator is hidden and all tasks are shown in UI
    verify(mTasksView).setLoadingIndicator(false);
  }

  @Test public void loadActiveTasksFromRepositoryAndLoadIntoView() {
    // Given an initialized TasksViewModel with initialized tasks
    when(mTasksRepository.getTasks()).thenReturn(Single.just(TASKS));
    // When loading of Tasks is requested
    mTasksViewModel.setFiltering(TasksFilterType.ACTIVE_TASKS);
    mTasksViewModel.loadTasks(true);

    // Then progress indicator is hidden and active tasks are shown in UI
    verify(mTasksView).setLoadingIndicator(false);
  }

  @Test public void loadCompletedTasksFromRepositoryAndLoadIntoView() {
    // Given an initialized TasksViewModel with initialized tasks
    when(mTasksRepository.getTasks()).thenReturn(Single.just(TASKS));
    // When loading of Tasks is requested
    mTasksViewModel.setFiltering(TasksFilterType.COMPLETED_TASKS);
    mTasksViewModel.loadTasks(true);

    // Then progress indicator is hidden and completed tasks are shown in UI
    verify(mTasksView).setLoadingIndicator(false);
  }

  @Test public void clickOnFab_ShowsAddTaskUi() {
    // When adding a new task
    mTasksViewModel.addNewTask();

    // Then add task UI is shown
    verify(mTasksView).showAddTask();
  }

  @Test public void clickOnTask_ShowsDetailUi() {
    // Given a stubbed active task
    Task requestedTask = new Task("Details Requested", "For this task");

    // When open task details is requested
    mTasksViewModel.openTaskDetails(requestedTask);

    // Then task detail UI is shown
    verify(mTasksView).showTaskDetailsUi(any(String.class));
  }

  @Test public void completeTask_ShowsTaskMarkedComplete() {
    // Given a stubbed task
    Task task = new Task("Details Requested", "For this task");
    // And no tasks available in the repository
    when(mTasksRepository.getTasks()).thenReturn(Single.just(Collections.emptyList()));

    // When task is marked as complete
    mTasksViewModel.completeTask(task);

    // Then repository is called and task marked complete UI is shown
    verify(mTasksRepository).completeTask(task);
    verify(mTasksView).showTaskMarkedComplete();
  }

  @Test public void activateTask_ShowsTaskMarkedActive() {
    // Given a stubbed completed task
    Task task = new Task("Details Requested", "For this task", true);
    // And no tasks available in the repository
    when(mTasksRepository.getTasks()).thenReturn(Single.just(Collections.emptyList()));
    mTasksViewModel.loadTasks(true);

    // When task is marked as activated
    mTasksViewModel.activateTask(task);

    // Then repository is called and task marked active UI is shown
    verify(mTasksRepository).activateTask(task);
    verify(mTasksView).showTaskMarkedActive();
  }

  @Test public void errorLoadingTasks_ShowsError() {
    // Given that no tasks are available in the repository
    when(mTasksRepository.getTasks()).thenReturn(Single.error(new Exception()));

    // When tasks are loaded
    mTasksViewModel.setFiltering(TasksFilterType.ALL_TASKS);
    mTasksViewModel.loadTasks(true);

    // Then an error message is shown
    verify(mTasksView).showLoadingTasksError();
  }
}
