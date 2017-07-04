package com.example.android.architecture.blueprints.todoapp.tasks;

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviIntent;
import com.google.auto.value.AutoValue;

interface TasksIntent extends MviIntent {
  @AutoValue abstract class InitialIntent implements TasksIntent {
    public static InitialIntent create() {
      return new AutoValue_TasksIntent_InitialIntent();
    }
  }

  @AutoValue abstract class GetLastState implements TasksIntent {
    public static GetLastState create() {
      return new AutoValue_TasksIntent_GetLastState();
    }
  }

  @AutoValue abstract class RefreshIntent implements TasksIntent {
    public static RefreshIntent create() {
      return new AutoValue_TasksIntent_RefreshIntent();
    }
  }

  @AutoValue abstract class ActivateTaskIntent implements TasksIntent {
    abstract Task task();

    public static ActivateTaskIntent create(Task task) {
      return new AutoValue_TasksIntent_ActivateTaskIntent(task);
    }
  }

  @AutoValue abstract class CompleteTaskIntent implements TasksIntent {
    abstract Task task();

    public static CompleteTaskIntent create(Task task) {
      return new AutoValue_TasksIntent_CompleteTaskIntent(task);
    }
  }

  @AutoValue abstract class ClearCompletedTasksIntent implements TasksIntent {
    public static ClearCompletedTasksIntent create() {
      return new AutoValue_TasksIntent_ClearCompletedTasksIntent();
    }
  }
}
