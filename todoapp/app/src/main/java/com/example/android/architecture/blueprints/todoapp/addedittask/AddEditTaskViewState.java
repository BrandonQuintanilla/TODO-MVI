package com.example.android.architecture.blueprints.todoapp.addedittask;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.android.architecture.blueprints.todoapp.mvibase.MviViewState;
import com.google.auto.value.AutoValue;

@AutoValue
abstract class AddEditTaskViewState implements MviViewState {
    abstract String title();

    abstract String description();

    abstract boolean isEmpty();

    abstract boolean isSaved();

    @Nullable
    abstract Throwable error();

    public abstract Builder buildWith();

    static AddEditTaskViewState idle() {
        return new AutoValue_AddEditTaskViewState.Builder()
                .title("")
                .description("")
                .error(null)
                .isEmpty(false)
                .isSaved(false)
                .build();
    }

    @AutoValue.Builder
    static abstract class Builder {
        abstract Builder title(String title);

        abstract Builder description(String description);

        abstract Builder isEmpty(boolean isEmpty);

        abstract Builder isSaved(boolean isSaved);

        abstract Builder error(@NonNull Throwable error);

        abstract AddEditTaskViewState build();
    }
}
