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

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.BiFunction;
import io.reactivex.subjects.PublishSubject;

import static com.example.android.architecture.blueprints.todoapp.util.UiNotificationStatus.SHOW;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Listens to user actions from the UI ({@link TasksFragment}), retrieves the data and updates the
 * UI as required.
 */
public class TasksViewModel extends ViewModel implements MviViewModel<TasksIntent, TasksViewState> {
    @NonNull
    private PublishSubject<TasksIntent> mIntentsSubject;
    @NonNull
    private Observable<TasksViewState> mStatesObservable;
    @NonNull
    private TasksActionProcessorHolder mActionProcessorHolder;

    public TasksViewModel(@NonNull TasksActionProcessorHolder taskActionProcessorHolder) {
        this.mActionProcessorHolder = checkNotNull(taskActionProcessorHolder, "taskActionProcessorHolder cannot be null");

        mIntentsSubject = PublishSubject.create();
        mStatesObservable = compose().replay(1).autoConnect(0);
    }

    @Override
    public void processIntents(Observable<TasksIntent> intents) {
        intents.subscribe(mIntentsSubject);
    }

    @Override
    public Observable<TasksViewState> states() {
        return mStatesObservable;
    }

    private Observable<TasksViewState> compose() {
        return mIntentsSubject
                .compose(intentFilter)
                .map(this::actionFromIntent)
                .compose(mActionProcessorHolder.actionProcessor)
                .scan(TasksViewState.idle(), reducer);
    }

    /**
     * take only the first ever InitialIntent and all intents of other types
     * to avoid reloading data on config changes
     */
    private ObservableTransformer<TasksIntent, TasksIntent> intentFilter =
            intents -> intents.publish(shared ->
                    Observable.merge(
                            shared.ofType(TasksIntent.InitialIntent.class).take(1),
                            shared.filter(intent -> !(intent instanceof TasksIntent.InitialIntent))
                    )
            );

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
                } else if (result instanceof TasksResult.CompleteTaskResult) {
                    TasksResult.CompleteTaskResult completeTaskResult =
                            (TasksResult.CompleteTaskResult) result;
                    switch (completeTaskResult.status()) {
                        case SUCCESS:
                            stateBuilder.taskComplete(completeTaskResult.uiNotificationStatus() == SHOW);
                            if (completeTaskResult.tasks() != null) {
                                List<Task> tasks =
                                        filteredTasks(checkNotNull(completeTaskResult.tasks()),
                                                previousState.tasksFilterType());
                                stateBuilder.tasks(tasks);
                            }
                            return stateBuilder.build();
                        case FAILURE:
                            return stateBuilder.error(completeTaskResult.error()).build();
                        case IN_FLIGHT:
                            return stateBuilder.build();
                    }
                } else if (result instanceof TasksResult.ActivateTaskResult) {
                    TasksResult.ActivateTaskResult activateTaskResult =
                            (TasksResult.ActivateTaskResult) result;
                    switch (activateTaskResult.status()) {
                        case SUCCESS:
                            stateBuilder.taskActivated(activateTaskResult.uiNotificationStatus() == SHOW);
                            if (activateTaskResult.tasks() != null) {
                                List<Task> tasks =
                                        filteredTasks(checkNotNull(activateTaskResult.tasks()),
                                                previousState.tasksFilterType());
                                stateBuilder.tasks(tasks);
                            }
                            return stateBuilder.build();
                        case FAILURE:
                            return stateBuilder.error(activateTaskResult.error()).build();
                        case IN_FLIGHT:
                            return stateBuilder.build();
                    }
                } else if (result instanceof TasksResult.ClearCompletedTasksResult) {
                    TasksResult.ClearCompletedTasksResult clearCompletedTasks =
                            (TasksResult.ClearCompletedTasksResult) result;
                    switch (clearCompletedTasks.status()) {
                        case SUCCESS:
                            stateBuilder.completedTasksCleared(clearCompletedTasks.uiNotificationStatus() == SHOW);
                            if (clearCompletedTasks.tasks() != null) {
                                List<Task> tasks =
                                        filteredTasks(checkNotNull(clearCompletedTasks.tasks()),
                                                previousState.tasksFilterType());
                                stateBuilder.tasks(tasks);
                            }
                            return stateBuilder.build();
                        case FAILURE:
                            return stateBuilder.error(clearCompletedTasks.error()).build();
                        case IN_FLIGHT:
                            return stateBuilder.build();
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
