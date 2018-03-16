package com.example.android.architecture.blueprints.todoapp.taskdetail

import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailIntent.ActivateTaskIntent
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailIntent.CompleteTaskIntent
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailIntent.DeleteTask
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailIntent.InitialIntent
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
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import java.util.NoSuchElementException

/**
 * Unit tests for the implementation of [TaskDetailViewModel]
 */
class TaskDetailViewModelTest {

  private lateinit var taskDetailViewModel: TaskDetailViewModel
  @Mock private lateinit var tasksRepository: TasksRepository
  private lateinit var schedulerProvider: BaseSchedulerProvider
  private lateinit var testObserver: TestObserver<TaskDetailViewState>

  @Before
  @Throws(Exception::class)
  fun setUpTaskDetailViewModel() {
    // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
    // inject the mocks in the test the initMocks method needs to be called.
    MockitoAnnotations.initMocks(this)

    // Make the sure that all schedulers are immediate.
    schedulerProvider = ImmediateSchedulerProvider()

    // Get a reference to the class under test
    taskDetailViewModel = TaskDetailViewModel(
        TaskDetailActionProcessorHolder(tasksRepository, schedulerProvider))

    testObserver = taskDetailViewModel.states().test()
  }

  @Test
  fun populateTask_callsRepoAndUpdatesViewOnSuccess() {
    val testTask = Task(title = "TITLE", description = "DESCRIPTION")
    `when`(tasksRepository.getTask(testTask.id)).thenReturn(Single.just(testTask))

    // When populating a task is initiated by an initial intent
    taskDetailViewModel.processIntents(Observable.just(InitialIntent(testTask.id)))

    // Then the task repository is queried and a stated is emitted back
    verify(tasksRepository).getTask(eq(testTask.id))
    testObserver.assertValueAt(2) { (title, description) ->
      title == testTask.title && description == testTask.description
    }
  }

  @Test
  fun populateTask_callsRepoAndUpdatesViewOnError() {
    val (id) = Task(title = "TITLE", description = "DESCRIPTION")
    `when`(tasksRepository.getTask(id))
        .thenReturn(Single.error(NoSuchElementException("The MaybeSource is empty")))

    // When populating a task is initiated by an initial intent
    taskDetailViewModel.processIntents(Observable.just(InitialIntent(id)))

    // Then the task repository is queried and a stated is emitted back
    verify(tasksRepository).getTask(eq(id))
    testObserver.assertValueAt(2) { (title, description, _, _, error) ->
      error != null && title.isEmpty() && description.isEmpty()
    }
  }

  @Test
  fun deleteTask_deletesFromRepository_showsSuccessMessageUi() {
    `when`(tasksRepository.deleteTask(any())).thenReturn(Completable.complete())

    // When an existing task saving intent is emitted by the view
    taskDetailViewModel.processIntents(Observable.just(DeleteTask("1")))

    // Then a task is saved in the repository and the view updates
    verify(tasksRepository).deleteTask(any<String>())
    // saved to the model
    testObserver.assertValueAt(1, TaskDetailViewState::taskDeleted)
  }

  @Test
  fun deleteTask_showsErrorMessageUi() {
    `when`(tasksRepository.deleteTask(any()))
        .thenReturn(Completable.error(NoSuchElementException("Task does not exist")))

    // When an existing task saving intent is emitted by the view
    taskDetailViewModel.processIntents(Observable.just(DeleteTask("1")))

    // Then a task is saved in the repository and the view updates
    verify(tasksRepository).deleteTask(any())
    // saved to the model
    testObserver.assertValueAt(1) { (_, _, _, _, error) -> error != null }
  }

  @Test
  fun completeTask_marksTaskAsComplete_showsSuccessMessageUi() {
    val task = Task(
        title = "Complete Requested",
        description = "For this task")

    `when`(tasksRepository.completeTask(any<String>())).thenReturn(Completable.complete())
    `when`(tasksRepository.getTask(any())).thenReturn(Single.just(task))

    // When an existing task saving intent is emitted by the view
    taskDetailViewModel.processIntents(Observable.just(CompleteTaskIntent("1")))

    // Then a task is saved in the repository and the view updates
    verify(tasksRepository).completeTask(any<String>())
    verify(tasksRepository).getTask(any())
    testObserver.assertValueAt(1, TaskDetailViewState::taskComplete)
  }

  @Test
  fun completeTask_showsErrorMessageUi() {
    `when`(tasksRepository.completeTask(any<String>())).thenReturn(Completable.complete())
    `when`(tasksRepository.getTask(any()))
        .thenReturn(Single.error(NoSuchElementException("The MaybeSource is empty")))

    // When an existing task saving intent is emitted by the view
    taskDetailViewModel.processIntents(Observable.just(CompleteTaskIntent("1")))

    testObserver.assertValueAt(1) { (_, _, _, _, error) -> error != null }
  }

  @Test
  fun activateTask_marksTaskAsActive_showsSuccessMessageUi() {
    val task = Task(
        title = "Activate Requested",
        description = "For this task")

    `when`(tasksRepository.activateTask(any<String>())).thenReturn(Completable.complete())
    `when`(tasksRepository.getTask(any())).thenReturn(Single.just(task))

    // When an existing task saving intent is emitted by the view
    taskDetailViewModel.processIntents(Observable.just(ActivateTaskIntent("1")))

    // Then a task is saved in the repository and the view updates
    verify(tasksRepository).activateTask(any<String>())
    verify(tasksRepository).getTask(any())
    testObserver.assertValueAt(1, TaskDetailViewState::taskActivated)
  }

  @Test
  fun activateTask_showsErrorMessageUi() {
    `when`(tasksRepository.activateTask(any<String>())).thenReturn(Completable.complete())
    `when`(tasksRepository.getTask(any()))
        .thenReturn(Single.error(NoSuchElementException("The MaybeSource is empty")))

    // When an existing task saving intent is emitted by the view
    taskDetailViewModel.processIntents(Observable.just(ActivateTaskIntent("1")))

    testObserver.assertValueAt(1) { (_, _, _, _, error) -> error != null }
  }
}