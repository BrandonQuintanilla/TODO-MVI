package com.example.android.architecture.blueprints.todoapp.tasks;

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
                    // Transform the Single to an Observable to allow emission of multiple
                    // events down the stream (e.g. the InFlight event)
                    .toObservable()
                    // Wrap returned data into an immutable object
                    .map(tasks -> TasksResult.LoadTasks.success(tasks, action.filterType()))
                    // Wrap any error into an immutable object and pass it down the stream
                    // without crashing.
                    // Because errors are data and hence, should just be part of the stream.
                    .onErrorReturn(TasksResult.LoadTasks::failure)
                    .subscribeOn(mSchedulerProvider.io())
                    .observeOn(mSchedulerProvider.ui())
                    // Emit an InFlight event to notify the subscribers (e.g. the UI) we are
                    // doing work and waiting on a response.
                    // We emit it after observing on the UI thread to allow the event to be emitted
                    // on the current frame and avoid jank.
                    .startWith(TasksResult.LoadTasks.inFlight()));

    private ObservableTransformer<TasksAction.ActivateTaskAction, TasksResult.ActivateTaskResult>
            activateTaskProcessor = actions -> actions.flatMap(
            action -> mTasksRepository.activateTask(action.task())
                    .andThen(mTasksRepository.getTasks())
                    // Transform the Single to an Observable to allow emission of multiple
                    // events down the stream (e.g. the InFlight event)
                    .toObservable()
                    .flatMap(tasks ->
                            // Emit two events to allow the UI notification to be hidden after
                            // some delay
                            ObservableUtilsKt.pairWithDelay(
                                    TasksResult.ActivateTaskResult.success(tasks),
                                    TasksResult.ActivateTaskResult.hideUiNotification())
                    )
                    // Wrap any error into an immutable object and pass it down the stream
                    // without crashing.
                    // Because errors are data and hence, should just be part of the stream.
                    .onErrorReturn(TasksResult.ActivateTaskResult::failure)
                    .subscribeOn(mSchedulerProvider.io())
                    .observeOn(mSchedulerProvider.ui())
                    // Emit an InFlight event to notify the subscribers (e.g. the UI) we are
                    // doing work and waiting on a response.
                    // We emit it after observing on the UI thread to allow the event to be emitted
                    // on the current frame and avoid jank.
                    .startWith(TasksResult.ActivateTaskResult.inFlight()));

    private ObservableTransformer<TasksAction.CompleteTaskAction, TasksResult.CompleteTaskResult>
            completeTaskProcessor = actions -> actions.flatMap(
            action -> mTasksRepository.completeTask(action.task())
                    .andThen(mTasksRepository.getTasks())
                    // Transform the Single to an Observable to allow emission of multiple
                    // events down the stream (e.g. the InFlight event)
                    .toObservable()
                    .flatMap(tasks ->
                            // Emit two events to allow the UI notification to be hidden after
                            // some delay
                            ObservableUtilsKt.pairWithDelay(
                                    TasksResult.CompleteTaskResult.success(tasks),
                                    TasksResult.CompleteTaskResult.hideUiNotification())
                    )
                    // Wrap any error into an immutable object and pass it down the stream
                    // without crashing.
                    // Because errors are data and hence, should just be part of the stream.
                    .onErrorReturn(TasksResult.CompleteTaskResult::failure)
                    .subscribeOn(mSchedulerProvider.io())
                    .observeOn(mSchedulerProvider.ui())
                    // Emit an InFlight event to notify the subscribers (e.g. the UI) we are
                    // doing work and waiting on a response.
                    // We emit it after observing on the UI thread to allow the event to be emitted
                    // on the current frame and avoid jank.
                    .startWith(TasksResult.CompleteTaskResult.inFlight()));

    private ObservableTransformer<TasksAction.ClearCompletedTasksAction, TasksResult.ClearCompletedTasksResult>
            clearCompletedTasksProcessor = actions -> actions.flatMap(
            action -> mTasksRepository.clearCompletedTasks()
                    .andThen(mTasksRepository.getTasks())
                    // Transform the Single to an Observable to allow emission of multiple
                    // events down the stream (e.g. the InFlight event)
                    .toObservable()
                    .flatMap(tasks ->
                            // Emit two events to allow the UI notification to be hidden after
                            // some delay
                            ObservableUtilsKt.pairWithDelay(
                                    TasksResult.ClearCompletedTasksResult.success(tasks),
                                    TasksResult.ClearCompletedTasksResult.hideUiNotification())
                    )
                    // Wrap any error into an immutable object and pass it down the stream
                    // without crashing.
                    // Because errors are data and hence, should just be part of the stream.
                    .onErrorReturn(TasksResult.ClearCompletedTasksResult::failure)
                    .subscribeOn(mSchedulerProvider.io())
                    .observeOn(mSchedulerProvider.ui())
                    // Emit an InFlight event to notify the subscribers (e.g. the UI) we are
                    // doing work and waiting on a response.
                    // We emit it after observing on the UI thread to allow the event to be emitted
                    // on the current frame and avoid jank.
                    .startWith(TasksResult.ClearCompletedTasksResult.inFlight()));

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
    ObservableTransformer<TasksAction, TasksResult> actionProcessor =
            actions -> actions.publish(shared -> Observable.merge(
                    // Match LoadTasks to loadTasksProcessor
                    shared.ofType(TasksAction.LoadTasks.class).compose(loadTasksProcessor),
                    // Match ActivateTaskAction to populateTaskProcessor
                    shared.ofType(TasksAction.ActivateTaskAction.class).compose(activateTaskProcessor),
                    // Match CompleteTaskAction to completeTaskProcessor
                    shared.ofType(TasksAction.CompleteTaskAction.class).compose(completeTaskProcessor),
                    // Match ClearCompletedTasksAction to clearCompletedTasksProcessor
                    shared.ofType(TasksAction.ClearCompletedTasksAction.class).compose(clearCompletedTasksProcessor))
                    .mergeWith(
                            // Error for not implemented actions
                            shared.filter(v -> !(v instanceof TasksAction.LoadTasks)
                                    && !(v instanceof TasksAction.ActivateTaskAction)
                                    && !(v instanceof TasksAction.CompleteTaskAction)
                                    && !(v instanceof TasksAction.ClearCompletedTasksAction))
                                    .flatMap(w -> Observable.error(
                                            new IllegalArgumentException("Unknown Action type: " + w)))));
}
