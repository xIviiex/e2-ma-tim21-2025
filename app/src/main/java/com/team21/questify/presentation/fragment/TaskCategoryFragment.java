package com.team21.questify.presentation.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.team21.questify.R;
import com.team21.questify.application.model.TaskCategory;
import com.team21.questify.application.service.TaskCategoryService;
import com.team21.questify.data.repository.TaskCategoryRepository;

import java.util.ArrayList;
import java.util.List;


public class TaskCategoryFragment extends Fragment {

    private static final String TAG = "TaskCategoryFragment";

    private Spinner categorySpinner;
    private TaskCategoryService categoryService;
    private FirebaseAuth firebaseAuth;
    private CategorySelectedListener categorySelectedListener;

    public TaskCategoryFragment() {

    }

    public static TaskCategoryFragment newInstance() {
        return new TaskCategoryFragment();
    }

    public interface CategorySelectedListener {
        void onCategorySelected(String categoryId);
    }

    public void setCategorySelectedListener(CategorySelectedListener listener) {
        this.categorySelectedListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inicijalizacija repozitorijuma i Firebase Autentifikacije
        categoryService = new TaskCategoryService(requireContext());
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_task_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        categorySpinner = view.findViewById(R.id.spinnerCategory);
        String userId = firebaseAuth.getUid();
        if (userId == null) {
            Log.e(TAG, "User ID is null. Cannot fetch categories.");
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        categoryService.getAllCategoriesForUser(task -> {
            if (task.isSuccessful()) {
                List<TaskCategory> categories = task.getResult();
                if (categories != null && !categories.isEmpty()) {
                    List<String> categoryNames = new ArrayList<>();
                    List<String> categoryIds = new ArrayList<>();

                    for (TaskCategory category : categories) {
                        categoryNames.add(category.getName());
                        categoryIds.add(category.getId());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_spinner_item, categoryNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    categorySpinner.setAdapter(adapter);


                    if (categorySelectedListener != null) {
                        categorySelectedListener.onCategorySelected(categoryIds.get(0));
                    }

                    categorySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                            if (categorySelectedListener != null) {
                                categorySelectedListener.onCategorySelected(categoryIds.get(position));
                            }
                        }

                        @Override
                        public void onNothingSelected(android.widget.AdapterView<?> parent) {
                            // No action
                        }
                    });

                } else {
                    Log.d(TAG, "No categories found for user.");
                    Toast.makeText(requireContext(), "No categories found.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "Error fetching categories: " + task.getException());
                Toast.makeText(requireContext(), "Failed to load categories.", Toast.LENGTH_SHORT).show();
            }
        });
    }

}