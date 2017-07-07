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

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviIntent;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviViewModel;
import com.example.android.architecture.blueprints.todoapp.util.EspressoIdlingResource;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.functions.BiFunction;
import io.reactivex.subjects.PublishSubject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Listens to user actions from the UI ({@link TasksFragment}), retrieves the data and updates the
 * UI as required.
 */
public class TasksViewModel extends ViewModel implements MviViewModel<TasksIntent, TasksViewState> {
    @NonNull
    private PublishSubject<MviIntent> mIntentsSubject;
    @NonNull
    private PublishSubject<TasksViewState> mStatesSubject;
    @NonNull
    private TasksActionProcessorHolder mActionProcessorHolder;

    public TasksViewModel(@NonNull TasksActionProcessorHolder taskActionProcessorHolder) {
        this.mActionProcessorHolder = checkNotNull(taskActionProcessorHolder, "taskActionProcessorHolder cannot be null");

        mIntentsSubject = PublishSubject.create();
        mStatesSubject = PublishSubject.create();

        compose().subscribe(this.mStatesSubject);
    }

    @Override
    public void processIntents(Observable<TasksIntent> intents) {
        intents.subscribe(mIntentsSubject);
    }

    @Override
    public Observable<TasksViewState> states() {
        return mStatesSubject;
    }

    private Observable<TasksViewState> compose() {
        return mIntentsSubject.doOnNext(MviViewModel::logIntent)
                .scan(initialIntentFilter)
                .map(this::actionFromIntent)
                .doOnNext(MviViewModel::logAction)
                .compose(mActionProcessorHolder.actionProcessor)
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
            return TasksAction.LoadTasks.loadAndFilter(true, TasksFilterType.ALL_TASKS);
        }
        if (intent instanceof TasksIntent.ChangeFilterIntent) {
            return TasksAction.LoadTasks.loadAndFilter(false,
                    ((TasksIntent.ChangeFilterIntent) intent).filterType());
        }
        if (intent instanceof TasksIntent.RefreshIntent) {
            return TasksAction.LoadTasks.load(((TasksIntent.RefreshIntent) intent).forceUpdate());
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

    private static BiFunction<TasksViewState, TasksResult, TasksViewState> reducer =
            (previousState, result) -> {
                TasksViewState.Builder stateBuilder = previousState.buildWith();
                if (result instanceof TasksResult.LoadTasks) {
                    TasksResult.LoadTasks loadResult = (TasksResult.LoadTasks) result;
                    switch (loadResult.status()) {
                        case SUCCESS:
                            TasksFilterType filterType = loadResult.filterType();
                            if (filterType == null) {
                                filterType = previousState.tasksFilterType();
                            }
                            List<Task> tasks = filteredTasks(checkNotNull(loadResult.tasks()), filterType);
                            return stateBuilder.isLoading(false).tasks(tasks).tasksFilterType(filterType).build();
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
                            List<Task> tasks = filteredTasks(checkNotNull(completeTaskResult.tasks()),
                                    previousState.tasksFilterType());
                            return stateBuilder.taskComplete(false).tasks(tasks).build();
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
                            List<Task> tasks = filteredTasks(checkNotNull(activateTaskResult.tasks()),
                                    previousState.tasksFilterType());
                            return stateBuilder.taskActivated(false).tasks(tasks).build();
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
                            List<Task> tasks = filteredTasks(checkNotNull(clearCompletedTasks.tasks()),
                                    previousState.tasksFilterType());
                            return stateBuilder.completedTasksCleared(false).tasks(tasks).build();
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

    private static List<Task> filteredTasks(@NonNull List<Task> tasks,
                                            @NonNull TasksFilterType filterType) {
        List<Task> filteredTasks = new ArrayList<>(tasks.size());
        switch (filterType) {
            case ALL_TASKS:
                filteredTasks.addAll(tasks);
                break;
            case ACTIVE_TASKS:
                for (Task task : tasks) {
                    if (task.isActive()) filteredTasks.add(task);
                }
                break;
            case COMPLETED_TASKS:
                for (Task task : tasks) {
                    if (task.isCompleted()) filteredTasks.add(task);
                }
                break;
        }
        return filteredTasks;
    }
}
