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

package com.example.android.architecture.blueprints.todoapp.addedittask;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.architecture.blueprints.todoapp.R;
import com.example.android.architecture.blueprints.todoapp.mvibase.MviView;
import com.example.android.architecture.blueprints.todoapp.util.ToDoViewModelFactory;
import com.jakewharton.rxbinding2.view.RxView;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Main UI for the add task screen. Users can enter a task title and description.
 */
public class AddEditTaskFragment extends Fragment implements MviView<AddEditTaskIntent, AddEditTaskViewState> {
    public static final String ARGUMENT_EDIT_TASK_ID = "EDIT_TASK_ID";
    private TextView mTitle;
    private TextView mDescription;
    private FloatingActionButton fab;
    private AddEditTaskViewModel mViewModel;
    private CompositeDisposable mDisposables = new CompositeDisposable();

    public static AddEditTaskFragment newInstance() {
        return new AddEditTaskFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
//        mPresenter.subscribe();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mDisposables.dispose();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.addtask_frag, container, false);
        mTitle = (TextView) root.findViewById(R.id.add_task_title);
        mDescription = (TextView) root.findViewById(R.id.add_task_description);
        setHasOptionsMenu(true);
        return root;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fab = (FloatingActionButton) getActivity().findViewById(R.id.fab_edit_task_done);
        fab.setImageResource(R.drawable.ic_done);

        mViewModel = ViewModelProviders.of(this, ToDoViewModelFactory.getInstance(getContext()))
                .get(AddEditTaskViewModel.class);
        mDisposables = new CompositeDisposable();
        bind();
    }

    private void bind() {
        mDisposables.add(mViewModel.states().subscribe(this::render));
        mViewModel.processIntents(intents());
    }

    @Override
    public Observable<AddEditTaskIntent> intents() {
        return Observable.merge(initialIntent(), saveTaskIntent());
    }

    private Observable<AddEditTaskIntent.InitialIntent> initialIntent() {
        return Observable.just(AddEditTaskIntent.InitialIntent.create(getArgumentTaskId()));
    }

    private Observable<AddEditTaskIntent.SaveTask> saveTaskIntent() {
        return RxView.clicks(fab).map(ignored ->
                AddEditTaskIntent.SaveTask.create(
                        getArgumentTaskId(),
                        mTitle.getText().toString(),
                        mDescription.getText().toString()));
    }

    @Override
    public void render(AddEditTaskViewState state) {
        if (state.isSaved()) {
            showTasksList();
            return;
        }
        if (state.isEmpty()) {
            showEmptyTaskError();
        }
        if (!state.title().isEmpty()) {
            setTitle(state.title());
        }
        if (!state.description().isEmpty()) {
            setDescription(state.description());
        }
    }

    private void showEmptyTaskError() {
        Snackbar.make(mTitle, getString(R.string.empty_task_message), Snackbar.LENGTH_LONG).show();
    }

    private void showTasksList() {
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }

    private void setTitle(String title) {
        mTitle.setText(title);
    }

    private void setDescription(String description) {
        mDescription.setText(description);
    }

    @Nullable
    private String getArgumentTaskId() {
        Bundle args = getArguments();
        if (args == null) return null;
        return args.getString(ARGUMENT_EDIT_TASK_ID);
    }
}
