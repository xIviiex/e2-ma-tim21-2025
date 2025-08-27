package com.team21.questify.data.repository;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.team21.questify.application.model.TaskOccurrence;
import com.team21.questify.data.database.TaskOccurrenceLocalDataSource;
import com.team21.questify.data.firebase.TaskOccurrenceRemoteDataSource;

import java.util.ArrayList;
import java.util.List;

public class TaskOccurrenceRepository {
    private final TaskOccurrenceLocalDataSource localDataSource;
    private final TaskOccurrenceRemoteDataSource remoteDataSource;


    public TaskOccurrenceRepository(Context context) {
        this.localDataSource = new TaskOccurrenceLocalDataSource(context);
        this.remoteDataSource = new TaskOccurrenceRemoteDataSource();
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

}