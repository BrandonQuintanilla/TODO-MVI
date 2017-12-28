package com.example.android.architecture.blueprints.todoapp.addedittask

import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskAction.CreateTaskAction
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskAction.PopulateTaskAction
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskAction.UpdateTaskAction
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskResult.CreateTaskResult
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskResult.PopulateTaskResult
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskResult.UpdateTaskResult
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.mvibase.MviAction
import com.example.android.architecture.blueprints.todoapp.mvibase.MviResult
import com.example.android.architecture.blueprints.todoapp.mvibase.MviViewModel
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
class AddEditTaskActionProcessorHolder(
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

  private val createTaskProcessor =
      ObservableTransformer<CreateTaskAction, CreateTaskResult> { actions ->
        actions
            .map { action -> Task(title = action.title, description = action.description) }
            .map { task ->
              if (task.empty) {
                CreateTaskResult.Empty
              } else {
                tasksRepository.saveTask(task)
                CreateTaskResult.Success
              }
            }
      }

  private val updateTaskProcessor =
      ObservableTransformer<UpdateTaskAction, UpdateTaskResult> { actions ->
        actions.flatMap { action ->
          tasksRepository.saveTask(Task.invoke(action.title, action.description, action.taskId))
              .andThen(Observable.just(UpdateTaskResult))
        }
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
      ObservableTransformer<AddEditTaskAction, AddEditTaskResult> { actions ->
        actions.publish { shared ->
          Observable.merge<AddEditTaskResult>(
              // Match PopulateTasks to populateTaskProcessor
              shared.ofType(AddEditTaskAction.PopulateTaskAction::class.java)
                  .compose(populateTaskProcessor),
              // Match CreateTasks to createTaskProcessor
              shared.ofType(AddEditTaskAction.CreateTaskAction::class.java)
                  .compose(createTaskProcessor),
              // Match UpdateTasks to updateTaskProcessor
              shared.ofType(AddEditTaskAction.UpdateTaskAction::class.java)
                  .compose(updateTaskProcessor))
              .mergeWith(
                  // Error for not implemented actions
                  shared.filter { v ->
                    v !is AddEditTaskAction.PopulateTaskAction &&
                        v !is AddEditTaskAction.CreateTaskAction &&
                        v !is AddEditTaskAction.UpdateTaskAction
                  }
                      .flatMap { w ->
                        Observable.error<AddEditTaskResult>(
                            IllegalArgumentException("Unknown Action type: " + w))
                      })
        }
      }
}
