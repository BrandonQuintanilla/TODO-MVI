package com.example.android.architecture.blueprints.todoapp.statistics;

import android.support.annotation.NonNull;

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository;
import com.example.android.architecture.blueprints.todoapp.util.Pair;
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;

public class StatisticsActionProcessorHolder {
    @NonNull
    private TasksRepository mTasksRepository;
    @NonNull
    private BaseSchedulerProvider mSchedulerProvider;

    public StatisticsActionProcessorHolder(@NonNull TasksRepository tasksRepository,
                                           @NonNull BaseSchedulerProvider schedulerProvider) {
        this.mTasksRepository = checkNotNull(tasksRepository, "tasksRepository cannot be null");
        this.mSchedulerProvider = checkNotNull(schedulerProvider, "schedulerProvider cannot be null");

    }

    private ObservableTransformer<StatisticsAction.LoadStatistics, StatisticsResult.LoadStatistics>
            loadStatisticsProcessor = actions -> actions.flatMap(action -> mTasksRepository.getTasks()
            .toObservable()
            .flatMap(Observable::fromIterable)
            .publish(shared -> //
                    Single.zip( //
                            shared.filter(Task::isActive).count(), //
                            shared.filter(Task::isCompleted).count(), //
                            Pair::create).toObservable())
            .map(pair -> StatisticsResult.LoadStatistics.success(pair.first().intValue(),
                    pair.second().intValue()))
            .onErrorReturn(StatisticsResult.LoadStatistics::failure)
            .subscribeOn(mSchedulerProvider.io())
            .observeOn(mSchedulerProvider.ui())
            .startWith(StatisticsResult.LoadStatistics.inFlight()));

    private ObservableTransformer<StatisticsAction.GetLastState, StatisticsResult.GetLastState>
            getLastStateProcessor =
            actions -> actions.map(ignored -> StatisticsResult.GetLastState.create());

    ObservableTransformer<StatisticsAction, StatisticsResult> actionProcessor =
            actions -> actions.publish(shared -> Observable.merge(
                    shared.ofType(StatisticsAction.LoadStatistics.class).compose(loadStatisticsProcessor),
                    shared.ofType(StatisticsAction.GetLastState.class).compose(getLastStateProcessor))
                    .mergeWith(
                            // Error for not implemented actions
                            shared.filter(v -> !(v instanceof StatisticsAction.LoadStatistics)
                                    && !(v instanceof StatisticsAction.GetLastState))
                                    .flatMap(w -> Observable.error(
                                            new IllegalArgumentException("Unknown Action type: " + w)))));

}
