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

import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.LifecycleRegistryOwner;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.example.android.architecture.blueprints.todoapp.R;
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskActivity;
import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailActivity;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Display a grid of {@link Task}s. User can choose to view all, active or completed tasks.
 */
public class TasksFragment extends Fragment implements LifecycleRegistryOwner {
  LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(this);

  private TasksPresenter presenter;
  private TasksAdapter listAdapter;
  private View noTasksView;
  private ImageView noTaskIcon;
  private TextView noTaskMainView;
  private TextView noTaskAddView;
  private LinearLayout tasksView;
  private TextView filteringLabelView;

  public TasksFragment() {
    // Requires empty public constructor
  }

  public static TasksFragment newInstance() {
    return new TasksFragment();
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    listAdapter = new TasksAdapter(new ArrayList<>(0), mItemListener);
  }

  @Override public void onResume() {
    super.onResume();
    presenter.subscribe();
  }

  @Override public void onPause() {
    super.onPause();
    presenter.unsubscribe();
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    presenter.result(requestCode, resultCode);
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
    noTaskAddView.setOnClickListener(__ -> showAddTask());

    // Set up floating action button
    FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab_add_task);

    fab.setImageResource(R.drawable.ic_add);
    fab.setOnClickListener(__ -> presenter.addNewTask());

    // Set up progress indicator
    final ScrollChildSwipeRefreshLayout swipeRefreshLayout =
        (ScrollChildSwipeRefreshLayout) root.findViewById(R.id.refresh_layout);
    swipeRefreshLayout.setColorSchemeColors(
        ContextCompat.getColor(getActivity(), R.color.colorPrimary),
        ContextCompat.getColor(getActivity(), R.color.colorAccent),
        ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark));
    // Set the scrolling view in the custom SwipeRefreshLayout.
    swipeRefreshLayout.setScrollUpChild(listView);

    swipeRefreshLayout.setOnRefreshListener(() -> presenter.loadTasks(false));

    setHasOptionsMenu(true);

    return root;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_clear:
        presenter.clearCompletedTasks();
        break;
      case R.id.menu_filter:
        showFilteringPopUpMenu();
        break;
      case R.id.menu_refresh:
        presenter.loadTasks(true);
        break;
    }
    return true;
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.tasks_fragment_menu, menu);
    super.onCreateOptionsMenu(menu, inflater);
  }

  public void showFilteringPopUpMenu() {
    PopupMenu popup = new PopupMenu(getContext(), getActivity().findViewById(R.id.menu_filter));
    popup.getMenuInflater().inflate(R.menu.filter_tasks, popup.getMenu());

    popup.setOnMenuItemClickListener(item -> {
      switch (item.getItemId()) {
        case R.id.active:
          presenter.setFiltering(TasksFilterType.ACTIVE_TASKS);
          break;
        case R.id.completed:
          presenter.setFiltering(TasksFilterType.COMPLETED_TASKS);
          break;
        default:
          presenter.setFiltering(TasksFilterType.ALL_TASKS);
          break;
      }
      presenter.loadTasks(false);
      return true;
    });

    popup.show();
  }

  /**
   * Listener for clicks on tasks in the ListView.
   */
  TaskItemListener mItemListener = new TaskItemListener() {
    @Override public void onTaskClick(Task clickedTask) {
      presenter.openTaskDetails(clickedTask);
    }

    @Override public void onCompleteTaskClick(Task completedTask) {
      presenter.completeTask(completedTask);
    }

    @Override public void onActivateTaskClick(Task activatedTask) {
      presenter.activateTask(activatedTask);
    }
  };

  public void setLoadingIndicator(final boolean active) {

    if (getView() == null) {
      return;
    }
    final SwipeRefreshLayout srl = (SwipeRefreshLayout) getView().findViewById(R.id.refresh_layout);

    // Make sure setRefreshing() is called after the layout is done with everything else.
    srl.post(() -> srl.setRefreshing(active));
  }

  public void showTasks(List<Task> tasks) {
    listAdapter.replaceData(tasks);

    tasksView.setVisibility(View.VISIBLE);
    noTasksView.setVisibility(View.GONE);
  }

  public void showNoActiveTasks() {
    showNoTasksViews(getResources().getString(R.string.no_tasks_active),
        R.drawable.ic_check_circle_24dp, false);
  }

  public void showNoTasks() {
    showNoTasksViews(getResources().getString(R.string.no_tasks_all),
        R.drawable.ic_assignment_turned_in_24dp, false);
  }

  public void showNoCompletedTasks() {
    showNoTasksViews(getResources().getString(R.string.no_tasks_completed),
        R.drawable.ic_verified_user_24dp, false);
  }

  public void showSuccessfullySavedMessage() {
    showMessage(getString(R.string.successfully_saved_task_message));
  }

  private void showNoTasksViews(String mainText, int iconRes, boolean showAddView) {
    tasksView.setVisibility(View.GONE);
    noTasksView.setVisibility(View.VISIBLE);

    noTaskMainView.setText(mainText);
    noTaskIcon.setImageDrawable(getResources().getDrawable(iconRes));
    noTaskAddView.setVisibility(showAddView ? View.VISIBLE : View.GONE);
  }

  public void showActiveFilterLabel() {
    filteringLabelView.setText(getResources().getString(R.string.label_active));
  }

  public void showCompletedFilterLabel() {
    filteringLabelView.setText(getResources().getString(R.string.label_completed));
  }

  public void showAllFilterLabel() {
    filteringLabelView.setText(getResources().getString(R.string.label_all));
  }

  public void showAddTask() {
    Intent intent = new Intent(getContext(), AddEditTaskActivity.class);
    startActivityForResult(intent, AddEditTaskActivity.REQUEST_ADD_TASK);
  }

  public void showTaskDetailsUi(String taskId) {
    // in it's own Activity, since it makes more sense that way and it gives us the flexibility
    // to show some MviIntent stubbing.
    Intent intent = new Intent(getContext(), TaskDetailActivity.class);
    intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, taskId);
    startActivity(intent);
  }

  public void showTaskMarkedComplete() {
    showMessage(getString(R.string.task_marked_complete));
  }

  public void showTaskMarkedActive() {
    showMessage(getString(R.string.task_marked_active));
  }

  public void showCompletedTasksCleared() {
    showMessage(getString(R.string.completed_tasks_cleared));
  }

  public void showLoadingTasksError() {
    showMessage(getString(R.string.loading_tasks_error));
  }

  private void showMessage(String message) {
    Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
  }

  public boolean isActive() {
    return isAdded();
  }

  private static class TasksAdapter extends BaseAdapter {

    private List<Task> mTasks;
    private TaskItemListener mItemListener;

    public TasksAdapter(List<Task> tasks, TaskItemListener itemListener) {
      setList(tasks);
      mItemListener = itemListener;
    }

    public void replaceData(List<Task> tasks) {
      setList(tasks);
      notifyDataSetChanged();
    }

    private void setList(List<Task> tasks) {
      mTasks = checkNotNull(tasks);
    }

    @Override public int getCount() {
      return mTasks.size();
    }

    @Override public Task getItem(int i) {
      return mTasks.get(i);
    }

    @Override public long getItemId(int i) {
      return i;
    }

    @Override public View getView(int i, View view, ViewGroup viewGroup) {
      View rowView = view;
      if (rowView == null) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        rowView = inflater.inflate(R.layout.task_item, viewGroup, false);
      }

      final Task task = getItem(i);

      TextView titleTV = (TextView) rowView.findViewById(R.id.title);
      titleTV.setText(task.getTitleForList());

      CheckBox completeCB = (CheckBox) rowView.findViewById(R.id.complete);

      // Active/completed task UI
      completeCB.setChecked(task.isCompleted());
      if (task.isCompleted()) {
        rowView.setBackgroundDrawable(viewGroup.getContext()
            .getResources()
            .getDrawable(R.drawable.list_completed_touch_feedback));
      } else {
        rowView.setBackgroundDrawable(
            viewGroup.getContext().getResources().getDrawable(R.drawable.touch_feedback));
      }

      completeCB.setOnClickListener(__ -> {
        if (!task.isCompleted()) {
          mItemListener.onCompleteTaskClick(task);
        } else {
          mItemListener.onActivateTaskClick(task);
        }
      });

      rowView.setOnClickListener(__ -> mItemListener.onTaskClick(task));

      return rowView;
    }
  }

  public interface TaskItemListener {

    void onTaskClick(Task clickedTask);

    void onCompleteTaskClick(Task completedTask);

    void onActivateTaskClick(Task activatedTask);
  }

  @Override public LifecycleRegistry getLifecycle() {
    return lifecycleRegistry;
  }
}
