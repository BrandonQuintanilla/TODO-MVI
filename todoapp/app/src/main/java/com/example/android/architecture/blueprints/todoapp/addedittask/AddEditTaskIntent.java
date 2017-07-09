package com.example.android.architecture.blueprints.todoapp.addedittask;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.android.architecture.blueprints.todoapp.mvibase.MviIntent;
import com.google.auto.value.AutoValue;

interface AddEditTaskIntent extends MviIntent {
    @AutoValue
    abstract class InitialIntent implements AddEditTaskIntent {
        @Nullable
        abstract String taskId();

        public static InitialIntent create(@Nullable String taskId) {
            return new AutoValue_AddEditTaskIntent_InitialIntent(taskId);
        }
    }

    @AutoValue
    abstract class GetLastState implements AddEditTaskIntent {
        public static GetLastState create() {
            return new AutoValue_AddEditTaskIntent_GetLastState();
        }
    }

    @AutoValue
    abstract class SaveTask implements AddEditTaskIntent {
        @Nullable
        abstract String taskId();

        abstract String title();

        abstract String description();

        public static SaveTask create(@Nullable String taskId, @NonNull String title, @NonNull String description) {
            return new AutoValue_AddEditTaskIntent_SaveTask(taskId, title, description);
        }
    }
}
