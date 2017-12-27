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

package com.example.android.architecture.blueprints.todoapp.data;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource;
import com.example.android.architecture.blueprints.todoapp.data.source.local.TasksDbHelper;
import com.example.android.architecture.blueprints.todoapp.data.source.local.TasksLocalDataSource;
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider;
import com.example.android.architecture.blueprints.todoapp.util.schedulers.ImmediateSchedulerProvider;
import io.reactivex.observers.TestObserver;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Integration test for the {@link TasksDataSource}, which uses the {@link TasksDbHelper}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class TasksLocalDataSourceTest {

    private final static String TITLE = "title";

    private final static String TITLE2 = "title2";

    private final static String TITLE3 = "title3";

    private BaseSchedulerProvider mSchedulerProvider;

    private TasksLocalDataSource mLocalDataSource;

    @Before
    public void setup() {
        TasksLocalDataSource.Companion.clearInstance();
        mSchedulerProvider = new ImmediateSchedulerProvider();

        mLocalDataSource = TasksLocalDataSource.Companion.getInstance(InstrumentationRegistry.getTargetContext(),
                mSchedulerProvider);
    }

    @After
    public void cleanUp() {
        mLocalDataSource.deleteAllTasks();
    }

    @Test
    public void testPreConditions() {
        assertNotNull(mLocalDataSource);
    }

    @Test
    public void saveTask_retrievesTask() {
        // Given a new task
        final Task newTask = Task.Companion.invoke(TITLE, "");

        // When saved into the persistent repository
        mLocalDataSource.saveTask(newTask);

        // Then the task can be retrieved from the persistent repository
        TestObserver<Task> testObserver = new TestObserver<>();
        mLocalDataSource.getTask(newTask.getId()).subscribe(testObserver);
        testObserver.assertValue(newTask);
    }

    @Test
    public void completeTask_retrievedTaskIsComplete() {
        // Given a new task in the persistent repository
        final Task newTask = Task.Companion.invoke(TITLE, "");
        mLocalDataSource.saveTask(newTask);

        // When completed in the persistent repository
        mLocalDataSource.completeTask(newTask);

        // Then the task can be retrieved from the persistent repository and is complete
        TestObserver<Task> testObserver = new TestObserver<>();
        mLocalDataSource.getTask(newTask.getId()).subscribe(testObserver);
        testObserver.assertValueCount(1);
        Task result = testObserver.values().get(0);
        assertThat(result.getCompleted(), is(true));
    }

    @Test
    public void activateTask_retrievedTaskIsActive() {
        // Given a new completed task in the persistent repository
        final Task newTask = Task.Companion.invoke(TITLE, "");
        mLocalDataSource.saveTask(newTask);
        mLocalDataSource.completeTask(newTask);

        // When activated in the persistent repository
        mLocalDataSource.activateTask(newTask);

        // Then the task can be retrieved from the persistent repository and is active
        TestObserver<Task> testObserver = new TestObserver<>();
        mLocalDataSource.getTask(newTask.getId()).subscribe(testObserver);
        testObserver.assertValueCount(1);
        Task result = testObserver.values().get(0);
        assertThat(result.getActive(), is(true));
        assertThat(result.getCompleted(), is(false));
    }

    @Test
    public void clearCompletedTask_taskNotRetrievable() {
        // Given 2 new completed tasks and 1 active task in the persistent repository
        final Task newTask1 = Task.Companion.invoke(TITLE, "");
        mLocalDataSource.saveTask(newTask1);
        mLocalDataSource.completeTask(newTask1);
        final Task newTask2 = Task.Companion.invoke(TITLE2, "");
        mLocalDataSource.saveTask(newTask2);
        mLocalDataSource.completeTask(newTask2);
        final Task newTask3 = Task.Companion.invoke(TITLE3, "");
        mLocalDataSource.saveTask(newTask3);

        // When completed tasks are cleared in the repository
        mLocalDataSource.clearCompletedTasks();

        // Then the completed tasks cannot be retrieved and the active one can
        TestObserver<List<Task>> testObserver = new TestObserver<>();
        mLocalDataSource.getTasks().subscribe(testObserver);
        List<Task> result = testObserver.values().get(0);
        assertThat(result, not(hasItems(newTask1, newTask2)));
    }

    @Test
    public void deleteAllTasks_emptyListOfRetrievedTask() {
        // Given a new task in the persistent repository and a mocked callback
        Task newTask = Task.Companion.invoke(TITLE, "");
        mLocalDataSource.saveTask(newTask);

        // When all tasks are deleted
        mLocalDataSource.deleteAllTasks();

        // Then the retrieved tasks is an empty list
        TestObserver<List<Task>> testObserver = new TestObserver<>();
        mLocalDataSource.getTasks().subscribe(testObserver);
        List<Task> result = testObserver.values().get(0);
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void getTasks_retrieveSavedTasks() {
        // Given 2 new tasks in the persistent repository
        final Task newTask1 = Task.Companion.invoke(TITLE, "a");
        mLocalDataSource.saveTask(newTask1);
        final Task newTask2 = Task.Companion.invoke(TITLE, "b");
        mLocalDataSource.saveTask(newTask2);

        // Then the tasks can be retrieved from the persistent repository
        TestObserver<List<Task>> testObserver = new TestObserver<>();
        mLocalDataSource.getTasks().subscribe(testObserver);
        List<Task> result = testObserver.values().get(0);
        assertThat(result, hasItems(newTask1, newTask2));
    }

    @Test
    public void getTask_whenTaskNotSaved() {
        //Given that no task has been saved
        //When querying for a task, null is returned.
        TestObserver<Task> testObserver = new TestObserver<>();
        mLocalDataSource.getTask("1").subscribe(testObserver);
        testObserver.assertEmpty();
    }
}
