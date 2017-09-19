/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.architecture.blueprints.todoapp.taskdetail;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.example.android.architecture.blueprints.todoapp.R;
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskActivity;
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskFragment;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviView;
import com.example.android.architecture.blueprints.todoapp.util.ToDoViewModelFactory;
import com.jakewharton.rxbinding2.view.RxView;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.PublishSubject;

/**
 * Main UI for the task detail screen.
 */
public class TaskDetailFragment extends Fragment implements MviView<TaskDetailIntent, TaskDetailViewState> {

    @NonNull
    private static final String ARGUMENT_TASK_ID = "TASK_ID";

    @NonNull
    private static final int REQUEST_EDIT_TASK = 1;

    private TextView mDetailTitle;
    private TextView mDetailDescription;
    private CheckBox mDetailCompleteStatus;
    private FloatingActionButton fab;

    TaskDetailViewModel mViewModel;

    private CompositeDisposable mDisposables = new CompositeDisposable();
    private PublishSubject<TaskDetailIntent.DeleteTask> mDeleteTaskIntentPublisher = PublishSubject.create();

    public static TaskDetailFragment newInstance(@Nullable String taskId) {
        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_TASK_ID, taskId);
        TaskDetailFragment fragment = new TaskDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.taskdetail_frag, container, false);
        setHasOptionsMenu(true);
        mDetailTitle = (TextView) root.findViewById(R.id.task_detail_title);
        mDetailDescription = (TextView) root.findViewById(R.id.task_detail_description);
        mDetailCompleteStatus = (CheckBox) root.findViewById(R.id.task_detail_complete);

        // Set up floating action button
        fab = (FloatingActionButton) getActivity().findViewById(R.id.fab_edit_task);

        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = ViewModelProviders.of(this, ToDoViewModelFactory.getInstance(getContext()))
                .get(TaskDetailViewModel.class);
        mDisposables = new CompositeDisposable();
        bind();
    }

    private void bind() {
        mDisposables.add(mViewModel.states().subscribe(this::render));
        mViewModel.processIntents(intents());

        RxView.clicks(fab).debounce(200, TimeUnit.MILLISECONDS)
                .subscribe(view -> showEditTask(getArgumentTaskId()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mDisposables.dispose();
    }

    @Override
    public Observable<TaskDetailIntent> intents() {
        return Observable.merge(initialIntent(), checkBoxIntents(), deleteIntent());
    }

    private Observable<TaskDetailIntent.InitialIntent> initialIntent() {
        return Observable.just(TaskDetailIntent.InitialIntent.create(getArgumentTaskId()));
    }

    private Observable<TaskDetailIntent> checkBoxIntents() {
        return RxView.clicks(mDetailCompleteStatus).map(
                activated -> {
                    if (mDetailCompleteStatus.isChecked()) {
                        return TaskDetailIntent.CompleteTaskIntent.create(getArgumentTaskId());
                    } else {
                        return TaskDetailIntent.ActivateTaskIntent.create(getArgumentTaskId());
                    }
                }
        );

    }

    private Observable<TaskDetailIntent.DeleteTask> deleteIntent() {
        return mDeleteTaskIntentPublisher;
    }

    @Nullable
    private String getArgumentTaskId() {
        Bundle args = getArguments();
        if (args == null) return null;
        return args.getString(ARGUMENT_TASK_ID);
    }

    @Override
    public void render(TaskDetailViewState state) {

        if (!state.title().isEmpty()) {
            showTitle(state.title());
        } else {
            hideTitle();
        }

        if (!state.description().isEmpty()) {
            showDescription(state.description());
        } else {
            hideDescription();
        }

        showActive(state.active());

        if (state.taskComplete()){
            showTaskMarkedComplete();
        }

        if (state.taskActivated()){
            showTaskMarkedActive();
        }

        if (state.taskDeleted()){
            showTaskDeleted();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_delete:
                mDeleteTaskIntentPublisher.onNext(TaskDetailIntent.DeleteTask.create(getArgumentTaskId()));
                return true;
        }
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.taskdetail_fragment_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EDIT_TASK) {
            // If the task was edited successfully, go back to the list.
            if (resultCode == Activity.RESULT_OK) {
                getActivity().finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setLoadingIndicator(boolean active) {
        if (active) {
            mDetailTitle.setText("");
            mDetailDescription.setText(getString(R.string.loading));
        }
    }

    public void hideDescription() {
        mDetailDescription.setVisibility(View.GONE);
    }

    public void hideTitle() {
        mDetailTitle.setVisibility(View.GONE);
    }

    public void showActive(boolean isActive){
        mDetailCompleteStatus.setChecked(!isActive);
    }

    public void showDescription(@NonNull String description) {
        mDetailDescription.setVisibility(View.VISIBLE);
        mDetailDescription.setText(description);
    }

    private void showEditTask(@NonNull String taskId) {
        Intent intent = new Intent(getContext(), AddEditTaskActivity.class);
        intent.putExtra(AddEditTaskFragment.ARGUMENT_EDIT_TASK_ID, taskId);
        startActivityForResult(intent, REQUEST_EDIT_TASK);
    }

    public void showTaskDeleted() {
        getActivity().finish();
    }

    public void showTaskMarkedComplete() {
        Snackbar.make(getView(), getString(R.string.task_marked_complete), Snackbar.LENGTH_LONG)
                .show();
    }

    public void showTaskMarkedActive() {
        Snackbar.make(getView(), getString(R.string.task_marked_active), Snackbar.LENGTH_LONG)
                .show();
    }

    public void showTitle(@NonNull String title) {
        mDetailTitle.setVisibility(View.VISIBLE);
        mDetailTitle.setText(title);
    }

    public void showMissingTask() {
        mDetailTitle.setText("");
        mDetailDescription.setText(getString(R.string.no_data));
    }

}
