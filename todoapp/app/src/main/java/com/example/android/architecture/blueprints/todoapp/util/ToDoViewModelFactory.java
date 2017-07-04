package com.example.android.architecture.blueprints.todoapp.util;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.content.Context;
import com.example.android.architecture.blueprints.todoapp.Injection;
import com.example.android.architecture.blueprints.todoapp.statistics.StatisticsViewModel;

public class ToDoViewModelFactory implements ViewModelProvider.Factory {
  private static ToDoViewModelFactory INSTANCE;

  private final Context applicationContext;

  private ToDoViewModelFactory(Context applicationContext) {
    this.applicationContext = applicationContext;
  }

  public static ToDoViewModelFactory getInstance(Context context) {
    if (INSTANCE == null) {
      INSTANCE = new ToDoViewModelFactory(context.getApplicationContext());
    }
    return INSTANCE;
  }

  @SuppressWarnings("unchecked") @Override
  public <T extends ViewModel> T create(Class<T> modelClass) {
    if (modelClass == StatisticsViewModel.class) {
      return (T) new StatisticsViewModel(Injection.provideTasksRepository(applicationContext),
          Injection.provideSchedulerProvider());
    } else {
      throw new IllegalArgumentException("unknown model class " + modelClass);
    }
  }
}
