package com.example.android.architecture.blueprints.todoapp.taskdetail;

import android.support.annotation.NonNull;
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviAction;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviResult;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviViewModel;
import com.example.android.architecture.blueprints.todoapp.util.ObservableUtilsKt;
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.Function;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Contains and executes the business logic for all emitted {@link MviAction}
 * and returns one unique {@link Observable} of {@link MviResult}.
 * <p>
 * This could have been included inside the {@link MviViewModel}
 * but was separated to ease maintenance, as the {@link MviViewModel} was getting too big.
 */
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
            actions -> actions.flatMap(action ->
                    mTasksRepository.getTask(action.taskId())
                            // Transform the Single to an Observable to allow emission of multiple
                            // events down the stream (e.g. the InFlight event)
                            .toObservable()
                            // Wrap returned data into an immutable object
                            .map(TaskDetailResult.PopulateTask::success)
                            // Wrap any error into an immutable object and pass it down the stream
                            // without crashing.
                            // Because errors are data and hence, should just be part of the stream.
                            .onErrorReturn(TaskDetailResult.PopulateTask::failure)
                            .subscribeOn(mSchedulerProvider.io())
                            .observeOn(mSchedulerProvider.ui())
                            // Emit an InFlight event to notify the subscribers (e.g. the UI) we are
                            // doing work and waiting on a response.
                            // We emit it after observing on the UI thread to allow the event to be emitted
                            // on the current frame and avoid jank.
                            .startWith(TaskDetailResult.PopulateTask.inFlight()));

    private ObservableTransformer<TaskDetailAction.CompleteTask, TaskDetailResult.CompleteTaskResult>
            completeTaskProcessor = actions -> actions.flatMap(
            action -> mTasksRepository.completeTask(action.taskId())
                    .andThen(mTasksRepository.getTask(action.taskId()))
                    // Transform the Single to an Observable to allow emission of multiple
                    // events down the stream (e.g. the InFlight event)
                    .toObservable()
                    .flatMap(task ->
                            // Emit two events to allow the UI notification to be hidden after
                            // some delay
                        ObservableUtilsKt.pairWithDelay(
                                    TaskDetailResult.CompleteTaskResult.success(task),
                                    TaskDetailResult.CompleteTaskResult.hideUiNotification()))
                    // Wrap any error into an immutable object and pass it down the stream
                    // without crashing.
                    // Because errors are data and hence, should just be part of the stream.
                    .onErrorReturn(TaskDetailResult.CompleteTaskResult::failure)
                    .subscribeOn(mSchedulerProvider.io())
                    .observeOn(mSchedulerProvider.ui())
                    // Emit an InFlight event to notify the subscribers (e.g. the UI) we are
                    // doing work and waiting on a response.
                    // We emit it after observing on the UI thread to allow the event to be emitted
                    // on the current frame and avoid jank.
                    .startWith(TaskDetailResult.CompleteTaskResult.inFlight()));

    private ObservableTransformer<TaskDetailAction.ActivateTask, TaskDetailResult.ActivateTaskResult>
            activateTaskProcessor = actions -> actions.flatMap(
            action -> mTasksRepository.activateTask(action.taskId())
                    .andThen(mTasksRepository.getTask(action.taskId()))
                    // Transform the Single to an Observable to allow emission of multiple
                    // events down the stream (e.g. the InFlight event)
                    .toObservable()
                    .flatMap(task ->
                            // Emit two events to allow the UI notification to be hidden after
                            // some delay
                        ObservableUtilsKt.pairWithDelay(
                            TaskDetailResult.ActivateTaskResult.success(task),
                            TaskDetailResult.ActivateTaskResult.hideUiNotification()))
                    // Wrap any error into an immutable object and pass it down the stream
                    // without crashing.
                    // Because errors are data and hence, should just be part of the stream.
                    .onErrorReturn(TaskDetailResult.ActivateTaskResult::failure)
                    .subscribeOn(mSchedulerProvider.io())
                    .observeOn(mSchedulerProvider.ui())
                    // Emit an InFlight event to notify the subscribers (e.g. the UI) we are
                    // doing work and waiting on a response.
                    // We emit it after observing on the UI thread to allow the event to be emitted
                    // on the current frame and avoid jank.
                    .startWith(TaskDetailResult.ActivateTaskResult.inFlight()));

    private ObservableTransformer<TaskDetailAction.DeleteTask, TaskDetailResult.DeleteTaskResult>
            deleteTaskProcessor = actions -> actions.flatMap(
            action -> mTasksRepository.deleteTask(action.taskId())
                    .andThen(Observable.just(TaskDetailResult.DeleteTaskResult.success()))
                    // Wrap any error into an immutable object and pass it down the stream
                    // without crashing.
                    // Because errors are data and hence, should just be part of the stream.
                    .onErrorReturn(TaskDetailResult.DeleteTaskResult::failure)
                    .subscribeOn(mSchedulerProvider.io())
                    .observeOn(mSchedulerProvider.ui())
                    // Emit an InFlight event to notify the subscribers (e.g. the UI) we are
                    // doing work and waiting on a response.
                    // We emit it after observing on the UI thread to allow the event to be emitted
                    // on the current frame and avoid jank.
                    .startWith(TaskDetailResult.DeleteTaskResult.inFlight()));

    /**
     * Splits the {@link Observable<MviAction>} to match each type of {@link MviAction} to
     * its corresponding business logic processor. Each processor takes a defined {@link MviAction},
     * returns a defined {@link MviResult}
     * The global actionProcessor then merges all {@link Observable<MviResult>} back to
     * one unique {@link Observable<MviResult>}.
     * <p>
     * The splitting is done using {@link Observable#publish(Function)} which allows almost anything
     * on the passed {@link Observable} as long as one and only one {@link Observable} is returned.
     * <p>
     * An security layer is also added for unhandled {@link MviAction} to allow early crash
     * at runtime to easy the maintenance.
     */
    ObservableTransformer<TaskDetailAction, TaskDetailResult> actionProcessor =
            actions -> actions.publish(shared -> Observable.merge(
                    // Match PopulateTasks to populateTaskProcessor
                    shared.ofType(TaskDetailAction.PopulateTask.class).compose(populateTaskProcessor),
                    // Match CompleteTask to completeTaskProcessor
                    shared.ofType(TaskDetailAction.CompleteTask.class).compose(completeTaskProcessor),
                    // Match ActivateTask to activateTaskProcessor
                    shared.ofType(TaskDetailAction.ActivateTask.class).compose(activateTaskProcessor),
                    // Match DeleteTask to deleteTaskProcessor
                    shared.ofType(TaskDetailAction.DeleteTask.class).compose(deleteTaskProcessor))
                    .mergeWith(
                            // Error for not implemented actions
                            shared.filter(v -> !(v instanceof TaskDetailAction.PopulateTask) &&
                                    !(v instanceof TaskDetailAction.CompleteTask) &&
                                    !(v instanceof TaskDetailAction.ActivateTask) &&
                                    !(v instanceof TaskDetailAction.DeleteTask))
                                    .flatMap(w -> Observable.error(
                                            new IllegalArgumentException("Unknown Action type: " + w)))));


}
