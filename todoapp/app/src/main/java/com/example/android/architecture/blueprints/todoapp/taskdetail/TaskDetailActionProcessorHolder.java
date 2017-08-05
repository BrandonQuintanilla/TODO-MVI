package com.example.android.architecture.blueprints.todoapp.taskdetail;

import android.support.annotation.NonNull;

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository;
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;

import static com.google.common.base.Preconditions.checkNotNull;

public class TaskDetailActionProcessorHolder {
    @NonNull
    private TasksRepository mTasksRepository;
    @NonNull
    private BaseSchedulerProvider mSchedulerProvider;

    public TaskDetailActionProcessorHolder(@NonNull TasksRepository tasksRepository,
                                           @NonNull BaseSchedulerProvider schedulerProvider) {
        this.mTasksRepository = checkNotNull(tasksRepository, "tasksRepository cannot be null");
        this.mSchedulerProvider = checkNotNull(schedulerProvider, "schedulerProvider cannot be null");

    }

    private ObservableTransformer<TaskDetailAction.PopulateTask, TaskDetailResult.PopulateTask>
            populateTaskProcessor =
            actions -> actions.flatMap(action -> mTasksRepository
                    .getTask(action.taskId())
                    .toObservable()
                    .map(TaskDetailResult.PopulateTask::success)
                    .onErrorReturn(TaskDetailResult.PopulateTask::failure)
                    .subscribeOn(mSchedulerProvider.io())
                    .observeOn(mSchedulerProvider.ui())
                    .startWith(TaskDetailResult.PopulateTask.inFlight()));


    private ObservableTransformer<TaskDetailAction.GetLastState, TaskDetailResult.GetLastState>
            getLastStateProcessor =
            actions -> actions.map(ignored -> TaskDetailResult.GetLastState.create());

    private ObservableTransformer<TaskDetailAction.CompleteTask, TaskDetailResult.CompleteTaskResult>
            completeTaskProcessor = actions -> actions.flatMap(
            action -> mTasksRepository.completeTask(action.taskId())
                    .andThen(mTasksRepository.getTask(action.taskId()))
                    .toObservable()
                    .map(TaskDetailResult.CompleteTaskResult::success)
                    .onErrorReturn(TaskDetailResult.CompleteTaskResult::failure)
                    .subscribeOn(mSchedulerProvider.io())
                    .observeOn(mSchedulerProvider.ui())
                    .startWith(TaskDetailResult.CompleteTaskResult.inFlight()));

    private ObservableTransformer<TaskDetailAction.ActivateTask, TaskDetailResult.ActivateTaskResult>
            activateTaskProcessor = actions -> actions.flatMap(
            action -> mTasksRepository.activateTask(action.taskId())
                    .andThen(mTasksRepository.getTask(action.taskId()))
                    .toObservable()
                    .map(TaskDetailResult.ActivateTaskResult::success)
                    .onErrorReturn(TaskDetailResult.ActivateTaskResult::failure)
                    .subscribeOn(mSchedulerProvider.io())
                    .observeOn(mSchedulerProvider.ui())
                    .startWith(TaskDetailResult.ActivateTaskResult.inFlight()));

    private ObservableTransformer<TaskDetailAction.DeleteTask, TaskDetailResult.DeleteTaskResult>
            deleteTaskProcessor = actions -> actions.flatMap(
            action -> mTasksRepository.deleteTask(action.taskId())
                    .andThen(Observable.just(TaskDetailResult.DeleteTaskResult.success()))
                    .onErrorReturn(TaskDetailResult.DeleteTaskResult::failure)
                    .subscribeOn(mSchedulerProvider.io())
                    .observeOn(mSchedulerProvider.ui())
                    .startWith(TaskDetailResult.DeleteTaskResult.inFlight()));

    ObservableTransformer<TaskDetailAction, TaskDetailResult> actionProcessor =
            actions -> actions.publish(shared -> Observable.merge(
                    shared.ofType(TaskDetailAction.PopulateTask.class).compose(populateTaskProcessor),
                    shared.ofType(TaskDetailAction.CompleteTask.class).compose(completeTaskProcessor),
                    shared.ofType(TaskDetailAction.ActivateTask.class).compose(activateTaskProcessor),
                    shared.ofType(TaskDetailAction.DeleteTask.class).compose(deleteTaskProcessor),
                    shared.ofType(TaskDetailAction.GetLastState.class).compose(getLastStateProcessor))
                    .mergeWith(
                            // Error for not implemented actions
                            shared.filter(v -> !(v instanceof TaskDetailAction.PopulateTask) &&
                                    !(v instanceof TaskDetailAction.CompleteTask) &&
                                    !(v instanceof TaskDetailAction.ActivateTask) &&
                                    !(v instanceof TaskDetailAction.DeleteTask) &&
                                    !(v instanceof TaskDetailAction.GetLastState))
                                    .flatMap(w -> Observable.error(
                                            new IllegalArgumentException("Unknown Action type: " + w)))));


}
