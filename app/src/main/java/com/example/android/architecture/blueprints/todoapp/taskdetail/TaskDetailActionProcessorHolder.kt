package com.example.android.architecture.blueprints.todoapp.taskdetail

import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.mvibase.MviAction
import com.example.android.architecture.blueprints.todoapp.mvibase.MviResult
import com.example.android.architecture.blueprints.todoapp.mvibase.MviViewModel
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailAction.ActivateTaskAction
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailAction.CompleteTaskAction
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailAction.DeleteTaskAction
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailAction.PopulateTaskAction
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailResult.ActivateTaskResult
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailResult.CompleteTaskResult
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailResult.DeleteTaskResult
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailResult.PopulateTaskResult
import com.example.android.architecture.blueprints.todoapp.util.pairWithDelay
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import io.reactivex.Observable
import io.reactivex.ObservableTransformer

/**
 * Contains and executes the business logic for all emitted [MviAction]
 * and returns one unique [Observable] of [MviResult].
 *
 *
 * This could have been included inside the [MviViewModel]
 * but was separated to ease maintenance, as the [MviViewModel] was getting too big.
 */
class TaskDetailActionProcessorHolder(
    private val tasksRepository: TasksRepository,
    private val schedulerProvider: BaseSchedulerProvider
) {
  private val populateTaskProcessor =
      ObservableTransformer<PopulateTaskAction, PopulateTaskResult> { actions ->
        actions.flatMap { action ->
          tasksRepository.getTask(action.taskId)
              // Transform the Single to an Observable to allow emission of multiple
              // events down the stream (e.g. the InFlight event)
              .toObservable()
              // Wrap returned data into an immutable object
              .map(PopulateTaskResult::Success)
              .cast(PopulateTaskResult::class.java)
              // Wrap any error into an immutable object and pass it down the stream
              // without crashing.
              // Because errors are data and hence, should just be part of the stream.
              .onErrorReturn(PopulateTaskResult::Failure)
              .subscribeOn(schedulerProvider.io())
              .observeOn(schedulerProvider.ui())
              // Emit an InFlight event to notify the subscribers (e.g. the UI) we are
              // doing work and waiting on a response.
              // We emit it after observing on the UI thread to allow the event to be emitted
              // on the current frame and avoid jank.
              .startWith(PopulateTaskResult.InFlight)
        }
      }

  private val completeTaskProcessor =
      ObservableTransformer<CompleteTaskAction, CompleteTaskResult> { actions ->
        actions.flatMap(
            { action ->
              tasksRepository.completeTask(action.taskId)
                  .andThen(tasksRepository.getTask(action.taskId))
                  // Transform the Single to an Observable to allow emission of multiple
                  // events down the stream (e.g. the InFlight event)
                  .toObservable()
                  .flatMap({ task ->
                    // Emit two events to allow the UI notification to be hidden after some delay
                    pairWithDelay(
                        CompleteTaskResult.Success(task),
                        CompleteTaskResult.HideUiNotification)
                  })
                  // Wrap any error into an immutable object and pass it down the stream
                  // without crashing.
                  // Because errors are data and hence, should just be part of the stream.
                  .onErrorReturn(CompleteTaskResult::Failure)
                  .subscribeOn(schedulerProvider.io())
                  .observeOn(schedulerProvider.ui())
                  // Emit an InFlight event to notify the subscribers (e.g. the UI) we are
                  // doing work and waiting on a response.
                  // We emit it after observing on the UI thread to allow the event to be emitted
                  // on the current frame and avoid jank.
                  .startWith(CompleteTaskResult.InFlight)
            })
      }

  private val activateTaskProcessor =
      ObservableTransformer<ActivateTaskAction, ActivateTaskResult> { actions ->
        actions.flatMap(
            { action ->
              tasksRepository.activateTask(action.taskId)
                  .andThen(tasksRepository.getTask(action.taskId))
                  // Transform the Single to an Observable to allow emission of multiple
                  // events down the stream (e.g. the InFlight event)
                  .toObservable()
                  .flatMap({ task ->
                    // Emit two events to allow the UI notification to be hidden after
                    // some delay
                    pairWithDelay(
                        ActivateTaskResult.Success(task),
                        ActivateTaskResult.HideUiNotification)
                  })
                  // Wrap any error into an immutable object and pass it down the stream
                  // without crashing.
                  // Because errors are data and hence, should just be part of the stream.
                  .onErrorReturn(ActivateTaskResult::Failure)
                  .subscribeOn(schedulerProvider.io())
                  .observeOn(schedulerProvider.ui())
                  // Emit an InFlight event to notify the subscribers (e.g. the UI) we are
                  // doing work and waiting on a response.
                  // We emit it after observing on the UI thread to allow the event to be emitted
                  // on the current frame and avoid jank.
                  .startWith(ActivateTaskResult.InFlight)
            })
      }

  private val deleteTaskProcessor =
      ObservableTransformer<DeleteTaskAction, DeleteTaskResult> { actions ->
        actions.flatMap(
            { action ->
              tasksRepository.deleteTask(action.taskId)
                  .andThen(Observable.just(DeleteTaskResult.Success))
                  .cast(DeleteTaskResult::class.java)
                  // Wrap any error into an immutable object and pass it down the stream
                  // without crashing.
                  // Because errors are data and hence, should just be part of the stream.
                  .onErrorReturn(DeleteTaskResult::Failure)
                  .subscribeOn(schedulerProvider.io())
                  .observeOn(schedulerProvider.ui())
                  // Emit an InFlight event to notify the subscribers (e.g. the UI) we are
                  // doing work and waiting on a response.
                  // We emit it after observing on the UI thread to allow the event to be emitted
                  // on the current frame and avoid jank.
                  .startWith(DeleteTaskResult.InFlight)
            })
      }

  /**
   * Splits the [<] to match each type of [MviAction] to
   * its corresponding business logic processor. Each processor takes a defined [MviAction],
   * returns a defined [MviResult]
   * The global actionProcessor then merges all [<] back to
   * one unique [<].
   *
   *
   * The splitting is done using [Observable.publish] which allows almost anything
   * on the passed [Observable] as long as one and only one [Observable] is returned.
   *
   *
   * An security layer is also added for unhandled [MviAction] to allow early crash
   * at runtime to easy the maintenance.
   */
  internal var actionProcessor =
      ObservableTransformer<TaskDetailAction, TaskDetailResult> { actions ->
        actions.publish({ shared ->
          Observable.merge<TaskDetailResult>(
              // Match PopulateTasks to populateTaskProcessor
              shared.ofType(PopulateTaskAction::class.java).compose(populateTaskProcessor),
              // Match CompleteTaskAction to completeTaskProcessor
              shared.ofType(CompleteTaskAction::class.java).compose(completeTaskProcessor),
              // Match ActivateTaskAction to activateTaskProcessor
              shared.ofType(ActivateTaskAction::class.java).compose(activateTaskProcessor),
              // Match DeleteTaskAction to deleteTaskProcessor
              shared.ofType(DeleteTaskAction::class.java).compose(deleteTaskProcessor))
              .mergeWith(
                  // Error for not implemented actions
                  shared.filter({ v ->
                    (v !is PopulateTaskAction
                        && v !is CompleteTaskAction
                        && v !is ActivateTaskAction
                        && v !is DeleteTaskAction)
                  })
                      .flatMap({ w ->
                        Observable.error<TaskDetailResult>(
                            IllegalArgumentException("Unknown Action type: " + w))
                      }))
        })
      }
}
