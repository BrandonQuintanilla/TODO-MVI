package com.example.android.architecture.blueprints.todoapp;

import android.app.Application;
import com.squareup.leakcanary.LeakCanary;
import timber.log.Timber;

public class ToDoApplication extends Application {
  @Override public void onCreate() {
    super.onCreate();

    setupLeakCanary();
    setupTimber();
  }

  private void setupLeakCanary() {
    if (LeakCanary.isInAnalyzerProcess(this)) {
      // This process is dedicated to LeakCanary for heap analysis.
      // You should not init your app in this process.
      return;
    }
    LeakCanary.install(this);
  }

  private void setupTimber() {
    if (BuildConfig.DEBUG) {
      Timber.plant(new Timber.DebugTree());
    }
  }
}
