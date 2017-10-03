package com.example.android.architecture.blueprints.todoapp.addedittask;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.util.LceStatus;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviResult;
import com.google.auto.value.AutoValue;


interface AddEditTaskResult extends MviResult {
    @AutoValue
    abstract class PopulateTask implements AddEditTaskResult {
        @NonNull
        abstract LceStatus status();

        @Nullable
        abstract Task task();

        @Nullable
        abstract Throwable error();

        @NonNull
        static PopulateTask success(@NonNull Task task) {
            return new AutoValue_AddEditTaskResult_PopulateTask(LceStatus.SUCCESS, task, null);
        }

        @NonNull
        static PopulateTask failure(Throwable error) {
            return new AutoValue_AddEditTaskResult_PopulateTask(LceStatus.FAILURE, null, error);
        }

        @NonNull
        static PopulateTask inFlight() {
            return new AutoValue_AddEditTaskResult_PopulateTask(LceStatus.IN_FLIGHT, null, null);
        }
    }

    @AutoValue
    abstract class CreateTask implements AddEditTaskResult {
        abstract boolean isEmpty();

        static CreateTask success() {
            return new AutoValue_AddEditTaskResult_CreateTask(false);
        }

        static CreateTask empty() {
            return new AutoValue_AddEditTaskResult_CreateTask(true);
        }
    }

    @AutoValue
    abstract class UpdateTask implements AddEditTaskResult {
        static UpdateTask create() {
            return new AutoValue_AddEditTaskResult_UpdateTask();
        }
    }

    @AutoValue
    abstract class GetLastState implements AddEditTaskResult {
        static GetLastState create() {
            return new AutoValue_AddEditTaskResult_GetLastState();
        }
    }
}
