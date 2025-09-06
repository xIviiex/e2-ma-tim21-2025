package com.team21.questify.data.firebase;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
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


    public void getOccurrencesByDateAndStatus(String userId, Long date, String status, OnCompleteListener<QuerySnapshot> listener) {
        db.collection(OCCURRENCES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", date)
                .whereEqualTo("status", status)
                .get()
                .addOnCompleteListener(listener);
    }



    public void updateOccurrence(String occurrenceId, Map<String, Object> updates, OnCompleteListener<Void> listener) {
        db.collection(OCCURRENCES_COLLECTION)
                .document(occurrenceId)
                .update(updates)
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
