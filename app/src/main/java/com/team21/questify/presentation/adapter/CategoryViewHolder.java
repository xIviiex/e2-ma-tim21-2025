package com.team21.questify.presentation.adapter; // Prilagodite Vašem paketu

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.team21.questify.R;
import com.team21.questify.application.model.TaskCategory;
import com.team21.questify.presentation.adapter.CategoryAdapter;

public class CategoryViewHolder extends RecyclerView.ViewHolder {


    private final View viewCategoryColor;
    private final TextView textViewCategoryName;
    private final ImageButton imageButtonEditCategory;

    private TaskCategory currentCategory;

    public CategoryViewHolder(@NonNull View itemView, CategoryAdapter.OnCategoryEditClickListener editClickListener) {
        super(itemView);


        viewCategoryColor = itemView.findViewById(R.id.viewCategoryColor);
        textViewCategoryName = itemView.findViewById(R.id.textViewCategoryName);
        imageButtonEditCategory = itemView.findViewById(R.id.imageButtonEditCategory);


        imageButtonEditCategory.setOnClickListener(v -> {
            if (editClickListener != null && currentCategory != null) {
                editClickListener.onEditClick(currentCategory);
            }
        });
    }


    public void bind(TaskCategory category) {
        this.currentCategory = category;

        textViewCategoryName.setText(category.getName());


        if (viewCategoryColor.getBackground() instanceof GradientDrawable) {
            GradientDrawable background = (GradientDrawable) viewCategoryColor.getBackground();
            try {

                background.setColor(Color.parseColor(category.getHexColor()));
            } catch (Exception e) {

                background.setColor(Color.GRAY);
            }
        }
    }
}