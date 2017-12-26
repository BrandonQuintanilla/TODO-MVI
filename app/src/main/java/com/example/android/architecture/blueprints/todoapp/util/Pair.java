package com.example.android.architecture.blueprints.todoapp.util;

import com.google.auto.value.AutoValue;

/**
 * Helper class to wrap two objects into a immutable one.
 */
@AutoValue
public abstract class Pair<F, S> {
    abstract public F first();

    abstract public S second();

    public static <A, B> Pair<A, B> create(A a, B b) {
        return new AutoValue_Pair<>(a, b);
    }
}