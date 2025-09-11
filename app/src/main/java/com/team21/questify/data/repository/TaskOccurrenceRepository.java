package com.team21.questify.data.repository;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.team21.questify.application.model.TaskOccurrence;
import com.team21.questify.application.model.enums.TaskStatus;
import com.team21.questify.data.database.DatabaseHelper;
import com.team21.questify.data.database.TaskOccurrenceLocalDataSource;
import com.team21.questify.data.firebase.TaskOccurrenceRemoteDataSource;


import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TaskOccurrenceRepository {
    private final TaskOccurrenceLocalDataSource localDataSource;
    private final TaskOccurrenceRemoteDataSource remoteDataSource;
    private final DatabaseHelper dbHelper;
    private final Executor executor;


    public TaskOccurrenceRepository(Context context) {
        this.localDataSource = new TaskOccurrenceLocalDataSource(context);
        this.remoteDataSource = new TaskOccurrenceRemoteDataSource();
        this.dbHelper = new DatabaseHelper(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void createOccurrence(TaskOccurrence occurrence, OnCompleteListener<Void> listener) {


        // Unos u udaljenu bazu
        remoteDataSource.insertOccurrence(occurrence, taskRemote -> {
            if (!taskRemote.isSuccessful()) {
                Log.e("TaskOccurrenceRepo", "Failed to insert occurrence to remote db: " + taskRemote.getException());

            }
            // Unos u lokalnu bazu
            localDataSource.insertTaskOccurrence(occurrence);
            listener.onComplete(taskRemote);
        });
    }


    public void getAllOccurrencesForUser(String userId, OnCompleteListener<List<TaskOccurrence>> listener) {
        List<TaskOccurrence> localOccurrences = localDataSource.getAllOccurrencesForUser(userId);

        remoteDataSource.getAllOccurrencesForUser(userId, taskRemote -> {
            if (taskRemote.isSuccessful() && taskRemote.getResult() != null) {
                List<TaskOccurrence> remoteOccurrences = new ArrayList<>();
                for (QueryDocumentSnapshot document : taskRemote.getResult()) {
                    TaskOccurrence occurrence = document.toObject(TaskOccurrence.class);
                    remoteOccurrences.add(occurrence);
                    localDataSource.insertTaskOccurrence(occurrence);
                }
                listener.onComplete(Tasks.forResult(remoteOccurrences));
            } else {
                // Ako Firestore nije uspeo, koristi lokalne ako ih ima
                if (!localOccurrences.isEmpty()) {
                    listener.onComplete(Tasks.forResult(localOccurrences));
                } else {
                    listener.onComplete(Tasks.forException(taskRemote.getException()));
                }
            }
        });
    }



    public Map<TaskStatus, Integer> getTaskCountsByStatus(String userId) {
        Map<String, Integer> stringCounts = localDataSource.getTaskCountsByStatus(userId);
        Map<TaskStatus, Integer> enumCounts = new HashMap<>();

        for (Map.Entry<String, Integer> entry : stringCounts.entrySet()) {
            try {
                TaskStatus status = TaskStatus.valueOf(entry.getKey());
                enumCounts.put(status, entry.getValue());
            } catch (IllegalArgumentException e) {
                Log.e("TaskOccurrenceRepository", "Unknown status: " + entry.getKey());
            }
        }

        return enumCounts;
    }

    public Task<List<TaskOccurrence>> getTaskOccurrencesByUserIdSortedByDate(String userId) {
        return remoteDataSource.getOccurrencesForUser(userId)
                .onSuccessTask(querySnapshot ->
                        Tasks.call(executor, () -> {
                            List<TaskOccurrence> remoteOccurrences = querySnapshot.toObjects(TaskOccurrence.class);
                            localDataSource.replaceAllForUser(userId, remoteOccurrences);
                            return localDataSource.getTaskOccurrencesByUserIdSortedByDate(userId);
                        })
                );
    }



    public void getOccurrencesByTaskId(String taskId, OnCompleteListener<List<TaskOccurrence>> listener) {
        remoteDataSource.getOccurrencesByTaskId(taskId, taskRemote -> {
            if (taskRemote.isSuccessful() && taskRemote.getResult() != null) {
                List<TaskOccurrence> remoteOccurrences = taskRemote.getResult().toObjects(TaskOccurrence.class);
                listener.onComplete(Tasks.forResult(remoteOccurrences));
            } else {
                // Fallback na lokalnu bazu ako remote ne uspe
                executor.execute(() -> {
                    List<TaskOccurrence> localOccurrences = localDataSource.getOccurrencesByTaskId(taskId);
                    listener.onComplete(Tasks.forResult(localOccurrences));
                });
            }
        });
    }

    public void findFutureOccurrences(String taskId, long fromDate, OnCompleteListener<List<TaskOccurrence>> listener) {
        remoteDataSource.findFutureOccurrences(taskId, fromDate, taskRemote -> {
            if (taskRemote.isSuccessful() && taskRemote.getResult() != null) {
                List<TaskOccurrence> allFutureOccurrences = taskRemote.getResult().toObjects(TaskOccurrence.class);

                // Dodatno filtriranje na klijentu
                List<TaskOccurrence> uncompletedFutureOccurrences = allFutureOccurrences.stream()
                        .filter(occ -> occ.getStatus() != TaskStatus.COMPLETED)
                        .collect(Collectors.toList());

                listener.onComplete(Tasks.forResult(uncompletedFutureOccurrences));
            } else {
                // Ako remote ne uspe, prosledi grešku
                listener.onComplete(Tasks.forException(taskRemote.getException()));
            }
        });
    }

    public void updateOccurrenceTaskId(String occurrenceId, String newTaskId, OnCompleteListener<Void> listener) {
        // Prvo ažuriraj remote bazu
        remoteDataSource.updateOccurrenceTaskId(occurrenceId, newTaskId, taskRemote -> {
            if (taskRemote.isSuccessful()) {
                // Ako je remote uspeo, ažuriraj i lokalnu
                executor.execute(() -> localDataSource.updateTaskOccurrenceTaskId(occurrenceId, newTaskId));
            }
            // Prosledi rezultat listeneru bez obzira na ishod lokalne operacije
            listener.onComplete(taskRemote);
        });
    }


}