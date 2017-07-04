package com.example.android.architecture.blueprints.todoapp;

import android.app.Application;
import timber.log.Timber;

public class ToDoApplication extends Application {

  @Override public void onCreate() {
    super.onCreate();

    setupTimber();
  }

  private void setupTimber() {
    if (BuildConfig.DEBUG) {
      Timber.plant(new Timber.DebugTree());
    }
  }
}
