package com.example.android.architecture.blueprints.todoapp.taskdetail;

import android.support.annotation.Nullable;

import com.example.android.architecture.blueprints.todoapp.mvibase.MviViewState;
import com.google.auto.value.AutoValue;

@AutoValue
abstract class TaskDetailViewState implements MviViewState {

    abstract String title();

    abstract String description();

    abstract boolean active();

    @Nullable
    abstract Throwable error();

    public abstract boolean taskComplete();

    public abstract boolean taskActivated();

    public abstract boolean taskDeleted();

    public abstract Builder buildWith();

    static TaskDetailViewState idle() {
        return new AutoValue_TaskDetailViewState.Builder()
                .title("")
                .description("")
                .active(false)
                .error(null)
                .taskComplete(false)
                .taskActivated(false)
                .taskDeleted(false)
                .build();
    }

    @AutoValue.Builder
    static abstract class Builder {
        abstract Builder title(String title);

        abstract Builder description(String description);

        abstract Builder active(boolean active);

        abstract Builder error(@Nullable Throwable error);

        abstract Builder taskComplete(boolean taskComplete);

        abstract Builder taskActivated(boolean taskActivated);

        abstract Builder taskDeleted(boolean taskDeleted);

        abstract TaskDetailViewState build();
    }
}
