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

package com.example.android.architecture.blueprints.todoapp.addedittask;

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository;
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider;
import com.example.android.architecture.blueprints.todoapp.util.schedulers.ImmediateSchedulerProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.NoSuchElementException;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the implementation of {@link AddEditTaskViewModel}.
 */
public class AddEditTaskViewModelTest {

    @Mock
    private TasksRepository mTasksRepository;
    private BaseSchedulerProvider mSchedulerProvider;
    private AddEditTaskViewModel mAddEditTaskViewModel;
    private TestObserver<AddEditTaskViewState> mTestObserver;

    @Before
    public void setupMocksAndView() {
        // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
        // inject the mocks in the test the initMocks method needs to be called.
        MockitoAnnotations.initMocks(this);

        mSchedulerProvider = new ImmediateSchedulerProvider();

        mAddEditTaskViewModel = new AddEditTaskViewModel(
                new AddEditTaskActionProcessorHolder(mTasksRepository, mSchedulerProvider));
        mTestObserver = mAddEditTaskViewModel.states().test();
    }

    @Test
    public void saveNewTaskToRepository_showsSuccessMessageUi() {
        // When task saving intent is emitted by the view
        mAddEditTaskViewModel.processIntents(Observable.just(
                AddEditTaskIntent.SaveTask.create(null, "New Task Title", "Some Task Description")
        ));

        // Then a task is saved in the repository and the view updates
        verify(mTasksRepository).saveTask(any(Task.class)); // saved to the model
        mTestObserver.assertValueAt(0, state -> state.isSaved() && !state.isEmpty());
    }

    @Test
    public void saveTask_emptyTaskShowsErrorUi() {
        // When an empty task's saving intent is emitted by the view
        mAddEditTaskViewModel.processIntents(Observable.just(
                AddEditTaskIntent.SaveTask.create(null, "", "")
        ));

        // Then an empty task state is emitted back to the view
        verify(mTasksRepository, never()).saveTask(any(Task.class)); // saved to the model
        mTestObserver.assertValueAt(0, AddEditTaskViewState::isEmpty);
    }

    @Test
    public void saveExistingTaskToRepository_showsSuccessMessageUi() {
        when(mTasksRepository.saveTask(any(Task.class))).thenReturn(Completable.complete());

        // When an existing task saving intent is emitted by the view
        mAddEditTaskViewModel.processIntents(Observable.just(
                AddEditTaskIntent.SaveTask.create("1", "Existing Task Title", "Some Task Description")
        ));

        // Then a task is saved in the repository and the view updates
        verify(mTasksRepository).saveTask(any(Task.class)); // saved to the model
        mTestObserver.assertValueAt(0, state -> state.isSaved() && !state.isEmpty());
    }

    @Test
    public void populateTask_callsRepoAndUpdatesViewOnSuccess() {
        final Task testTask = new Task("TITLE", "DESCRIPTION");
        when(mTasksRepository.getTask(testTask.getId())).thenReturn(Single.just(testTask));

        // When populating a task is initiated by an initial intent
        mAddEditTaskViewModel.processIntents(Observable.just(
                AddEditTaskIntent.InitialIntent.create(testTask.getId())
        ));

        // Then the task repository is queried and a stated is emitted back
        verify(mTasksRepository).getTask(eq(testTask.getId()));
        mTestObserver.assertValueAt(1, state ->
                state.title().equals(testTask.getTitle()) &&
                        state.description().equals(testTask.getDescription()));
    }

    @Test
    public void populateTask_callsRepoAndUpdatesViewOnError() {
        Task testTask = new Task("TITLE", "DESCRIPTION");
        when(mTasksRepository.getTask(testTask.getId())).thenReturn(
                Single.error(new NoSuchElementException("The MaybeSource is empty")));

        // When populating a task is initiated by an initial intent
        mAddEditTaskViewModel.processIntents(Observable.just(
                AddEditTaskIntent.InitialIntent.create(testTask.getId())
        ));

        // Then the task repository is queried and a stated is emitted back
        verify(mTasksRepository).getTask(eq(testTask.getId()));
        mTestObserver.assertValueAt(1, state ->
                state.error() != null &&
                        state.title().isEmpty() &&
                        state.description().isEmpty());
    }
}
