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
        // Kreiranje liste hex boja koje se prikazuju
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
            Toast.makeText(this, "Molimo unesite ime kategorije.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedHexColor == null) {
            Toast.makeText(this, "Molimo odaberite boju.", Toast.LENGTH_SHORT).show();
            return;
        }

        taskCategoryService.createCategory(categoryName, selectedHexColor, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Kategorija je uspešno kreirana!", Toast.LENGTH_SHORT).show();
                finish(); // Zatvara aktivnost i vraća se na prethodnu
            } else {
                Toast.makeText(this, "Neuspešno kreiranje kategorije: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Unutrašnje klase za Adapter i ViewHolder za RecyclerView
    private class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ColorViewHolder> {

        private final List<String> colors;
        private final OnColorSelectedListener listener;
        private int selectedPosition = RecyclerView.NO_POSITION;

        public ColorAdapter(List<String> colors, OnColorSelectedListener listener) {
            this.colors = colors;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_color_circle, parent, false);
            return new ColorViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
            String hexColor = colors.get(position);
            holder.bind(hexColor, position == selectedPosition);
        }

        @Override
        public int getItemCount() {
            return colors.size();
        }

        public class ColorViewHolder extends RecyclerView.ViewHolder {
            private final View colorCircle;
            private final View selectionBorder;

            public ColorViewHolder(@NonNull View itemView) {
                super(itemView);
                colorCircle = itemView.findViewById(R.id.color_circle);
                selectionBorder = itemView.findViewById(R.id.selection_border);
                itemView.setOnClickListener(v -> {
                    int adapterPosition = getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        int oldPosition = selectedPosition;
                        selectedPosition = adapterPosition;

                        if (oldPosition != RecyclerView.NO_POSITION) {
                            notifyItemChanged(oldPosition);
                        }
                        notifyItemChanged(selectedPosition);

                        listener.onColorSelected(colors.get(selectedPosition));
                    }
                });
            }

            public void bind(String hexColor, boolean isSelected) {
                // Postavljanje boje kruga direktno na drawable
                GradientDrawable background = (GradientDrawable) colorCircle.getBackground();
                if (background != null) {
                    background.setColor(Color.parseColor(hexColor));
                }
                selectionBorder.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            }
        }
    }

    // Interfejs za slušanje odabira boje
    private interface OnColorSelectedListener {
        void onColorSelected(String hexColor);
    }
}
