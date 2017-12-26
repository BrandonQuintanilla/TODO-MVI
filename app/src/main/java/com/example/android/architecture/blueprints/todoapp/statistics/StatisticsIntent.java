package com.example.android.architecture.blueprints.todoapp.statistics;

import com.example.android.architecture.blueprints.todoapp.mvibase.MviIntent;
import com.google.auto.value.AutoValue;

interface StatisticsIntent extends MviIntent {
    @AutoValue
    abstract class InitialIntent implements StatisticsIntent {
        public static InitialIntent create() {
            return new AutoValue_StatisticsIntent_InitialIntent();
        }
    }
}
