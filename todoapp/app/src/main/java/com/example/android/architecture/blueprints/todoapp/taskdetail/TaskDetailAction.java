package com.example.android.architecture.blueprints.todoapp.taskdetail;

import android.support.annotation.NonNull;

import com.example.android.architecture.blueprints.todoapp.mvibase.MviAction;
import com.google.auto.value.AutoValue;

interface TaskDetailAction extends MviAction {
    @AutoValue
    abstract class PopulateTask implements TaskDetailAction {
        abstract String taskId();

        public static PopulateTask create(@NonNull String taskId) {
            return new AutoValue_TaskDetailAction_PopulateTask(taskId);
        }
    }

    @AutoValue
    abstract class DeleteTask implements TaskDetailAction {
        abstract String taskId();

        public static DeleteTask create(@NonNull String taskId) {
            return new AutoValue_TaskDetailAction_DeleteTask(taskId);
        }
    }

    @AutoValue
    abstract class ActivateTask implements TaskDetailAction {
        abstract String taskId();

        public static ActivateTask create(@NonNull String taskId) {
            return new AutoValue_TaskDetailAction_ActivateTask(taskId);
        }
    }

    @AutoValue
    abstract class CompleteTask implements TaskDetailAction {
        abstract String taskId();

        public static CompleteTask create(@NonNull String taskId) {
            return new AutoValue_TaskDetailAction_CompleteTask(taskId);
        }
    }

    @AutoValue
    abstract class GetLastState implements TaskDetailAction {
        public static GetLastState create() {
            return new AutoValue_TaskDetailAction_GetLastState();
        }
    }
}
