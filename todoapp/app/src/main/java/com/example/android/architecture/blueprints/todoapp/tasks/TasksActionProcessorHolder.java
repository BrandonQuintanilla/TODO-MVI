package com.example.android.architecture.blueprints.todoapp.tasks;

import android.support.annotation.NonNull;

import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository;
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;

import static com.google.common.base.Preconditions.checkNotNull;

public class TasksActionProcessorHolder {
    @NonNull
    private TasksRepository mTasksRepository;
    @NonNull
    private BaseSchedulerProvider mSchedulerProvider;

    public TasksActionProcessorHolder(@NonNull TasksRepository tasksRepository,
                                      @NonNull BaseSchedulerProvider schedulerProvider) {
        this.mTasksRepository = checkNotNull(tasksRepository, "tasksRepository cannot be null");
        this.mSchedulerProvider = checkNotNull(schedulerProvider, "schedulerProvider cannot be null");
    }

    private ObservableTransformer<TasksAction.LoadTasks, TasksResult.LoadTasks> loadTasksProcessor =
            actions -> actions.flatMap(action -> mTasksRepository.getTasks(action.forceUpdate())
                    .toObservable()
                    .map(tasks -> TasksResult.LoadTasks.success(tasks, action.filterType()))
                    .onErrorReturn(TasksResult.LoadTasks::failure)
                    .subscribeOn(mSchedulerProvider.io())
                    .observeOn(mSchedulerProvider.ui())
                    .startWith(TasksResult.LoadTasks.inFlight()));

    private ObservableTransformer<TasksAction.GetLastState, TasksResult.GetLastState>
            getLastStateProcessor = actions -> actions.map(ignored -> TasksResult.GetLastState.create());

    private ObservableTransformer<TasksAction.ActivateTaskAction, TasksResult.ActivateTaskResult>
            activateTaskProcessor = actions -> actions.flatMap(
            action -> mTasksRepository.activateTask(action.task())
                    .andThen(mTasksRepository.getTasks())
                    .toObservable()
                    .map(TasksResult.ActivateTaskResult::success)
                    .onErrorReturn(TasksResult.ActivateTaskResult::failure)
                    .subscribeOn(mSchedulerProvider.io())
                    .observeOn(mSchedulerProvider.ui())
                    .startWith(TasksResult.ActivateTaskResult.inFlight()));

    private ObservableTransformer<TasksAction.CompleteTaskAction, TasksResult.CompleteTaskResult>
            completeTaskProcessor = actions -> actions.flatMap(
            action -> mTasksRepository.completeTask(action.task())
                    .andThen(mTasksRepository.getTasks())
                    .toObservable()
                    .map(TasksResult.CompleteTaskResult::success)
                    .onErrorReturn(TasksResult.CompleteTaskResult::failure)
                    .subscribeOn(mSchedulerProvider.io())
                    .observeOn(mSchedulerProvider.ui())
                    .startWith(TasksResult.CompleteTaskResult.inFlight()));

    private ObservableTransformer<TasksAction.ClearCompletedTasksAction, TasksResult.ClearCompletedTasksResult>
            clearCompletedTasksProcessor = actions -> actions.flatMap(
            action -> mTasksRepository.clearCompletedTasks()
                    .andThen(mTasksRepository.getTasks())
                    .toObservable()
                    .map(TasksResult.ClearCompletedTasksResult::success)
                    .onErrorReturn(TasksResult.ClearCompletedTasksResult::failure)
                    .subscribeOn(mSchedulerProvider.io())
                    .observeOn(mSchedulerProvider.ui())
                    .startWith(TasksResult.ClearCompletedTasksResult.inFlight()));

    ObservableTransformer<TasksAction, TasksResult> actionProcessor =
            (Observable<TasksAction> actions) -> actions.publish(shared -> Observable.merge(
                    shared.ofType(TasksAction.LoadTasks.class).compose(loadTasksProcessor),
                    shared.ofType(TasksAction.GetLastState.class).compose(getLastStateProcessor),
                    shared.ofType(TasksAction.ActivateTaskAction.class).compose(activateTaskProcessor),
                    shared.ofType(TasksAction.CompleteTaskAction.class).compose(completeTaskProcessor))
                    .mergeWith(shared.ofType(TasksAction.ClearCompletedTasksAction.class)
                            .compose(clearCompletedTasksProcessor))
                    .mergeWith(
                            // Error for not implemented actions
                            shared.filter(v -> !(v instanceof TasksAction.LoadTasks)
                                    && !(v instanceof TasksAction.GetLastState)
                                    && !(v instanceof TasksAction.ActivateTaskAction)
                                    && !(v instanceof TasksAction.CompleteTaskAction)
                                    && !(v instanceof TasksAction.ClearCompletedTasksAction))
                                    .flatMap(w -> Observable.error(
                                            new IllegalArgumentException("Unknown Action type: " + w)))));
}
