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

package com.example.android.architecture.blueprints.todoapp.statistics;

import android.arch.lifecycle.ViewModel;
import android.support.annotation.NonNull;

import com.example.android.architecture.blueprints.todoapp.mvibase.MviIntent;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviViewModel;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.BiFunction;
import io.reactivex.subjects.PublishSubject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Listens to user actions from the UI ({@link StatisticsFragment}), retrieves the data and updates
 * the UI as required.
 */
public class StatisticsViewModel extends ViewModel
        implements MviViewModel<StatisticsIntent, StatisticsViewState> {
    @NonNull
    private PublishSubject<StatisticsIntent> mIntentsSubject;
    @NonNull
    private Observable<StatisticsViewState> mStatesObservable;
    @NonNull
    private StatisticsActionProcessorHolder mActionProcessorHolder;

    public StatisticsViewModel(@NonNull StatisticsActionProcessorHolder actionProcessorHolder) {
        this.mActionProcessorHolder = checkNotNull(actionProcessorHolder, "actionProcessorHolder cannot be null");

        mIntentsSubject = PublishSubject.create();
        mStatesObservable = compose().skip(1).replay(1).autoConnect(0);
    }

    @Override
    public void processIntents(Observable<StatisticsIntent> intents) {
        intents.subscribe(mIntentsSubject);
    }

    @Override
    public Observable<StatisticsViewState> states() {
        return mStatesObservable;
    }

    private Observable<StatisticsViewState> compose() {
        return mIntentsSubject
                .compose(intentFilter)
                .map(this::actionFromIntent)
                .compose(mActionProcessorHolder.actionProcessor)
                .scan(StatisticsViewState.idle(), reducer);
    }

    /**
     * take only the first ever InitialIntent and all intents of other types
     * to avoid reloading data on config changes
     */
    private ObservableTransformer<StatisticsIntent, StatisticsIntent> intentFilter =
            intents -> intents.publish(shared ->
                    Observable.merge(
                            shared.ofType(StatisticsIntent.InitialIntent.class).take(1),
                            shared.filter(intent -> !(intent instanceof StatisticsIntent.InitialIntent))
                    )
            );

    private StatisticsAction actionFromIntent(MviIntent intent) {
        if (intent instanceof StatisticsIntent.InitialIntent) {
            return StatisticsAction.LoadStatistics.create();
        }
        throw new IllegalArgumentException("do not know how to treat this intent " + intent);
    }

    private static BiFunction<StatisticsViewState, StatisticsResult, StatisticsViewState> reducer =
            (previousState, result) -> {
                StatisticsViewState.Builder stateBuilder = previousState.buildWith();
                if (result instanceof StatisticsResult.LoadStatistics) {
                    StatisticsResult.LoadStatistics loadResult = (StatisticsResult.LoadStatistics) result;
                    switch (loadResult.status()) {
                        case SUCCESS:
                            return stateBuilder.isLoading(false)
                                    .activeCount(loadResult.activeCount())
                                    .completedCount(loadResult.completedCount())
                                    .build();
                        case FAILURE:
                            return stateBuilder.isLoading(false).error(loadResult.error()).build();
                        case IN_FLIGHT:
                            return stateBuilder.isLoading(true).build();
                    }
                } else {
                    throw new IllegalArgumentException("Don't know this result " + result);
                }
                throw new IllegalStateException("Mishandled result? Should not happen (as always)");
            };
}
