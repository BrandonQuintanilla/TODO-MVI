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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the implementation of {@link TasksViewModel}
 */
public class TasksViewModelTest {
    private static List<Task> TASKS;
    @Mock
    private TasksRepository mTasksRepository;
    private BaseSchedulerProvider mSchedulerProvider;
    private TasksViewModel mTasksViewModel;
    private TestObserver<TasksViewState> mTestObserver;

    @Before
    public void setupTasksViewModel() {
        // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
        // inject the mocks in the test the initMocks method needs to be called.
        MockitoAnnotations.initMocks(this);

        // Make the sure that all schedulers are immediate.
        mSchedulerProvider = new ImmediateSchedulerProvider();

        // Get a reference to the class under test
        mTasksViewModel = new TasksViewModel(
                new TasksActionProcessorHolder(mTasksRepository, mSchedulerProvider));

        // We subscribe the tasks to 3, with one active and two completed
        TASKS = Lists.newArrayList(new Task("Title1", "Description1"),
                new Task("Title2", "Description2", true), new Task("Title3", "Description3", true));

        mTestObserver = mTasksViewModel.states().test();
    }

    @Test
    public void loadAllTasksFromRepositoryAndLoadIntoView() {
        // Given an initialized TasksViewModel with initialized tasks
        when(mTasksRepository.getTasks(anyBoolean())).thenReturn(Single.just(TASKS));
        // When loading of Tasks is initiated
        mTasksViewModel.processIntents(Observable.just(TasksIntent.InitialIntent.create()));

        // Then progress indicator state is emitted
        mTestObserver.assertValueAt(1, TasksViewState::isLoading);
        // Then progress indicator state is canceled and all tasks are emitted
        mTestObserver.assertValueAt(2, tasksViewState -> !tasksViewState.isLoading());
    }

    @Test
    public void loadActiveTasksFromRepositoryAndLoadIntoView() {
        // Given an initialized TasksViewModel with initialized tasks
        when(mTasksRepository.getTasks(anyBoolean())).thenReturn(Single.just(TASKS));
        // When loading of Tasks is initiated
        mTasksViewModel.processIntents(
                Observable.just(TasksIntent.ChangeFilterIntent.create(TasksFilterType.ACTIVE_TASKS)));

        // Then progress indicator state is emitted
        mTestObserver.assertValueAt(1, TasksViewState::isLoading);
        // Then progress indicator state is canceled and all tasks are emitted
        mTestObserver.assertValueAt(2, tasksViewState -> !tasksViewState.isLoading());
    }

    @Test
    public void loadCompletedTasksFromRepositoryAndLoadIntoView() {
        // Given an initialized TasksViewModel with initialized tasks
        when(mTasksRepository.getTasks(anyBoolean())).thenReturn(Single.just(TASKS));
        // When loading of Tasks is requested
        mTasksViewModel.processIntents(
                Observable.just(TasksIntent.ChangeFilterIntent.create(TasksFilterType.COMPLETED_TASKS)));

        // Then progress indicator state is emitted
        mTestObserver.assertValueAt(1, TasksViewState::isLoading);
        // Then progress indicator state is canceled and all tasks are emitted
        mTestObserver.assertValueAt(2, tasksViewState -> !tasksViewState.isLoading());
    }

    @Test
    public void completeTask_ShowsTaskMarkedComplete() {
        // Given a stubbed task
        Task task = new Task("Details Requested", "For this task");
        // And no tasks available in the repository
        when(mTasksRepository.completeTask(task)).thenReturn(Completable.complete());
        when(mTasksRepository.getTasks()).thenReturn(Single.just(Collections.emptyList()));

        // When task is marked as complete
        mTasksViewModel.processIntents(Observable.just(TasksIntent.CompleteTaskIntent.create(task)));

        // Then repository is called and task marked complete state is emitted
        verify(mTasksRepository).completeTask(task);
        verify(mTasksRepository).getTasks();
        mTestObserver.assertValueAt(2, TasksViewState::taskComplete);
    }

    @Test
    public void activateTask_ShowsTaskMarkedActive() {
        // Given a stubbed completed task
        Task task = new Task("Details Requested", "For this task", true);
        // And no tasks available in the repository
        when(mTasksRepository.activateTask(task)).thenReturn(Completable.complete());
        when(mTasksRepository.getTasks()).thenReturn(Single.just(Collections.emptyList()));

        // When task is marked as activated
        mTasksViewModel.processIntents(Observable.just(TasksIntent.ActivateTaskIntent.create(task)));

        // Then repository is called and task marked active state is emitted
        verify(mTasksRepository).activateTask(task);
        verify(mTasksRepository).getTasks();
        mTestObserver.assertValueAt(2, TasksViewState::taskActivated);
    }

    @Test
    public void errorLoadingTasks_ShowsError() {
        // Given that no tasks are available in the repository
        when(mTasksRepository.getTasks(anyBoolean())).thenReturn(Single.error(new Exception()));

        // When tasks are loaded
        mTasksViewModel.processIntents(Observable.just(TasksIntent.InitialIntent.create()));

        // Then an error containing state is emitted
        mTestObserver.assertValueAt(2, state -> state.error() != null);
    }
}
