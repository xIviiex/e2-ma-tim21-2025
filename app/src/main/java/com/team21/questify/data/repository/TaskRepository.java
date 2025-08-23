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

    /*
    public void getAllTasksForUser(String userId, OnCompleteListener<List<Task>> listener) {
        List<Task> localTasks = localDataSource.getAllTasksForUser(userId);
        if (!localTasks.isEmpty()) {
            // Ako postoje lokalni podaci, odmah ih vrati
            listener.onComplete(Tasks.forResult(localTasks));
        }

        // Uvek pokušaj preuzimanje sa Firebase-a radi sinhronizacije
        remoteDataSource.getAllTasksForUser(userId, taskRemote -> {
            if (taskRemote.isSuccessful()) {
                List<Task> remoteTasks = new ArrayList<>();
                for (QueryDocumentSnapshot document : taskRemote.getResult()) {
                    Task task = document.toObject(Task.class);
                    remoteTasks.add(task);
                    // Sinhronizacija u lokalnu bazu
                    localDataSource.insertTask(task);
                }
                // Vrati ažurirane podatke sa Firebase-a
                listener.onComplete(Tasks.forResult(remoteTasks));
            } else {
                // Ako Firebase operacija ne uspe i nema lokalnih podataka, obavesti o grešci
                if (localTasks.isEmpty()) {
                    listener.onComplete(Tasks.forException(taskRemote.getException()));
                }
            }
        });
    }*/
}

