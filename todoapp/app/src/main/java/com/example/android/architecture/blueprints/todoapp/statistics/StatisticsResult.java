package com.example.android.architecture.blueprints.todoapp.statistics;

import android.support.annotation.NonNull;

import com.example.android.architecture.blueprints.todoapp.util.LceStatus;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviResult;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

interface StatisticsResult extends MviResult {
    @AutoValue
    abstract class LoadStatistics implements StatisticsResult {
        @NonNull
        abstract LceStatus status();

        abstract int activeCount();

        abstract int completedCount();

        @Nullable
        abstract Throwable error();

        @NonNull
        static LoadStatistics success(int activeCount, int completedCount) {
            return new AutoValue_StatisticsResult_LoadStatistics(LceStatus.SUCCESS, activeCount,
                    completedCount, null);
        }

        @NonNull
        static LoadStatistics failure(Throwable error) {
            return new AutoValue_StatisticsResult_LoadStatistics(LceStatus.FAILURE, 0, 0, error);
        }

        @NonNull
        static LoadStatistics inFlight() {
            return new AutoValue_StatisticsResult_LoadStatistics(LceStatus.IN_FLIGHT, 0, 0, null);
        }
    }

    @AutoValue
    abstract class GetLastState implements StatisticsResult {
        static GetLastState create() {
            return new AutoValue_StatisticsResult_GetLastState();
        }
    }
}
