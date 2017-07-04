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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.example.android.architecture.blueprints.todoapp.Injection;
import com.example.android.architecture.blueprints.todoapp.R;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviBaseView;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Main UI for the statistics screen.
 */
public class StatisticsFragment extends Fragment implements MviBaseView<StatisticsViewState> {
  private TextView statisticsTV;
  private StatisticsViewModel presenter;
  private CompositeDisposable disposables;

  public static StatisticsFragment newInstance() {
    return new StatisticsFragment();
  }

  @Nullable @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View root = inflater.inflate(R.layout.statistics_frag, container, false);
    statisticsTV = (TextView) root.findViewById(R.id.statistics);
    return root;
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    presenter = new StatisticsViewModel(
        Injection.provideTasksRepository(getActivity().getApplicationContext()),
        Injection.provideSchedulerProvider());
    disposables = new CompositeDisposable();
    bind();
  }

  private void bind() {
    disposables.add(presenter.states().subscribe(this::render));
    presenter.forwardIntents(intents());
  }

  @Override public void onDestroy() {
    super.onDestroy();
    disposables.dispose();
  }

  public boolean isActive() {
    return isAdded();
  }

  @Override public Observable<StatisticsIntent> intents() {
    return initialIntent();
  }

  private Observable<StatisticsIntent> initialIntent() {
    return Observable.just(StatisticsIntent.InitialIntent.create());
  }

  @Override public void render(StatisticsViewState state) {
    if (state.isLoading()) statisticsTV.setText(getString(R.string.loading));
    if (state.error() != null) {
      statisticsTV.setText(getResources().getString(R.string.statistics_error));
    }

    if (state.error() == null && !state.isLoading()) {
      showStatistics(state.activeCount(), state.completedCount());
    }
  }

  private void showStatistics(int numberOfActiveTasks, int numberOfCompletedTasks) {
    if (numberOfCompletedTasks == 0 && numberOfActiveTasks == 0) {
      statisticsTV.setText(getResources().getString(R.string.statistics_no_tasks));
    } else {
      String displayString = getResources().getString(R.string.statistics_active_tasks)
          + " "
          + numberOfActiveTasks
          + "\n"
          + getResources().getString(R.string.statistics_completed_tasks)
          + " "
          + numberOfCompletedTasks;
      statisticsTV.setText(displayString);
    }
  }
}
