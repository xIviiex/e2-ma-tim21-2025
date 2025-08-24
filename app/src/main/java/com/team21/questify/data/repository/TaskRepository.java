package com.team21.questify.data.repository;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.team21.questify.application.model.Task;
import com.team21.questify.application.model.TaskOccurrence;
import com.team21.questify.application.model.enums.TaskDifficulty;
import com.team21.questify.data.database.DatabaseHelper;
import com.team21.questify.data.database.TaskLocalDataSource;
import com.team21.questify.data.firebase.TaskRemoteDataSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public Map<String, Integer> getCompletedTaskCountsByCategory(String userId) {
        return localDataSource.getCompletedTaskCountsByCategory(userId);
    }

    public Map<Long, TaskDifficulty> getCompletedTaskDifficultiesWithDates(String userId) {
        Map<Long, String> difficultyStrings = localDataSource.getCompletedTaskDifficultiesWithDates(userId);
        Map<Long, TaskDifficulty> difficulties = new HashMap<>();

        for (Map.Entry<Long, String> entry : difficultyStrings.entrySet()) {
            try {
                difficulties.put(entry.getKey(), TaskDifficulty.valueOf(entry.getValue()));
            } catch (IllegalArgumentException e) {
                Log.e("TaskRepository", "Unknown task difficulty: " + entry.getValue());
            }
        }
        return difficulties;
    }

    public Map<String, Integer> getWeeklyXp(String userId) {
        return localDataSource.getWeeklyXp(userId);
    }
}

