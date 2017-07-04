package com.example.android.architecture.blueprints.todoapp.statistics;

import android.support.annotation.Nullable;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviViewState;
import com.google.auto.value.AutoValue;

@AutoValue abstract class StatisticsViewState implements MviViewState {
  abstract boolean isLoading();

  abstract int activeCount();

  abstract int completedCount();

  @Nullable abstract Throwable error();

  public abstract Builder buildWith();

  static StatisticsViewState idle() {
    return new AutoValue_StatisticsViewState.Builder().isLoading(false)
        .activeCount(0)
        .completedCount(0)
        .error(null)
        .build();
  }

  @AutoValue.Builder static abstract class Builder {
    abstract Builder isLoading(boolean isLoading);

    abstract Builder activeCount(int activeCount);

    abstract Builder completedCount(int completedCount);

    abstract Builder error(@Nullable Throwable error);

    abstract StatisticsViewState build();
  }
}
