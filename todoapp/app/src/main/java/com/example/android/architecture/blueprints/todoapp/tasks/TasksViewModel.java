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

import android.arch.lifecycle.ViewModel;
import android.support.annotation.NonNull;
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviIntent;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviViewModel;
import com.example.android.architecture.blueprints.todoapp.util.EspressoIdlingResource;
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.BiFunction;
import io.reactivex.subjects.PublishSubject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Listens to user actions from the UI ({@link TasksFragment}), retrieves the data and updates the
 * UI as required.
 */
public class TasksViewModel extends ViewModel implements MviViewModel<TasksIntent> {
  @NonNull private PublishSubject<MviIntent> intentsSubject;
  @NonNull private PublishSubject<TasksViewState> statesSubject;
  @NonNull private TasksRepository tasksRepository;
  @NonNull private BaseSchedulerProvider schedulerProvider;
  @NonNull private TasksFilterType currentFiltering = TasksFilterType.ALL_TASKS;
  private boolean mFirstLoad = true;

  public TasksViewModel(@NonNull TasksRepository tasksRepository,
      @NonNull BaseSchedulerProvider schedulerProvider) {
    this.tasksRepository = checkNotNull(tasksRepository, "tasksRepository cannot be null");
    this.schedulerProvider = checkNotNull(schedulerProvider, "schedulerProvider cannot be null");

    intentsSubject = PublishSubject.create();
    statesSubject = PublishSubject.create();

    compose().subscribe(this.statesSubject);
  }

  @Override public void forwardIntents(Observable<TasksIntent> intents) {
    intents.subscribe(intentsSubject);
  }

  @Override public Observable<TasksViewState> states() {
    return statesSubject;
  }

  private Observable<TasksViewState> compose() {
    return intentsSubject.doOnNext(MviViewModel::logIntent)
        .scan(initialIntentFilter)
        .map(this::actionFromIntent)
        .doOnNext(MviViewModel::logAction)
        .compose(actionProcessor)
        .doOnNext(MviViewModel::logResult)
        .scan(TasksViewState.idle(), reducer)
        .doOnNext(MviViewModel::logState)
        .doOnNext(state -> {
          // The network request might be handled in a different thread so make sure Espresso knows
          // that the app is busy until the response is handled.
          if (state.isLoading()) {
            EspressoIdlingResource.increment();
          } else if (!EspressoIdlingResource.getIdlingResource().isIdleNow()) {
            EspressoIdlingResource.decrement(); // Set app as idle.
          }
        });
  }

  private BiFunction<MviIntent, MviIntent, MviIntent> initialIntentFilter =
      (previousIntent, newIntent) -> {
        // if isReConnection (e.g. after config change)
        // i.e. we are inside the scan, meaning there has already
        // been intent in the past, meaning the InitialIntent cannot
        // be the first => it is a reconnection.
        if (newIntent instanceof TasksIntent.InitialIntent) {
          return TasksIntent.GetLastState.create();
        } else {
          return newIntent;
        }
      };

  private TasksAction actionFromIntent(MviIntent intent) {
    if (intent instanceof TasksIntent.InitialIntent) {
      // TODO(benoit) what's with this forceupdate?
      //// Simplification for sample: a network reload will be forced on first load.
      //loadTasks(forceUpdate || mFirstLoad, true);
      //mFirstLoad = false;
      return TasksAction.LoadTasks.create(true, TasksFilterType.ALL_TASKS);
    }
    if (intent instanceof TasksIntent.RefreshIntent) {
      return TasksAction.LoadTasks.create(false, TasksFilterType.ALL_TASKS);
    }
    if (intent instanceof TasksIntent.GetLastState) {
      return TasksAction.GetLastState.create();
    }
    if (intent instanceof TasksIntent.ActivateTaskIntent) {
      return TasksAction.ActivateTaskAction.create(
          ((TasksIntent.ActivateTaskIntent) intent).task());
    }
    if (intent instanceof TasksIntent.CompleteTaskIntent) {
      return TasksAction.CompleteTaskAction.create(
          ((TasksIntent.CompleteTaskIntent) intent).task());
    }
    if (intent instanceof TasksIntent.ClearCompletedTasksIntent) {
      return TasksAction.ClearCompletedTasksAction.create();
    }
    throw new IllegalArgumentException("do not know how to treat this intent " + intent);
  }

  private ObservableTransformer<TasksAction.LoadTasks, TasksResult.LoadTasks> loadTasksProcessor =
      actions -> actions.flatMap(action -> tasksRepository.getTasks(action.forceUpdate())
          .toObservable()
          .flatMap(Observable::fromIterable)
          // TODO(benoit) Next 3 calls are ugly. Why not letting the repo do it?
          .filter(task -> {
            switch (action.filterType()) {
              case ACTIVE_TASKS:
                return task.isActive();
              case COMPLETED_TASKS:
                return task.isCompleted();
              case ALL_TASKS:
              default:
                return true;
            }
          })
          .toList()
          .toObservable()
          .map(TasksResult.LoadTasks::success)
          .onErrorReturn(TasksResult.LoadTasks::failure)
          .subscribeOn(schedulerProvider.io())
          .observeOn(schedulerProvider.ui())
          .startWith(TasksResult.LoadTasks.inFlight()));

  private ObservableTransformer<TasksAction.GetLastState, TasksResult.GetLastState>
      getLastStateProcessor = actions -> actions.map(ignored -> TasksResult.GetLastState.create());

  private ObservableTransformer<TasksAction.ActivateTaskAction, TasksResult.ActivateTaskResult>
      activateTaskProcessor = actions -> actions.flatMap(
      action -> tasksRepository.activateTask(action.task())
          .andThen(tasksRepository.getTasks())
          .toObservable()
          .flatMap(Observable::fromIterable)
          // TODO(benoit) Pass the state with the call ? Do it in the repo ? Do it in the reducer?
          //.filter(task -> {
          //  switch (action.filterType()) {
          //    case ACTIVE_TASKS:
          //      return task.isActive();
          //    case COMPLETED_TASKS:
          //      return task.isCompleted();
          //    case ALL_TASKS:
          //    default:
          //      return true;
          //  }
          //})
          .toList()
          .toObservable()
          .map(TasksResult.ActivateTaskResult::success)
          .onErrorReturn(TasksResult.ActivateTaskResult::failure)
          .subscribeOn(schedulerProvider.io())
          .observeOn(schedulerProvider.ui())
          .startWith(TasksResult.ActivateTaskResult.inFlight()));

  private ObservableTransformer<TasksAction.CompleteTaskAction, TasksResult.CompleteTaskResult>
      completeTaskProcessor = actions -> actions.flatMap(
      action -> tasksRepository.completeTask(action.task())
          .andThen(tasksRepository.getTasks())
          .toObservable()
          .flatMap(Observable::fromIterable)
          // TODO(benoit) Pass the state with the call ? Do it in the repo ? Do it in the reducer?
          //.filter(task -> {
          //  switch (action.filterType()) {
          //    case ACTIVE_TASKS:
          //      return task.isActive();
          //    case COMPLETED_TASKS:
          //      return task.isCompleted();
          //    case ALL_TASKS:
          //    default:
          //      return true;
          //  }
          //})
          .toList()
          .toObservable()
          .map(TasksResult.CompleteTaskResult::success)
          .onErrorReturn(TasksResult.CompleteTaskResult::failure)
          .subscribeOn(schedulerProvider.io())
          .observeOn(schedulerProvider.ui())
          .startWith(TasksResult.CompleteTaskResult.inFlight()));

  private ObservableTransformer<TasksAction.ClearCompletedTasksAction, TasksResult.ClearCompletedTasksResult>
      clearCompletedTasksProcessor = actions -> actions.flatMap(
      action -> tasksRepository.clearCompletedTasks()
          .andThen(tasksRepository.getTasks())
          .toObservable()
          .flatMap(Observable::fromIterable)
          // TODO(benoit) Pass the state with the call ? Do it in the repo ? Do it in the reducer?
          //.filter(task -> {
          //  switch (action.filterType()) {
          //    case ACTIVE_TASKS:
          //      return task.isActive();
          //    case COMPLETED_TASKS:
          //      return task.isCompleted();
          //    case ALL_TASKS:
          //    default:
          //      return true;
          //  }
          //})
          .toList()
          .toObservable()
          .map(TasksResult.ClearCompletedTasksResult::success)
          .onErrorReturn(TasksResult.ClearCompletedTasksResult::failure)
          .subscribeOn(schedulerProvider.io())
          .observeOn(schedulerProvider.ui())
          .startWith(TasksResult.ClearCompletedTasksResult.inFlight()));

  private ObservableTransformer<TasksAction, TasksResult> actionProcessor =
      actions -> actions.publish(shared -> Observable.merge(
          shared.ofType(TasksAction.LoadTasks.class).compose(loadTasksProcessor),
          shared.ofType(TasksAction.GetLastState.class).compose(getLastStateProcessor),
          shared.ofType(TasksAction.ActivateTaskAction.class).compose(activateTaskProcessor),
          shared.ofType(TasksAction.CompleteTaskAction.class).compose(completeTaskProcessor))
          .mergeWith(shared.ofType(TasksAction.ClearCompletedTasksAction.class)
              .compose(clearCompletedTasksProcessor))
          .mergeWith(
              // Error for not implemented actions
              shared.filter(v -> !(v instanceof TasksAction.LoadTasks)
                  && !(v instanceof TasksAction.GetLastState)
                  && !(v instanceof TasksAction.ActivateTaskAction)
                  && !(v instanceof TasksAction.CompleteTaskAction)
                  && !(v instanceof TasksAction.ClearCompletedTasksAction))
                  .flatMap(w -> Observable.error(
                      new IllegalArgumentException("Unknown Action type: " + w)))));

  private static BiFunction<TasksViewState, TasksResult, TasksViewState> reducer =
      (previousState, result) -> {
        TasksViewState.Builder stateBuilder = previousState.buildWith();
        if (result instanceof TasksResult.LoadTasks) {
          TasksResult.LoadTasks loadResult = (TasksResult.LoadTasks) result;
          switch (loadResult.status()) {
            case SUCCESS:
              return stateBuilder.isLoading(false).tasks(loadResult.tasks()).build();
            case FAILURE:
              return stateBuilder.isLoading(false).error(loadResult.error()).build();
            case IN_FLIGHT:
              return stateBuilder.isLoading(true).build();
          }
        } else if (result instanceof TasksResult.GetLastState) {
          return stateBuilder.build();
        } else if (result instanceof TasksResult.CompleteTaskResult) {
          TasksResult.CompleteTaskResult completeTaskResult =
              (TasksResult.CompleteTaskResult) result;
          switch (completeTaskResult.status()) {
            case SUCCESS:
              return stateBuilder.taskComplete(false).tasks(completeTaskResult.tasks()).build();
            case FAILURE:
              return stateBuilder.taskComplete(false).error(completeTaskResult.error()).build();
            case IN_FLIGHT:
              return stateBuilder.taskComplete(true).build();
          }
        } else if (result instanceof TasksResult.ActivateTaskResult) {
          TasksResult.ActivateTaskResult activateTaskResult =
              (TasksResult.ActivateTaskResult) result;
          switch (activateTaskResult.status()) {
            case SUCCESS:
              return stateBuilder.taskActivated(false).tasks(activateTaskResult.tasks()).build();
            case FAILURE:
              return stateBuilder.taskActivated(false).error(activateTaskResult.error()).build();
            case IN_FLIGHT:
              return stateBuilder.taskActivated(true).build();
          }
        } else if (result instanceof TasksResult.ClearCompletedTasksResult) {
          TasksResult.ClearCompletedTasksResult clearCompletedTasks =
              (TasksResult.ClearCompletedTasksResult) result;
          switch (clearCompletedTasks.status()) {
            case SUCCESS:
              return stateBuilder.completedTasksCleared(false)
                  .tasks(clearCompletedTasks.tasks())
                  .build();
            case FAILURE:
              return stateBuilder.completedTasksCleared(false)
                  .error(clearCompletedTasks.error())
                  .build();
            case IN_FLIGHT:
              return stateBuilder.completedTasksCleared(true).build();
          }
        } else {
          throw new IllegalArgumentException("Don't know this result " + result);
        }
        throw new IllegalStateException("Mishandled result? Should not happen (as always)");
      };

  /**
   * Sets the current task filtering type.
   *
   * @param requestType Can be {@link TasksFilterType#ALL_TASKS},
   * {@link TasksFilterType#COMPLETED_TASKS}, or
   * {@link TasksFilterType#ACTIVE_TASKS}
   */
  public void setFiltering(@NonNull TasksFilterType requestType) {
    currentFiltering = requestType;
  }

  public TasksFilterType getFiltering() {
    return currentFiltering;
  }
}
