package com.example.android.architecture.blueprints.todoapp.taskdetail;

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

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the implementation of {@link TaskDetailViewModel}
 */
public class TaskDetailViewModelTest {

    private TaskDetailViewModel mTaskDetailViewModel;
    @Mock
    private TasksRepository mTasksRepository;
    private BaseSchedulerProvider mSchedulerProvider;
    private TestObserver<TaskDetailViewState> mTestObserver;

    @Before
    public void setUpTaskDetailViewModel() throws Exception {
        // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
        // inject the mocks in the test the initMocks method needs to be called.
        MockitoAnnotations.initMocks(this);

        // Make the sure that all schedulers are immediate.
        mSchedulerProvider = new ImmediateSchedulerProvider();

        // Get a reference to the class under test
        mTaskDetailViewModel = new TaskDetailViewModel(
                new TaskDetailActionProcessorHolder(mTasksRepository, mSchedulerProvider));

        mTestObserver = mTaskDetailViewModel.states().test();
    }


    @Test
    public void populateTask_callsRepoAndUpdatesViewOnSuccess() {
        final Task testTask = new Task("TITLE", "DESCRIPTION");
        when(mTasksRepository.getTask(testTask.getId())).thenReturn(Single.just(testTask));

        // When populating a task is initiated by an initial intent
        mTaskDetailViewModel.processIntents(Observable.just(
                TaskDetailIntent.InitialIntent.create(testTask.getId())
        ));

        // Then the task repository is queried and a stated is emitted back
        verify(mTasksRepository).getTask(eq(testTask.getId()));
        mTestObserver.assertValueAt(1, state ->
                state.title().equals(testTask.getTitle()) &&
                        state.description().equals(testTask.getDescription()));
    }

    @Test
    public void populateTask_callsRepoAndUpdatesViewOnError() {
        final Task testTask = new Task("TITLE", "DESCRIPTION");
        when(mTasksRepository.getTask(testTask.getId()))
                .thenReturn(Single.error(new NoSuchElementException("The MaybeSource is empty")));

        // When populating a task is initiated by an initial intent
        mTaskDetailViewModel.processIntents(Observable.just(
                TaskDetailIntent.InitialIntent.create(testTask.getId())
        ));

        // Then the task repository is queried and a stated is emitted back
        verify(mTasksRepository).getTask(eq(testTask.getId()));
        mTestObserver.assertValueAt(1, state ->
                state.error() != null &&
                        state.title().isEmpty() &&
                        state.description().isEmpty());
    }

    @Test
    public void deleteTask_deletesFromRepository_showsSuccessMessageUi() {
        when(mTasksRepository.deleteTask(anyString())).thenReturn(Completable.complete());

        // When an existing task saving intent is emitted by the view
        mTaskDetailViewModel.processIntents(Observable.just(
                TaskDetailIntent.DeleteTask.create("1")
        ));

        // Then a task is saved in the repository and the view updates
        verify(mTasksRepository).deleteTask(anyString()); // saved to the model
        mTestObserver.assertValueAt(1, TaskDetailViewState::taskDeleted);
    }

    @Test
    public void deleteTask_showsErrorMessageUi() {
        when(mTasksRepository.deleteTask(anyString()))
                .thenReturn(Completable.error(new NoSuchElementException("Task does not exist")));

        // When an existing task saving intent is emitted by the view
        mTaskDetailViewModel.processIntents(Observable.just(
                TaskDetailIntent.DeleteTask.create("1")
        ));

        // Then a task is saved in the repository and the view updates
        verify(mTasksRepository).deleteTask(anyString()); // saved to the model
        mTestObserver.assertValueAt(1, state -> state.error() != null);
    }

    @Test
    public void completeTask_marksTaskAsComplete_showsSuccessMessageUi() {
        Task task = new Task("Complete Requested", "For this task");

        when(mTasksRepository.completeTask(anyString())).thenReturn(Completable.complete());
        when(mTasksRepository.getTask(anyString())).thenReturn(Single.just(task));

        // When an existing task saving intent is emitted by the view
        mTaskDetailViewModel.processIntents(Observable.just(
                TaskDetailIntent.CompleteTaskIntent.create("1")
        ));

        // Then a task is saved in the repository and the view updates
        verify(mTasksRepository).completeTask(anyString());
        verify(mTasksRepository).getTask(anyString());
        mTestObserver.assertValueAt(1, TaskDetailViewState::taskComplete);
    }

    @Test
    public void completeTask_showsErrorMessageUi() {
        when(mTasksRepository.completeTask(anyString()))
                .thenReturn(Completable.complete());
        when(mTasksRepository.getTask(anyString()))
                .thenReturn(Single.error(new NoSuchElementException("The MaybeSource is empty")));

        // When an existing task saving intent is emitted by the view
        mTaskDetailViewModel.processIntents(Observable.just(
                TaskDetailIntent.CompleteTaskIntent.create("1")
        ));

        mTestObserver.assertValueAt(1, state -> state.error() != null);
    }

    @Test
    public void activateTask_marksTaskAsActive_showsSuccessMessageUi() {
        Task task = new Task("Activate Requested", "For this task");

        when(mTasksRepository.activateTask(anyString())).thenReturn(Completable.complete());
        when(mTasksRepository.getTask(anyString())).thenReturn(Single.just(task));

        // When an existing task saving intent is emitted by the view
        mTaskDetailViewModel.processIntents(Observable.just(
                TaskDetailIntent.ActivateTaskIntent.create("1")
        ));

        // Then a task is saved in the repository and the view updates
        verify(mTasksRepository).activateTask(anyString());
        verify(mTasksRepository).getTask(anyString());
        mTestObserver.assertValueAt(1, TaskDetailViewState::taskActivated);
    }

    @Test
    public void activateTask_showsErrorMessageUi() {
        when(mTasksRepository.activateTask(anyString())).thenReturn(Completable.complete());
        when(mTasksRepository.getTask(anyString()))
                .thenReturn(Single.error(new NoSuchElementException("The MaybeSource is empty")));

        // When an existing task saving intent is emitted by the view
        mTaskDetailViewModel.processIntents(Observable.just(
                TaskDetailIntent.ActivateTaskIntent.create("1")
        ));

        mTestObserver.assertValueAt(1, state -> state.error() != null);
    }


}