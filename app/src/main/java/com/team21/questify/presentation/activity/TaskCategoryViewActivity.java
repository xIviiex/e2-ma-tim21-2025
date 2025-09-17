package com.team21.questify.presentation.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.team21.questify.R;
import com.team21.questify.application.model.TaskCategory;
import com.team21.questify.application.service.TaskCategoryService;
import com.team21.questify.presentation.adapter.CategoryAdapter;
import com.team21.questify.presentation.fragment.ColorPickerDialogFragment;

import java.util.List;

public class TaskCategoryViewActivity extends AppCompatActivity implements CategoryAdapter.OnCategoryEditClickListener {

    private RecyclerView recyclerView;
    private CategoryAdapter adapter;
    private TaskCategoryService taskCategoryService;
    private FloatingActionButton fabAddCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_task_category_view);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        taskCategoryService = new TaskCategoryService(this);
        setupRecyclerView();
        loadCategories();
        initViews();
    }

    private void initViews() {
        fabAddCategory = findViewById(R.id.fabAddCategory);
        fabAddCategory.setOnClickListener(v -> {

            Intent intent = new Intent(TaskCategoryViewActivity.this, CreateTaskCategoryActivity.class);
            startActivity(intent);
        });
    }
    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewCategories);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        adapter = new CategoryAdapter(this);
        recyclerView.setAdapter(adapter);
    }


    private void loadCategories() {
        taskCategoryService.getAllCategoriesForUser(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<TaskCategory> categories = task.getResult();
                // Važno: Ažuriranje UI-a (liste) mora da se desi na glavnoj (UI) niti.
                runOnUiThread(() -> adapter.setCategories(categories));
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Failed to load categories.", Toast.LENGTH_SHORT).show());
            }
        });
    }


    @Override
    public void onEditClick(TaskCategory category) {

        ColorPickerDialogFragment dialog = new ColorPickerDialogFragment();


        dialog.setColorPickerListener(hexColor -> {

            updateCategoryColor(category, hexColor);
        });


        dialog.show(getSupportFragmentManager(), "ColorPickerDialog");
    }


    private void updateCategoryColor(TaskCategory category, String newColorHex) {

        category.setHexColor(newColorHex);


        taskCategoryService.updateCategory(category, task -> {
            if (task.isSuccessful()) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Category color updated!", Toast.LENGTH_SHORT).show();

                    loadCategories();
                });
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Failed to update color.", Toast.LENGTH_SHORT).show());
            }
        });
    }
}