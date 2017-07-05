package com.example.android.architecture.blueprints.todoapp.tasks;

import android.support.annotation.Nullable;
import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviAction;
import com.google.auto.value.AutoValue;

interface TasksAction extends MviAction {
  @AutoValue abstract class LoadTasks implements TasksAction {
    public abstract boolean forceUpdate();

    @Nullable public abstract TasksFilterType filterType();

    public static LoadTasks loadAndFilter(boolean forceUpdate, TasksFilterType filterType) {
      return new AutoValue_TasksAction_LoadTasks(forceUpdate, filterType);
    }

    public static LoadTasks load(boolean forceUpdate) {
      return new AutoValue_TasksAction_LoadTasks(forceUpdate, null);
    }
  }

  @AutoValue abstract class GetLastState implements TasksAction {
    public static GetLastState create() {
      return new AutoValue_TasksAction_GetLastState();
    }
  }

  @AutoValue abstract class ActivateTaskAction implements TasksAction {
    abstract Task task();

    public static ActivateTaskAction create(Task task) {
      return new AutoValue_TasksAction_ActivateTaskAction(task);
    }
  }

  @AutoValue abstract class CompleteTaskAction implements TasksAction {
    abstract Task task();

    public static CompleteTaskAction create(Task task) {
      return new AutoValue_TasksAction_CompleteTaskAction(task);
    }
  }

  @AutoValue abstract class ClearCompletedTasksAction implements TasksAction {
    public static ClearCompletedTasksAction create() {
      return new AutoValue_TasksAction_ClearCompletedTasksAction();
    }
  }
}
