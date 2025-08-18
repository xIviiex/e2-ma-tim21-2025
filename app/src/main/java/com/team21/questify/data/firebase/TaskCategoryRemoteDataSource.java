package com.team21.questify.data.firebase;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.team21.questify.application.model.TaskCategory;

public class TaskCategoryRemoteDataSource {
    private final FirebaseFirestore db;
    private static final String CATEGORIES_COLLECTION = "task_categories";

    public TaskCategoryRemoteDataSource() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void insertCategory(TaskCategory category, OnCompleteListener<Void> listener) {
        if (category.getId() != null) {
            db.collection(CATEGORIES_COLLECTION)
                    .document(category.getId())
                    .set(category)
                    .addOnCompleteListener(listener);
        }
    }


    public void getAllCategoriesForUser(String userId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection(CATEGORIES_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(listener);
    }


    public void updateCategoryColor(String categoryId, String hexColor, OnCompleteListener<Void> listener) {
        db.collection(CATEGORIES_COLLECTION)
                .document(categoryId)
                .update("hexColor", hexColor)
                .addOnCompleteListener(listener);
    }


    public void deleteCategory(String categoryId, OnCompleteListener<Void> listener) {
        db.collection(CATEGORIES_COLLECTION)
                .document(categoryId)
                .delete()
                .addOnCompleteListener(listener);
    }
}

