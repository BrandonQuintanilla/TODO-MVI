package com.example.android.architecture.blueprints.todoapp.taskdetail;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.mvibase.LceStatus;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviResult;
import com.google.auto.value.AutoValue;

import static com.example.android.architecture.blueprints.todoapp.mvibase.LceStatus.FAILURE;
import static com.example.android.architecture.blueprints.todoapp.mvibase.LceStatus.IN_FLIGHT;
import static com.example.android.architecture.blueprints.todoapp.mvibase.LceStatus.SUCCESS;

interface TaskDetailResult extends MviResult {

    @AutoValue
    abstract class PopulateTask implements TaskDetailResult {
        @NonNull
        abstract LceStatus status();

        @Nullable
        abstract Task task();

        @Nullable
        abstract Throwable error();

        @NonNull
        static PopulateTask success(@NonNull Task task) {
            return new AutoValue_TaskDetailResult_PopulateTask(LceStatus.SUCCESS, task, null);
        }

        @NonNull
        static PopulateTask failure(Throwable error) {
            return new AutoValue_TaskDetailResult_PopulateTask(LceStatus.FAILURE, null, error);
        }

        @NonNull
        static PopulateTask inFlight() {
            return new AutoValue_TaskDetailResult_PopulateTask(LceStatus.IN_FLIGHT, null, null);
        }
    }

    @AutoValue
    abstract class GetLastState implements TaskDetailResult {
        static GetLastState create() {
            return new AutoValue_TaskDetailResult_GetLastState();
        }
    }

    @AutoValue
    abstract class ActivateTaskResult implements TaskDetailResult {
        @NonNull
        abstract LceStatus status();

        @Nullable
        abstract Task task();

        @Nullable
        abstract Throwable error();

        @NonNull
        static ActivateTaskResult success(@NonNull Task task) {
            return new AutoValue_TaskDetailResult_ActivateTaskResult(SUCCESS, task, null);
        }

        @NonNull
        static ActivateTaskResult failure(Throwable error) {
            return new AutoValue_TaskDetailResult_ActivateTaskResult(FAILURE, null, error);
        }

        @NonNull
        static ActivateTaskResult inFlight() {
            return new AutoValue_TaskDetailResult_ActivateTaskResult(IN_FLIGHT, null, null);
        }
    }

    @AutoValue
    abstract class CompleteTaskResult implements TaskDetailResult {
        @NonNull
        abstract LceStatus status();

        @Nullable
        abstract Task task();

        @Nullable
        abstract Throwable error();

        @NonNull
        static CompleteTaskResult success(@NonNull Task task) {
            return new AutoValue_TaskDetailResult_CompleteTaskResult(SUCCESS, task, null);
        }

        @NonNull
        static CompleteTaskResult failure(Throwable error) {
            return new AutoValue_TaskDetailResult_CompleteTaskResult(FAILURE, null, error);
        }

        @NonNull
        static CompleteTaskResult inFlight() {
            return new AutoValue_TaskDetailResult_CompleteTaskResult(IN_FLIGHT, null, null);
        }
    }

    @AutoValue
    abstract class DeleteTaskResult implements TaskDetailResult {
        @NonNull
        abstract LceStatus status();

        @Nullable
        abstract Throwable error();

        @NonNull
        static DeleteTaskResult success() {
            return new AutoValue_TaskDetailResult_DeleteTaskResult(SUCCESS,  null);
        }

        @NonNull
        static DeleteTaskResult failure(Throwable error) {
            return new AutoValue_TaskDetailResult_DeleteTaskResult(FAILURE,  error);
        }

        @NonNull
        static DeleteTaskResult inFlight() {
            return new AutoValue_TaskDetailResult_DeleteTaskResult(IN_FLIGHT, null);
        }
    }
}
