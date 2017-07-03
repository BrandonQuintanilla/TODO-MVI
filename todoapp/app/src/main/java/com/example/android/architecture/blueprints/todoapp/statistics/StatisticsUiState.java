package com.example.android.architecture.blueprints.todoapp.statistics;

import android.support.annotation.Nullable;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviUiState;
import com.google.auto.value.AutoValue;

@AutoValue abstract class StatisticsUiState implements MviUiState {
  abstract boolean isLoading();

  abstract int activeCount();

  abstract int completedCount();

  @Nullable abstract Throwable error();

  public abstract Builder buildWith();

  static StatisticsUiState idle() {
    return new AutoValue_StatisticsUiState.Builder().isLoading(false)
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

    abstract StatisticsUiState build();
  }
}
