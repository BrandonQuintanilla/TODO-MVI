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

package com.example.android.architecture.blueprints.todoapp.taskdetail;

import android.arch.lifecycle.ViewModel;
import android.support.annotation.NonNull;

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviIntent;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviViewModel;

import io.reactivex.Observable;
import io.reactivex.functions.BiFunction;
import io.reactivex.subjects.PublishSubject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Listens to user actions from the UI ({@link TaskDetailFragment}), retrieves the data and updates
 * the UI as required.
 */
public class TaskDetailViewModel extends ViewModel
        implements MviViewModel<TaskDetailIntent, TaskDetailViewState> {

    @NonNull
    private PublishSubject<TaskDetailIntent> mIntentsSubject;
    @NonNull
    private PublishSubject<TaskDetailViewState> mStatesSubject;
    @NonNull
    private TaskDetailActionProcessorHolder mActionProcessorHolder;

    /**
     * Creates a presenter for the add/edit view.
     */
    public TaskDetailViewModel(@NonNull TaskDetailActionProcessorHolder actionProcessorHolder) {
        mActionProcessorHolder = checkNotNull(actionProcessorHolder);

        mIntentsSubject = PublishSubject.create();
        mStatesSubject = PublishSubject.create();

        compose().subscribe(this.mStatesSubject);
    }

    @Override
    public void processIntents(Observable<TaskDetailIntent> intents) {
        intents.subscribe(mIntentsSubject);
    }

    @Override
    public Observable<TaskDetailViewState> states() {
        return mStatesSubject;
    }

    private Observable<TaskDetailViewState> compose() {
        return mIntentsSubject.doOnNext(MviViewModel::logIntent)
                .scan(initialIntentFilter)
                .map(this::actionFromIntent)
                .doOnNext(MviViewModel::logAction)
                .compose(mActionProcessorHolder.actionProcessor)
                .doOnNext(MviViewModel::logResult)
                .scan(TaskDetailViewState.idle(), reducer)
                .doOnNext(MviViewModel::logState);
    }

    private BiFunction<TaskDetailIntent, TaskDetailIntent, TaskDetailIntent> initialIntentFilter =
            (previousIntent, newIntent) -> {
                // if isReConnection (e.g. after config change)
                // i.e. we are inside the scan, meaning there has already
                // been intent in the past, meaning the InitialIntent cannot
                // be the first => it is a reconnection.
                if (newIntent instanceof TaskDetailIntent.InitialIntent) {
                    return TaskDetailIntent.GetLastState.create();
                } else {
                    return newIntent;
                }
            };

    private TaskDetailAction actionFromIntent(MviIntent intent) {
        if (intent instanceof TaskDetailIntent.InitialIntent) {
            String taskId = ((TaskDetailIntent.InitialIntent) intent).taskId();
            if (taskId == null) {
                // nothing to do here so getting the idle state with GetLastState intent
                return TaskDetailAction.GetLastState.create();
            } else {
                return TaskDetailAction.PopulateTask.create(taskId);
            }
        }
        if (intent instanceof TaskDetailIntent.DeleteTask) {
            TaskDetailIntent.DeleteTask deleteTaskIntent = (TaskDetailIntent.DeleteTask) intent;
            final String taskId = deleteTaskIntent.taskId();
            return TaskDetailAction.DeleteTask.create(taskId);
        }

        if (intent instanceof TaskDetailIntent.CompleteTaskIntent) {
            TaskDetailIntent.CompleteTaskIntent completeTaskIntent = (TaskDetailIntent.CompleteTaskIntent) intent;
            final String taskId = completeTaskIntent.taskId();
            return TaskDetailAction.CompleteTask.create(taskId);
        }

        if (intent instanceof TaskDetailIntent.ActivateTaskIntent) {
            TaskDetailIntent.ActivateTaskIntent activateTaskIntent = (TaskDetailIntent.ActivateTaskIntent) intent;
            final String taskId = activateTaskIntent.taskId();
            return TaskDetailAction.ActivateTask.create(taskId);
        }

        if (intent instanceof TaskDetailIntent.GetLastState) {
            return TaskDetailAction.GetLastState.create();
        }
        throw new IllegalArgumentException("do not know how to treat this intent " + intent);
    }

    private static BiFunction<TaskDetailViewState, TaskDetailResult, TaskDetailViewState> reducer =
            (previousState, result) -> {
                TaskDetailViewState.Builder stateBuilder = previousState.buildWith();
                if (result instanceof TaskDetailResult.GetLastState) {
                    return stateBuilder.build();
                }
                if (result instanceof TaskDetailResult.PopulateTask) {
                    TaskDetailResult.PopulateTask populateTaskResult =
                            (TaskDetailResult.PopulateTask) result;
                    switch (populateTaskResult.status()) {
                        case SUCCESS:
                            Task task = checkNotNull(populateTaskResult.task());
                            stateBuilder.title(task.getTitle());
                            stateBuilder.description(task.getDescription());
                            stateBuilder.active(task.isActive());
                            return stateBuilder.build();
                        case FAILURE:
                            Throwable error = checkNotNull(populateTaskResult.error());
                            return stateBuilder.error(error).build();
                        case IN_FLIGHT:
                            // nothing to do
                            return stateBuilder.build();
                    }
                }
                if (result instanceof TaskDetailResult.DeleteTaskResult) {
                    TaskDetailResult.DeleteTaskResult deleteTaskResult =
                            (TaskDetailResult.DeleteTaskResult) result;
                    switch (deleteTaskResult.status()) {
                        case SUCCESS:
                            return stateBuilder.taskDeleted(true).build();
                        case FAILURE:
                            return stateBuilder.taskComplete(false).error(deleteTaskResult.error()).build();
                        case IN_FLIGHT:
                            return stateBuilder.taskComplete(true).build();
                    }
                } else if (result instanceof TaskDetailResult.ActivateTaskResult) {
                    TaskDetailResult.ActivateTaskResult activateTaskResult =
                            (TaskDetailResult.ActivateTaskResult) result;
                    switch (activateTaskResult.status()) {
                        case SUCCESS:
                            return stateBuilder
                                    .taskActivated(false)
                                    .completed(false)
                                    .build();

                        case FAILURE:
                            return stateBuilder
                                    .taskActivated(false)
                                    .error(activateTaskResult.error()).build();
                        case IN_FLIGHT:
                            return stateBuilder.taskActivated(true).build();
                    }
                } else if (result instanceof TaskDetailResult.CompleteTaskResult) {
                    TaskDetailResult.CompleteTaskResult completeTaskResult =
                            (TaskDetailResult.CompleteTaskResult) result;
                    switch (completeTaskResult.status()) {
                        case SUCCESS:
                            return stateBuilder
                                    .taskActivated(false)
                                    .completed(true)
                                    .build();

                        case FAILURE:
                            return stateBuilder
                                    .taskActivated(false)
                                    .error(completeTaskResult.error()).build();
                        case IN_FLIGHT:
                            return stateBuilder.taskActivated(true).build();
                    }
                }
                throw new IllegalStateException("Mishandled result? Should not happen―as always: " + result);
            };
}
