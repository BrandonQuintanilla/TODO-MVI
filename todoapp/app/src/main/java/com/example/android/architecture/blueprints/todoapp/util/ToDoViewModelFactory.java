package com.example.android.architecture.blueprints.todoapp.util;

import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.content.Context;

import com.example.android.architecture.blueprints.todoapp.Injection;
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskActionProcessorHolder;
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskViewModel;
import com.example.android.architecture.blueprints.todoapp.statistics.StatisticsActionProcessorHolder;
import com.example.android.architecture.blueprints.todoapp.statistics.StatisticsViewModel;
import com.example.android.architecture.blueprints.todoapp.tasks.TasksActionProcessorHolder;
import com.example.android.architecture.blueprints.todoapp.tasks.TasksViewModel;

public class ToDoViewModelFactory implements ViewModelProvider.Factory {
    @SuppressLint("StaticFieldLeak")
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

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        if (modelClass == StatisticsViewModel.class) {
            return (T) new StatisticsViewModel(
                    new StatisticsActionProcessorHolder(
                            Injection.provideTasksRepository(applicationContext),
                            Injection.provideSchedulerProvider()));
        }
        if (modelClass == TasksViewModel.class) {
            return (T) new TasksViewModel(
                    new TasksActionProcessorHolder(
                            Injection.provideTasksRepository(applicationContext),
                            Injection.provideSchedulerProvider()));
        }
        if (modelClass == AddEditTaskViewModel.class) {
            return (T) new AddEditTaskViewModel(
                    new AddEditTaskActionProcessorHolder(
                            Injection.provideTasksRepository(applicationContext),
                            Injection.provideSchedulerProvider()));
        }
        throw new IllegalArgumentException("unknown model class " + modelClass);
    }
}
