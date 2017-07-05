/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.architecture.blueprints.todoapp.tasks;

import android.app.Activity;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.LifecycleRegistryOwner;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.example.android.architecture.blueprints.todoapp.R;
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskActivity;
import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviView;
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailActivity;
import com.example.android.architecture.blueprints.todoapp.util.ToDoViewModelFactory;
import com.jakewharton.rxbinding2.support.v4.widget.RxSwipeRefreshLayout;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.PublishSubject;
import java.util.ArrayList;

import static com.example.android.architecture.blueprints.todoapp.tasks.TasksFilterType.ACTIVE_TASKS;
import static com.example.android.architecture.blueprints.todoapp.tasks.TasksFilterType.ALL_TASKS;
import static com.example.android.architecture.blueprints.todoapp.tasks.TasksFilterType.COMPLETED_TASKS;

/**
 * Display a grid of {@link Task}s. User can choose to view all, active or completed tasks.
 */
public class TasksFragment extends Fragment
    implements LifecycleRegistryOwner, MviView<TasksViewState> {
  LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(this);

  private TasksViewModel viewModel;
  private TasksAdapter listAdapter;
  private View noTasksView;
  private ImageView noTaskIcon;
  private TextView noTaskMainView;
  private TextView noTaskAddView;
  private LinearLayout tasksView;
  private TextView filteringLabelView;
  private ScrollChildSwipeRefreshLayout swipeRefreshLayout;
  private PublishSubject<TasksIntent.RefreshIntent> refreshIntentPublisher =
      PublishSubject.create();
  private PublishSubject<TasksIntent.ClearCompletedTasksIntent> clearCompletedTaskIntentPublisher =
      PublishSubject.create();
  private PublishSubject<TasksIntent.ChangeFilterIntent> changeFilterIntentPublisher =
      PublishSubject.create();
  private CompositeDisposable disposables = new CompositeDisposable();

  public static TasksFragment newInstance() {
    return new TasksFragment();
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    listAdapter = new TasksAdapter(new ArrayList<>(0));
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    viewModel = ViewModelProviders.of(this, ToDoViewModelFactory.getInstance(getContext()))
        .get(TasksViewModel.class);
    bind();
  }

  private void bind() {
    disposables.add(viewModel.states().subscribe(this::render));
    viewModel.forwardIntents(intents());

    disposables.add(
        listAdapter.getTaskClickObservable().subscribe(task -> showTaskDetailsUi(task.getId())));
  }

  @Override public void onDestroy() {
    super.onDestroy();

    disposables.dispose();
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    // TODO(benoit) is this the right place?
    refreshIntentPublisher.onNext(TasksIntent.RefreshIntent.create(false));
    // If a task was successfully added, show snackbar
    if (AddEditTaskActivity.REQUEST_ADD_TASK == requestCode && Activity.RESULT_OK == resultCode) {
      showSuccessfullySavedMessage();
    }
  }

  @Nullable @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View root = inflater.inflate(R.layout.tasks_frag, container, false);

    // Set up tasks view
    ListView listView = (ListView) root.findViewById(R.id.tasks_list);
    listView.setAdapter(listAdapter);
    filteringLabelView = (TextView) root.findViewById(R.id.filteringLabel);
    tasksView = (LinearLayout) root.findViewById(R.id.tasksLL);

    // Set up  no tasks view
    noTasksView = root.findViewById(R.id.noTasks);
    noTaskIcon = (ImageView) root.findViewById(R.id.noTasksIcon);
    noTaskMainView = (TextView) root.findViewById(R.id.noTasksMain);
    noTaskAddView = (TextView) root.findViewById(R.id.noTasksAdd);
    noTaskAddView.setOnClickListener(ignored -> showAddTask());

    // Set up floating action button
    FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab_add_task);

    fab.setImageResource(R.drawable.ic_add);
    fab.setOnClickListener(ignored -> showAddTask());

    // Set up progress indicator
    swipeRefreshLayout = (ScrollChildSwipeRefreshLayout) root.findViewById(R.id.refresh_layout);
    swipeRefreshLayout.setColorSchemeColors(
        ContextCompat.getColor(getActivity(), R.color.colorPrimary),
        ContextCompat.getColor(getActivity(), R.color.colorAccent),
        ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark));
    // Set the scrolling view in the custom SwipeRefreshLayout.
    swipeRefreshLayout.setScrollUpChild(listView);

    setHasOptionsMenu(true);

    return root;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_clear:
        clearCompletedTaskIntentPublisher.onNext(TasksIntent.ClearCompletedTasksIntent.create());
        break;
      case R.id.menu_filter:
        showFilteringPopUpMenu();
        break;
      case R.id.menu_refresh:
        refreshIntentPublisher.onNext(TasksIntent.RefreshIntent.create(true));
        break;
    }
    return true;
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.tasks_fragment_menu, menu);
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override public Observable<TasksIntent> intents() {
    return Observable.merge(initialIntent(), refreshIntent(), adapterIntents(),
        clearCompletedTaskIntent()).mergeWith(changeFilterIntent());
  }

  @Override public void render(TasksViewState state) {
    swipeRefreshLayout.setRefreshing(state.isLoading());
    if (state.error() != null) {
      showLoadingTasksError();
      return;
    }

    if (state.taskActivated()) showMessage(getString(R.string.task_marked_active));

    if (state.taskComplete()) showMessage(getString(R.string.task_marked_complete));

    if (state.completedTasksCleared()) showMessage(getString(R.string.completed_tasks_cleared));

    if (state.tasks().isEmpty()) {
      switch (state.tasksFilterType()) {
        case ACTIVE_TASKS:
          showNoActiveTasks();
          break;
        case COMPLETED_TASKS:
          showNoCompletedTasks();
          break;
        default:
          showNoTasks();
          break;
      }
    } else {
      listAdapter.replaceData(state.tasks());

      tasksView.setVisibility(View.VISIBLE);
      noTasksView.setVisibility(View.GONE);

      switch (state.tasksFilterType()) {
        case ACTIVE_TASKS:
          showActiveFilterLabel();
          break;
        case COMPLETED_TASKS:
          showCompletedFilterLabel();
          break;
        default:
          showAllFilterLabel();
          break;
      }
    }
  }

  @Override public LifecycleRegistry getLifecycle() {
    return lifecycleRegistry;
  }

  private void showFilteringPopUpMenu() {
    PopupMenu popup = new PopupMenu(getContext(), getActivity().findViewById(R.id.menu_filter));
    popup.getMenuInflater().inflate(R.menu.filter_tasks, popup.getMenu());
    popup.setOnMenuItemClickListener(item -> {
      switch (item.getItemId()) {
        case R.id.active:
          changeFilterIntentPublisher.onNext(TasksIntent.ChangeFilterIntent.create(ACTIVE_TASKS));
          break;
        case R.id.completed:
          changeFilterIntentPublisher.onNext(
              TasksIntent.ChangeFilterIntent.create(COMPLETED_TASKS));
          break;
        default:
          changeFilterIntentPublisher.onNext(TasksIntent.ChangeFilterIntent.create(ALL_TASKS));
          break;
      }
      return true;
    });

    popup.show();
  }

  private void showMessage(String message) {
    View view = getView();
    if (view == null) return;
    Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
  }

  private Observable<TasksIntent.InitialIntent> initialIntent() {
    return Observable.just(TasksIntent.InitialIntent.create());
  }

  private Observable<TasksIntent.RefreshIntent> refreshIntent() {
    //swipeRefreshLayout.setOnRefreshListener(() -> viewModel.loadTasks(false));
    return RxSwipeRefreshLayout.refreshes(swipeRefreshLayout)
        .map(ignored -> TasksIntent.RefreshIntent.create(false))
        .mergeWith(refreshIntentPublisher);
  }

  private Observable<TasksIntent.ClearCompletedTasksIntent> clearCompletedTaskIntent() {
    return clearCompletedTaskIntentPublisher;
  }

  private Observable<TasksIntent.ChangeFilterIntent> changeFilterIntent() {
    return changeFilterIntentPublisher;
  }

  private Observable<TasksIntent> adapterIntents() {
    return listAdapter.getTaskToggleObservable().map(task -> {
      if (!task.isCompleted()) {
        return TasksIntent.CompleteTaskIntent.create(task);
      } else {
        return TasksIntent.ActivateTaskIntent.create(task);
      }
    });
  }

  private void showNoActiveTasks() {
    showNoTasksViews(getResources().getString(R.string.no_tasks_active),
        R.drawable.ic_check_circle_24dp, false);
  }

  private void showNoTasks() {
    showNoTasksViews(getResources().getString(R.string.no_tasks_all),
        R.drawable.ic_assignment_turned_in_24dp, true);
  }

  private void showNoCompletedTasks() {
    showNoTasksViews(getResources().getString(R.string.no_tasks_completed),
        R.drawable.ic_verified_user_24dp, false);
  }

  private void showSuccessfullySavedMessage() {
    showMessage(getString(R.string.successfully_saved_task_message));
  }

  private void showNoTasksViews(String mainText, int iconRes, boolean showAddView) {
    tasksView.setVisibility(View.GONE);
    noTasksView.setVisibility(View.VISIBLE);

    noTaskMainView.setText(mainText);
    noTaskIcon.setImageDrawable(getResources().getDrawable(iconRes));
    noTaskAddView.setVisibility(showAddView ? View.VISIBLE : View.GONE);
  }

  private void showActiveFilterLabel() {
    filteringLabelView.setText(getResources().getString(R.string.label_active));
  }

  private void showCompletedFilterLabel() {
    filteringLabelView.setText(getResources().getString(R.string.label_completed));
  }

  private void showAllFilterLabel() {
    filteringLabelView.setText(getResources().getString(R.string.label_all));
  }

  private void showAddTask() {
    Intent intent = new Intent(getContext(), AddEditTaskActivity.class);
    startActivityForResult(intent, AddEditTaskActivity.REQUEST_ADD_TASK);
  }

  private void showTaskDetailsUi(String taskId) {
    // in it's own Activity, since it makes more sense that way and it gives us the flexibility
    // to show some MviIntent stubbing.
    Intent intent = new Intent(getContext(), TaskDetailActivity.class);
    intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, taskId);
    startActivity(intent);
  }

  private void showLoadingTasksError() {
    showMessage(getString(R.string.loading_tasks_error));
  }
}
