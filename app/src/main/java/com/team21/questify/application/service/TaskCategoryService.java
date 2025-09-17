package com.team21.questify.application.service;

import android.content.Context;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.team21.questify.application.model.TaskCategory;
import com.team21.questify.data.repository.TaskCategoryRepository;

import java.util.List;
import java.util.UUID;

public class TaskCategoryService {
    private final TaskCategoryRepository repository;
    private final FirebaseAuth auth;

    public TaskCategoryService(Context context) {
        this.repository = new TaskCategoryRepository(context);
        this.auth = FirebaseAuth.getInstance();
    }


    public void createCategory(String name, String hexColor, OnCompleteListener<Void> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onComplete(Tasks.forException(new Exception("User not authenticated")));
            return;
        }
        String userId = user.getUid();


        getAllCategoriesForUser(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<TaskCategory> existingCategories = task.getResult();


                boolean colorExists = false;
                for (TaskCategory category : existingCategories) {

                    if (category.getHexColor().equalsIgnoreCase(hexColor)) {
                        colorExists = true;
                        break; // Nema potrebe da tražimo dalje
                    }
                }


                if (colorExists) {
                    listener.onComplete(Tasks.forException(new Exception("This color is already in use. Please select another one.")));
                } else {

                    String categoryId = UUID.randomUUID().toString();
                    TaskCategory newCategory = new TaskCategory(categoryId, userId, name, hexColor);
                    repository.createCategory(newCategory, listener);
                }
            } else {
                // Ako dohvatanje kategorija nije uspelo, vrati tu grešku
                listener.onComplete(Tasks.forException(task.getException()));
            }
        });
    }


    public void getAllCategoriesForUser(OnCompleteListener<List<TaskCategory>> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            repository.getAllCategoriesForUser(user.getUid(), listener);
        } else {
            // Rukovanje situacijom kada korisnik nije prijavljen.
            listener.onComplete(null);
        }
    }

    public void getCategoryById(String categoryId, OnCompleteListener<TaskCategory> listener) {
        repository.getCategoryById(categoryId, listener);
    }


    public void updateCategory(TaskCategory updatedCategory, OnCompleteListener<Void> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            if (listener != null) listener.onComplete(null);
            return;
        }

        // Provera da li je boja već zauzeta
        repository.getAllCategoriesForUser(user.getUid(), task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<TaskCategory> categories = task.getResult();
                boolean colorTaken = categories.stream()
                        .anyMatch(cat -> !cat.getId().equals(updatedCategory.getId()) &&
                                cat.getHexColor().equalsIgnoreCase(updatedCategory.getHexColor()));

                if (colorTaken) {
                    // Boja već postoji
                    if (listener != null) listener.onComplete(null);
                } else {
                    // Boja slobodna → update
                    repository.updateCategory(updatedCategory, listener);

                    // Ako čuvaš boju i u Task dokumentu → ovde pozvati TaskService.updateTasksWithCategoryColor(...)
                }
            } else {
                if (listener != null) listener.onComplete(null);
            }
        });
    }



}
