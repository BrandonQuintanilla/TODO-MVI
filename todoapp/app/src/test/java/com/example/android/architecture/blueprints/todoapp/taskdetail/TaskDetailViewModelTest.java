package com.example.android.architecture.blueprints.todoapp.taskdetail;

import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskIntent;
import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository;
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider;
import com.example.android.architecture.blueprints.todoapp.util.schedulers.ImmediateSchedulerProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the implementation of {@link TaskDetailViewModel}
 */
public class TaskDetailViewModelTest {

    private TaskDetailViewModel mTaskDetailViewModel;
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
        mAddEditTaskViewModel.processIntents(Observable.just(
                AddEditTaskIntent.InitialIntent.create(testTask.getId())
        ));

        // Then the task repository is queried and a stated is emitted back
        verify(mTasksRepository).getTask(eq(testTask.getId()));
        mTestObserver.assertValueAt(1, state ->
                state.title().equals(testTask.getTitle()) &&
                        state.description().equals(testTask.getDescription()));
    }



}