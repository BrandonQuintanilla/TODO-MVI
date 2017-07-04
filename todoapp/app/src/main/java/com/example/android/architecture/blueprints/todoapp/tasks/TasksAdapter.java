package com.example.android.architecture.blueprints.todoapp.tasks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import com.example.android.architecture.blueprints.todoapp.R;
import com.example.android.architecture.blueprints.todoapp.data.Task;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class TasksAdapter extends BaseAdapter {

  private PublishSubject<Task> taskClickObservable = PublishSubject.create();
  private PublishSubject<Task> taskToggleObservable = PublishSubject.create();
  private List<Task> tasks;

  public TasksAdapter(List<Task> tasks) {
    setList(tasks);
  }

  public void replaceData(List<Task> tasks) {
    setList(tasks);
    notifyDataSetChanged();
  }

  Observable<Task> getTaskClickObservable() {
    return taskClickObservable;
  }

  Observable<Task> getTaskToggleObservable() {
    return taskToggleObservable;
  }

  private void setList(List<Task> tasks) {
    this.tasks = checkNotNull(tasks);
  }

  @Override public int getCount() {
    return tasks.size();
  }

  @Override public Task getItem(int position) {
    return tasks.get(position);
  }

  @Override public long getItemId(int position) {
    return position;
  }

  @Override public View getView(int position, View view, ViewGroup viewGroup) {
    View rowView = view;
    if (rowView == null) {
      LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
      rowView = inflater.inflate(R.layout.task_item, viewGroup, false);
    }

    final Task task = getItem(position);

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

    completeCB.setOnClickListener(ignored -> taskToggleObservable.onNext(task));

    rowView.setOnClickListener(ignored -> taskClickObservable.onNext(task));

    return rowView;
  }
}
