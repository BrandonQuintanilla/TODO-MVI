package com.example.android.architecture.blueprints.todoapp.addedittask;

import android.support.annotation.NonNull;

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviAction;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviResult;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviViewModel;
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
public class AddEditTaskActionProcessorHolder {
    @NonNull
    private TasksRepository mTasksRepository;
    @NonNull
    private BaseSchedulerProvider mSchedulerProvider;

    public AddEditTaskActionProcessorHolder(@NonNull TasksRepository tasksRepository,
                                            @NonNull BaseSchedulerProvider schedulerProvider) {
        this.mTasksRepository = checkNotNull(tasksRepository, "tasksRepository cannot be null");
        this.mSchedulerProvider = checkNotNull(schedulerProvider, "schedulerProvider cannot be null");
    }

    private ObservableTransformer<AddEditTaskAction.PopulateTask, AddEditTaskResult.PopulateTask>
            populateTaskProcessor =
            actions -> actions.flatMap(action ->
                    mTasksRepository.getTask(action.taskId())
                            // Transform the Single to an Observable to allow emission of multiple
                            // events down the stream (e.g. the InFlight event)
                            .toObservable()
                            // Wrap returned data into an immutable object
                            .map(AddEditTaskResult.PopulateTask::success)
                            // Wrap any error into an immutable object and pass it down the stream
                            // without crashing.
                            // Because errors are data and hence, should just be part of the stream.
                            .onErrorReturn(AddEditTaskResult.PopulateTask::failure)
                            .subscribeOn(mSchedulerProvider.io())
                            .observeOn(mSchedulerProvider.ui())
                            // Emit an InFlight event to notify the subscribers (e.g. the UI) we are
                            // doing work and waiting on a response.
                            // We emit it after observing on the UI thread to allow the event to be emitted
                            // on the current frame and avoid jank.
                            .startWith(AddEditTaskResult.PopulateTask.inFlight()));

    private ObservableTransformer<AddEditTaskAction.CreateTask, AddEditTaskResult.CreateTask>
            createTaskProcessor =
            actions -> actions.map(action -> {
                Task task = new Task(action.title(), action.description());
                if (task.isEmpty()) {
                    return AddEditTaskResult.CreateTask.empty();
                }
                mTasksRepository.saveTask(task);
                return AddEditTaskResult.CreateTask.success();
            });

    private ObservableTransformer<AddEditTaskAction.UpdateTask, AddEditTaskResult.UpdateTask>
            updateTaskProcessor =
            actions -> actions.flatMap(action ->
                    mTasksRepository.saveTask(
                            new Task(action.title(), action.description(), action.taskId())
                    )
                            .andThen(Observable.just(AddEditTaskResult.UpdateTask.create())));

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
    ObservableTransformer<AddEditTaskAction, AddEditTaskResult> actionProcessor =
            actions -> actions.publish(shared -> Observable.merge(
                    shared.ofType(AddEditTaskAction.PopulateTask.class).compose(populateTaskProcessor),
                    shared.ofType(AddEditTaskAction.CreateTask.class).compose(createTaskProcessor),
                    shared.ofType(AddEditTaskAction.UpdateTask.class).compose(updateTaskProcessor))
                    .mergeWith(
                            // Error for not implemented actions
                            shared.filter(v -> !(v instanceof AddEditTaskAction.PopulateTask) &&
                                    !(v instanceof AddEditTaskAction.CreateTask) &&
                                    !(v instanceof AddEditTaskAction.UpdateTask))
                                    .flatMap(w -> Observable.error(
                                            new IllegalArgumentException("Unknown Action type: " + w)))));


}
