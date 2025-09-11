package com.team21.questify.data.firebase;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.team21.questify.application.model.TaskOccurrence;

import java.util.Map;


public class TaskOccurrenceRemoteDataSource {

    private static final String OCCURRENCES_COLLECTION = "task_occurrences";
    private final FirebaseFirestore db;

    public TaskOccurrenceRemoteDataSource() {
        this.db = FirebaseFirestore.getInstance();
    }


    public void insertOccurrence(TaskOccurrence occurrence, OnCompleteListener<Void> listener) {
        if (occurrence.getId() != null) {
            db.collection(OCCURRENCES_COLLECTION)
                    .document(occurrence.getId())
                    .set(occurrence)
                    .addOnCompleteListener(listener);
        }
    }


    public void getAllOccurrencesForUser(String userId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection(OCCURRENCES_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(listener);
    }


    public void getOccurrencesByTaskId(String taskId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection(OCCURRENCES_COLLECTION)
                .whereEqualTo("taskId", taskId)
                .orderBy("date", Query.Direction.ASCENDING) // Sortirano po datumu
                .get()
                .addOnCompleteListener(listener);
    }

    public void findFutureOccurrences(String taskId, long fromDate, OnCompleteListener<QuerySnapshot> listener) {
        db.collection(OCCURRENCES_COLLECTION)
                .whereEqualTo("taskId", taskId)
                .whereGreaterThanOrEqualTo("date", fromDate)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(listener);
    }



    public void updateOccurrence(String occurrenceId, Map<String, Object> updates, OnCompleteListener<Void> listener) {
        db.collection(OCCURRENCES_COLLECTION)
                .document(occurrenceId)
                .update(updates)
                .addOnCompleteListener(listener);
    }


    public void updateOccurrenceTaskId(String occurrenceId, String newTaskId, OnCompleteListener<Void> listener) {
        db.collection(OCCURRENCES_COLLECTION)
                .document(occurrenceId)
                .update("taskId", newTaskId)
                .addOnCompleteListener(listener);
    }


    public void deleteOccurrence(String occurrenceId, OnCompleteListener<Void> listener) {
        db.collection(OCCURRENCES_COLLECTION)
                .document(occurrenceId)
                .delete()
                .addOnCompleteListener(listener);
    }

    public Task<QuerySnapshot> getOccurrencesForUser(String userId) {
        return db.collection("task_occurrences")
                .whereEqualTo("userId", userId)
                .get();
    }
}
