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

        localDataSource.insertCategory(category);
        remoteDataSource.insertCategory(category, task -> {
            if (!task.isSuccessful()) {
                Log.e("TaskCategoryRepository", "Failed to insert category to remote db: " + task.getException());
            }
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


}
