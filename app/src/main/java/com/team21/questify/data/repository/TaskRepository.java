package com.team21.questify.data.repository;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.team21.questify.application.model.Task;
import com.team21.questify.data.database.TaskLocalDataSource;
import com.team21.questify.data.firebase.TaskRemoteDataSource;

import java.util.ArrayList;
import java.util.List;

public class TaskRepository {
    private final TaskLocalDataSource localDataSource;
    private final TaskRemoteDataSource remoteDataSource;

    public TaskRepository(Context context) {
        this.localDataSource = new TaskLocalDataSource(context);
        this.remoteDataSource = new TaskRemoteDataSource();
    }

    public void createTask(Task task, OnCompleteListener<Void> listener) {
        // Pokušaj unosa u udaljenu bazu
        remoteDataSource.insertTask(task, taskRemote -> {
            if (!taskRemote.isSuccessful()) {
                Log.e("TaskRepository", "Failed to insert task to remote db: " + taskRemote.getException());

            }
            localDataSource.insertTask(task);
            listener.onComplete(taskRemote);
        });
    }


    public void getAllTasksForUser(String userId, OnCompleteListener<List<Task>> listener) {
        List<Task> localTasks = localDataSource.getAllTasksForUser(userId);

        remoteDataSource.getAllTasksForUser(userId, taskRemote -> {
            if (taskRemote.isSuccessful() && taskRemote.getResult() != null) {
                List<Task> remoteTasks = new ArrayList<>();
                for (QueryDocumentSnapshot document : taskRemote.getResult()) {
                    Task task = document.toObject(Task.class);
                    remoteTasks.add(task);
                    localDataSource.insertTask(task);
                }
                listener.onComplete(Tasks.forResult(remoteTasks));
            } else {
                if (!localTasks.isEmpty()) {
                    listener.onComplete(Tasks.forResult(localTasks));
                } else {
                    listener.onComplete(Tasks.forException(taskRemote.getException()));
                }
            }
        });
    }

    public void getTaskById(String taskId, OnCompleteListener<Task> listener) {

        Task localTask = localDataSource.getTaskById(taskId);
        if (localTask != null) {

            listener.onComplete(Tasks.forResult(localTask));
            return;
        }

        // 2. If not found locally, query the remote database
        remoteDataSource.getTaskById(taskId, remoteTaskResult -> {
            if (remoteTaskResult.isSuccessful() && remoteTaskResult.getResult() != null && remoteTaskResult.getResult().exists()) {
                // Convert the DocumentSnapshot to a Task object
                Task remoteTask = remoteTaskResult.getResult().toObject(Task.class);

                if (remoteTask != null) {
                    // Insert the fetched task into the local database for future offline access
                    localDataSource.insertTask(remoteTask);
                    listener.onComplete(Tasks.forResult(remoteTask));
                } else {
                    listener.onComplete(Tasks.forException(new Exception("Failed to convert remote data to Task object.")));
                }

            } else {
                // Task not found in remote database, or there was an error
                Exception exception = remoteTaskResult.getException() != null ? remoteTaskResult.getException() : new Exception("Task not found.");
                listener.onComplete(Tasks.forException(exception));
            }
        });
    }

}

