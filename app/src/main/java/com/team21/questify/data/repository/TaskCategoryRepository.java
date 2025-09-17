package com.team21.questify.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.team21.questify.application.model.TaskCategory;
import com.team21.questify.data.database.TaskCategoryLocalDataSource;
import com.team21.questify.data.firebase.TaskCategoryRemoteDataSource;

import java.util.ArrayList;
import java.util.List;
public class TaskCategoryRepository {
    private final TaskCategoryLocalDataSource localDataSource;
    private final TaskCategoryRemoteDataSource remoteDataSource;

    public TaskCategoryRepository(Context context) {
        this.localDataSource = new TaskCategoryLocalDataSource(context);
        this.remoteDataSource = new TaskCategoryRemoteDataSource();
    }


    public void createCategory(TaskCategory category, OnCompleteListener<Void> listener) {


        remoteDataSource.insertCategory(category, task -> {
            if (!task.isSuccessful()) {
                Log.e("TaskCategoryRepository", "Failed to insert category to remote db: " + task.getException());

            }
            localDataSource.insertCategory(category);
            listener.onComplete(task);
        });
    }


    public void getAllCategoriesForUser(String userId, OnCompleteListener<List<TaskCategory>> listener) {
        List<TaskCategory> localCategories = localDataSource.getAllCategoriesForUser(userId);
        if (!localCategories.isEmpty()) {
            listener.onComplete(Tasks.forResult(localCategories));
        }

        remoteDataSource.getAllCategoriesForUser(userId, task -> {
            if (task.isSuccessful()) {
                List<TaskCategory> remoteCategories = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    TaskCategory category = document.toObject(TaskCategory.class);
                    remoteCategories.add(category);
                    localDataSource.insertCategory(category); // Sinhronizacija u lokalnu bazu
                }
                listener.onComplete(Tasks.forResult(remoteCategories));
            } else {
                if (localCategories.isEmpty()) {
                    listener.onComplete(Tasks.forException(task.getException()));
                }
            }
        });
    }


    public void getCategoryById(String categoryId, OnCompleteListener<TaskCategory> listener) {
        TaskCategory localCategory = localDataSource.getCategoryById(categoryId);
        if (localCategory != null) {
            listener.onComplete(Tasks.forResult(localCategory));
            return;
        }

        remoteDataSource.getCategoryById(categoryId, task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                TaskCategory remoteCategory = task.getResult().toObject(TaskCategory.class);
                if (remoteCategory != null) {
                    localDataSource.insertCategory(remoteCategory);
                    listener.onComplete(Tasks.forResult(remoteCategory));
                } else {
                    listener.onComplete(Tasks.forException(new Exception("Failed to convert document to TaskCategory.")));
                }
            } else {
                listener.onComplete(Tasks.forException(task.getException() != null ? task.getException() : new Exception("Category not found.")));
            }
        });
    }


    public void updateCategory(TaskCategory category, OnCompleteListener<Void> listener) {
        // Prvo update u remote
        remoteDataSource.updateCategory(category, task -> {
            if (task.isSuccessful()) {
                // Ako remote uspe, update-ujemo i lokalno
                localDataSource.updateCategory(category);
                listener.onComplete(task);
            } else {
                Log.e("TaskCategoryRepository", "Failed to update category in remote db: " + task.getException());
                // I dalje ažuriraj lokalno (možeš i da preskočiš ako hoćeš striktno remote-first logiku)
                localDataSource.updateCategory(category);
                listener.onComplete(task);
            }
        });
    }



}
