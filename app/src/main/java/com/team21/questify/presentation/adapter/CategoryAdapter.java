package com.team21.questify.presentation.adapter; // Prilagodite Vašem paketu

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.team21.questify.R;
import com.team21.questify.application.model.TaskCategory;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryViewHolder> {

    private List<TaskCategory> categories = new ArrayList<>();
    private final OnCategoryEditClickListener editClickListener;


    public interface OnCategoryEditClickListener {
        void onEditClick(TaskCategory category);
    }

    public CategoryAdapter(OnCategoryEditClickListener listener) {
        this.editClickListener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_category, parent, false);

        return new CategoryViewHolder(view, editClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {

        TaskCategory currentCategory = categories.get(position);
        holder.bind(currentCategory);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void setCategories(List<TaskCategory> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }
}