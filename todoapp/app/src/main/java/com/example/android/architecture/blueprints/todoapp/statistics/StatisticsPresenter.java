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

import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository;
import com.example.android.architecture.blueprints.todoapp.util.EspressoIdlingResource;
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Listens to user actions from the UI ({@link StatisticsFragment}), retrieves the data and updates
 * the UI as required.
 */
public class StatisticsPresenter implements StatisticsContract.Presenter {

  @NonNull private final TasksRepository mTasksRepository;

  @NonNull private final StatisticsContract.View mStatisticsView;

  @NonNull private final BaseSchedulerProvider mSchedulerProvider;

  @NonNull private CompositeDisposable disposables;

  public StatisticsPresenter(@NonNull TasksRepository tasksRepository,
      @NonNull StatisticsContract.View statisticsView,
      @NonNull BaseSchedulerProvider schedulerProvider) {
    mTasksRepository = checkNotNull(tasksRepository, "tasksRepository cannot be null");
    mStatisticsView = checkNotNull(statisticsView, "statisticsView cannot be null!");
    mSchedulerProvider = checkNotNull(schedulerProvider, "schedulerProvider cannot be null");

    disposables = new CompositeDisposable();
    mStatisticsView.setPresenter(this);
  }

  @Override public void subscribe() {
    loadStatistics();
  }

  @Override public void unsubscribe() {
    disposables.clear();
  }

  private void loadStatistics() {
    mStatisticsView.setProgressIndicator(true);

    // The network request might be handled in a different thread so make sure Espresso knows
    // that the app is busy until the response is handled.
    EspressoIdlingResource.increment(); // App is busy until further notice

    Observable<Task> tasks =
        mTasksRepository.getTasks().toObservable().flatMap(Observable::fromIterable);
    Single<Long> completedTasks = tasks.filter(Task::isCompleted).count();
    Single<Long> activeTasks = tasks.filter(Task::isActive).count();
    Disposable disposable = Single.zip(completedTasks, activeTasks,
        (completed, active) -> Pair.create(active, completed))
        .subscribeOn(mSchedulerProvider.computation())
        .observeOn(mSchedulerProvider.ui())
        .doOnSuccess(ignored -> {
          if (!EspressoIdlingResource.getIdlingResource().isIdleNow()) {
            EspressoIdlingResource.decrement(); // Set app as idle.
          }
        })
        .subscribe(
            // onSuccess
            stats -> {
              mStatisticsView.showStatistics(stats.first.intValue(), stats.second.intValue());
              mStatisticsView.setProgressIndicator(false);
            },
            // onError
            throwable -> mStatisticsView.showLoadingStatisticsError());
    disposables.add(disposable);
  }
}
