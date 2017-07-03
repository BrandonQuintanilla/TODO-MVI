package com.example.android.architecture.blueprints.todoapp.statistics;

import com.example.android.architecture.blueprints.todoapp.mvibase.MviAction;
import com.google.auto.value.AutoValue;

interface StatisticsAction extends MviAction {
  @AutoValue abstract class LoadStatistics implements StatisticsAction {
    public static LoadStatistics create() {
      return new AutoValue_StatisticsAction_LoadStatistics();
    }
  }
}
