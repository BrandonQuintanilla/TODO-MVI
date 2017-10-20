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

import android.arch.lifecycle.ViewModel;
import android.support.annotation.NonNull;

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviIntent;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviViewModel;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.BiFunction;
import io.reactivex.subjects.PublishSubject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Listens to user actions from the UI ({@link AddEditTaskFragment}), retrieves the data and updates
 * the UI as required.
 */
public class AddEditTaskViewModel extends ViewModel
        implements MviViewModel<AddEditTaskIntent, AddEditTaskViewState> {

    @NonNull
    private PublishSubject<AddEditTaskIntent> mIntentsSubject;
    @NonNull
    private Observable<AddEditTaskViewState> mStatesObservable;
    @NonNull
    private AddEditTaskActionProcessorHolder mActionProcessorHolder;

    public AddEditTaskViewModel(@NonNull AddEditTaskActionProcessorHolder actionProcessorHolder) {
        mActionProcessorHolder = checkNotNull(actionProcessorHolder);

        mIntentsSubject = PublishSubject.create();
        mStatesObservable = compose().replay(1).autoConnect(0);
    }

    @Override
    public void processIntents(Observable<AddEditTaskIntent> intents) {
        intents.subscribe(mIntentsSubject);
    }

    @Override
    public Observable<AddEditTaskViewState> states() {
        return mStatesObservable;
    }

    private Observable<AddEditTaskViewState> compose() {
        return mIntentsSubject
                .compose(intentFilter)
                .map(this::actionFromIntent)
                .filter(action -> !(action instanceof AddEditTaskAction.SkipMe))
                .compose(mActionProcessorHolder.actionProcessor)
                .scan(AddEditTaskViewState.idle(), reducer);
    }

    /**
     * take only the first ever InitialIntent and all intents of other types
     * to avoid reloading data on config changes
     */
    private ObservableTransformer<AddEditTaskIntent, AddEditTaskIntent> intentFilter =
            intents -> intents.publish(shared ->
                    Observable.merge(
                            shared.ofType(AddEditTaskIntent.InitialIntent.class).take(1),
                            shared.filter(intent -> !(intent instanceof AddEditTaskIntent.InitialIntent))
                    )
            );

    private AddEditTaskAction actionFromIntent(MviIntent intent) {
        if (intent instanceof AddEditTaskIntent.InitialIntent) {
            String taskId = ((AddEditTaskIntent.InitialIntent) intent).taskId();
            if (taskId == null) {
                // new Task, so nothing to do
                return AddEditTaskAction.SkipMe.create();
            } else {
                return AddEditTaskAction.PopulateTask.create(taskId);
            }
        }
        if (intent instanceof AddEditTaskIntent.SaveTask) {
            AddEditTaskIntent.SaveTask saveTaskIntent = (AddEditTaskIntent.SaveTask) intent;
            final String taskId = saveTaskIntent.taskId();
            if (taskId == null) {
                return AddEditTaskAction.CreateTask.create(
                        saveTaskIntent.title(), saveTaskIntent.description());
            } else {
                return AddEditTaskAction.UpdateTask.create(
                        taskId, saveTaskIntent.title(), saveTaskIntent.description());
            }
        }
        throw new IllegalArgumentException("do not know how to treat this intent " + intent);
    }

    private static BiFunction<AddEditTaskViewState, AddEditTaskResult, AddEditTaskViewState> reducer =
            (previousState, result) -> {
                AddEditTaskViewState.Builder stateBuilder = previousState.buildWith();
                if (result instanceof AddEditTaskResult.PopulateTask) {
                    AddEditTaskResult.PopulateTask populateTaskResult =
                            (AddEditTaskResult.PopulateTask) result;
                    switch (populateTaskResult.status()) {
                        case SUCCESS:
                            Task task = checkNotNull(populateTaskResult.task());
                            if (task.isActive()) {
                                stateBuilder.title(task.getTitle());
                                stateBuilder.description(task.getDescription());
                            }
                            return stateBuilder.build();
                        case FAILURE:
                            Throwable error = checkNotNull(populateTaskResult.error());
                            return stateBuilder.error(error).build();
                        case IN_FLIGHT:
                            // nothing to do
                            return stateBuilder.build();
                    }
                }
                if (result instanceof AddEditTaskResult.CreateTask) {
                    AddEditTaskResult.CreateTask createTaskResult =
                            (AddEditTaskResult.CreateTask) result;
                    if (createTaskResult.isEmpty()) {
                        return stateBuilder.isEmpty(true).build();
                    } else {
                        return stateBuilder.isEmpty(false).isSaved(true).build();
                    }
                }
                if (result instanceof AddEditTaskResult.UpdateTask) {
                    return stateBuilder.isSaved(true).build();
                }
                throw new IllegalStateException("Mishandled result? Should not happen―as always: " + result);
            };
}
