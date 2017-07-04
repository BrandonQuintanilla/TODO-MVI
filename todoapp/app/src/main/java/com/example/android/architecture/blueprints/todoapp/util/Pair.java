package com.example.android.architecture.blueprints.todoapp.util;

import com.google.auto.value.AutoValue;

@AutoValue public abstract class Pair<F, S> {
  abstract public F first();

  abstract public S second();

  public static <A, B> Pair<A, B> create(A a, B b) {
    return new AutoValue_Pair<>(a, b);
  }
}