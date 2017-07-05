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

package com.example.android.architecture.blueprints.todoapp.statistics;

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository;
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider;
import com.example.android.architecture.blueprints.todoapp.util.schedulers.ImmediateSchedulerProvider;
import com.google.common.collect.Lists;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the implementation of {@link StatisticsViewModel}
 */
public class StatisticsViewModelTest {

  private static List<Task> TASKS;
  @Mock private TasksRepository tasksRepository;
  private BaseSchedulerProvider schedulerProvider;
  private StatisticsViewModel statisticsViewModel;
  private TestObserver<StatisticsViewState> testObserver;

  @Before public void setupStatisticsPresenter() {
    // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
    // inject the mocks in the test the initMocks method needs to be called.
    MockitoAnnotations.initMocks(this);

    // Make the sure that all schedulers are immediate.
    schedulerProvider = new ImmediateSchedulerProvider();

    // Get a reference to the class under test
    statisticsViewModel = new StatisticsViewModel(tasksRepository, schedulerProvider);

    // We subscribe the tasks to 3, with one active and two completed
    TASKS = Lists.newArrayList(new Task("Title1", "Description1"),
        new Task("Title2", "Description2", true), new Task("Title3", "Description3", true));

    testObserver = statisticsViewModel.states().test();
  }

  @Test public void loadEmptyTasksFromRepository_CallViewToDisplay() {
    // Given an initialized StatisticsViewModel with no tasks
    TASKS.clear();
    setTasksAvailable(TASKS);

    // When loading of Tasks is requested
    statisticsViewModel.forwardIntents(Observable.just(StatisticsIntent.InitialIntent.create()));

    //Then progress indicator is shown
    testObserver.assertValueAt(0, StatisticsViewState::isLoading);

    // Callback is captured and invoked with stubbed tasks
    verify(tasksRepository).getTasks();

    // Then progress indicator is hidden and correct data is passed on to the view
    testObserver.assertValueAt(1,
        state -> !state.isLoading() && state.activeCount() == 0 && state.completedCount() == 0);
  }

  @Test public void loadNonEmptyTasksFromRepository_CallViewToDisplay() {
    // Given an initialized StatisticsViewModel with 1 active and 2 completed tasks
    setTasksAvailable(TASKS);

    // When loading of Tasks is requested
    statisticsViewModel.forwardIntents(Observable.just(StatisticsIntent.InitialIntent.create()));

    //Then progress indicator is shown
    testObserver.assertValueAt(0, StatisticsViewState::isLoading);

    // Then progress indicator is hidden and correct data is passed on to the view
    testObserver.assertValueAt(1,
        state -> !state.isLoading() && state.activeCount() == 1 && state.completedCount() == 2);
  }

  @Test public void loadStatisticsWhenTasksAreUnavailable_CallErrorToDisplay() {
    // Given that tasks data isn't available
    setTasksNotAvailable();

    // When statistics are loaded
    statisticsViewModel.forwardIntents(Observable.just(StatisticsIntent.InitialIntent.create()));

    // Then an error message is shown
    testObserver.assertValueAt(1, state -> state.error() != null);
  }

  private void setTasksAvailable(List<Task> tasks) {
    when(tasksRepository.getTasks()).thenReturn(Single.just(tasks));
  }

  private void setTasksNotAvailable() {
    when(tasksRepository.getTasks()).thenReturn(Single.error(new Exception()));
  }
}
