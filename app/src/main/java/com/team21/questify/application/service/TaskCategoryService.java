package com.team21.questify.application.service;

import android.content.Context;
import com.google.android.gms.tasks.OnCompleteListener;
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
        if (user != null) {
            String userId = user.getUid();
            String categoryId = UUID.randomUUID().toString(); // Generiše jedinstveni ID
            TaskCategory newCategory = new TaskCategory(categoryId, userId, name, hexColor);
            repository.createCategory(newCategory, listener);
        } else {
            // Rukovanje situacijom kada korisnik nije prijavljen
            // Možda treba prikazati grešku ili preusmeriti korisnika na prijavu.
            // Za sada, samo vraćamo null ili bacamo izuzetak.
            listener.onComplete(null);
        }
    }

    /**
     * Dohvata sve kategorije za trenutno prijavljenog korisnika.
     *
     * @param listener Slušalac za obaveštenje o završetku operacije.
     */
    public void getAllCategoriesForUser(OnCompleteListener<List<TaskCategory>> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            repository.getAllCategoriesForUser(user.getUid(), listener);
        } else {
            // Rukovanje situacijom kada korisnik nije prijavljen.
            listener.onComplete(null);
        }
    }


}
