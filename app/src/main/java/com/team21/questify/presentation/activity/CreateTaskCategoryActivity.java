package com.team21.questify.presentation.activity;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.team21.questify.R;
import com.team21.questify.application.service.TaskCategoryService;
import com.team21.questify.presentation.adapter.ColorAdapter;

import java.util.ArrayList;
import java.util.List;

public class CreateTaskCategoryActivity extends AppCompatActivity {

    private TaskCategoryService taskCategoryService;
    private EditText etCategoryName;
    private Button btnCreateCategory;
    private RecyclerView rvColorPicker;
    private String selectedHexColor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_task_category);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        taskCategoryService = new TaskCategoryService(this);

        initViews();
        setupColorPicker();
    }

    private void initViews() {
        etCategoryName = findViewById(R.id.et_category_name);
        btnCreateCategory = findViewById(R.id.btn_create_category);
        rvColorPicker = findViewById(R.id.rv_color_picker);

        btnCreateCategory.setOnClickListener(v -> createCategory());
    }

    private void setupColorPicker() {

        List<String> colorList = new ArrayList<>();
        colorList.add("#F44336"); // Red
        colorList.add("#E91E63"); // Pink
        colorList.add("#9C27B0"); // Purple
        colorList.add("#673AB7"); // Deep Purple
        colorList.add("#3F51B5"); // Indigo
        colorList.add("#2196F3"); // Blue
        colorList.add("#03A9F4"); // Light Blue
        colorList.add("#00BCD4"); // Cyan
        colorList.add("#009688"); // Teal
        colorList.add("#4CAF50"); // Green
        colorList.add("#8BC34A"); // Light Green
        colorList.add("#CDDC39"); // Lime
        colorList.add("#FFEB3B"); // Yellow
        colorList.add("#FFC107"); // Amber
        colorList.add("#FF9800"); // Orange
        colorList.add("#FF5722"); // Deep Orange
        colorList.add("#795548"); // Brown
        colorList.add("#9E9E9E"); // Grey
        colorList.add("#607D8B"); // Blue Grey

        ColorAdapter colorAdapter = new ColorAdapter(colorList, hexColor -> {
            selectedHexColor = hexColor;
        });

        rvColorPicker.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvColorPicker.setAdapter(colorAdapter);
    }

    private void createCategory() {
        String categoryName = etCategoryName.getText().toString().trim();

        if (categoryName.isEmpty()) {
            Toast.makeText(this, "Please choose a category name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedHexColor == null) {
            Toast.makeText(this, "Please choose a color.", Toast.LENGTH_SHORT).show();
            return;
        }

        taskCategoryService.createCategory(categoryName, selectedHexColor, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Category created!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Category creating failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
