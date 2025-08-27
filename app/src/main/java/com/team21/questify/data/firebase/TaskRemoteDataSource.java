package com.team21.questify.data.firebase;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.team21.questify.application.model.Task;

import java.util.Map;

public class TaskRemoteDataSource {

    private static final String TASKS_COLLECTION = "tasks";
    private final FirebaseFirestore db;

    public TaskRemoteDataSource() {
        this.db = FirebaseFirestore.getInstance();
    }


    public void insertTask(Task task, OnCompleteListener<Void> listener) {
        if (task.getId() != null) {
            db.collection(TASKS_COLLECTION)
                    .document(task.getId())
                    .set(task)
                    .addOnCompleteListener(listener);
        }
    }


    public void getAllTasksForUser(String userId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection(TASKS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(listener);
    }

    public void getTaskById(String taskId, OnCompleteListener<DocumentSnapshot> listener) {
        db.collection(TASKS_COLLECTION)
                .document(taskId)
                .get()
                .addOnCompleteListener(listener);
    }


    public void updateTask(String taskId, Map<String, Object> updates, OnCompleteListener<Void> listener) {
        db.collection(TASKS_COLLECTION)
                .document(taskId)
                .update(updates)
                .addOnCompleteListener(listener);
    }


    public void deleteTask(String taskId, OnCompleteListener<Void> listener) {
        db.collection(TASKS_COLLECTION)
                .document(taskId)
                .delete()
                .addOnCompleteListener(listener);
    }
}
