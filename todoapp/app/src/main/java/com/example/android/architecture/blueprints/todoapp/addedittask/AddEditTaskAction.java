package com.example.android.architecture.blueprints.todoapp.addedittask;

import android.support.annotation.NonNull;

import com.example.android.architecture.blueprints.todoapp.mvibase.MviAction;
import com.google.auto.value.AutoValue;

interface AddEditTaskAction extends MviAction {
    @AutoValue
    abstract class PopulateTask implements AddEditTaskAction {
        abstract String taskId();

        public static PopulateTask create(@NonNull String taskId) {
            return new AutoValue_AddEditTaskAction_PopulateTask(taskId);
        }
    }

    @AutoValue
    abstract class CreateTask implements AddEditTaskAction {
        abstract String title();

        abstract String description();

        public static CreateTask create(@NonNull String title, @NonNull String description) {
            return new AutoValue_AddEditTaskAction_CreateTask(title, description);
        }
    }

    @AutoValue
    abstract class UpdateTask implements AddEditTaskAction {
        abstract String taskId();

        abstract String title();

        abstract String description();

        public static UpdateTask create(@NonNull String talkId, @NonNull String title, @NonNull String description) {
            return new AutoValue_AddEditTaskAction_UpdateTask(talkId, title, description);
        }
    }

    @AutoValue
    abstract class GetLastState implements AddEditTaskAction {
        public static GetLastState create() {
            return new AutoValue_AddEditTaskAction_GetLastState();
        }
    }
}
