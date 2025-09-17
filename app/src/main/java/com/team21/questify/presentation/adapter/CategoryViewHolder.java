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

    // View-ovi iz list_item_category.xml
    private final View viewCategoryColor;
    private final TextView textViewCategoryName;
    private final ImageButton imageButtonEditCategory;

    private TaskCategory currentCategory;

    public CategoryViewHolder(@NonNull View itemView, CategoryAdapter.OnCategoryEditClickListener editClickListener) {
        super(itemView);

        // Inicijalizacija View-ova
        viewCategoryColor = itemView.findViewById(R.id.viewCategoryColor);
        textViewCategoryName = itemView.findViewById(R.id.textViewCategoryName);
        imageButtonEditCategory = itemView.findViewById(R.id.imageButtonEditCategory);

        // Postavljanje listenera direktno u konstruktoru
        imageButtonEditCategory.setOnClickListener(v -> {
            if (editClickListener != null && currentCategory != null) {
                editClickListener.onEditClick(currentCategory);
            }
        });
    }

    /**
     * Metoda koja popunjava jedan red podacima iz TaskCategory objekta.
     * @param category Objekat kategorije za prikaz.
     */
    public void bind(TaskCategory category) {
        this.currentCategory = category;

        textViewCategoryName.setText(category.getName());

        // Postavljanje boje kruga
        // Proveravamo da li je pozadina GradientDrawable da bismo sigurno mogli da menjamo boju
        if (viewCategoryColor.getBackground() instanceof GradientDrawable) {
            GradientDrawable background = (GradientDrawable) viewCategoryColor.getBackground();
            try {
                // Pokušaj parsiranja heksadecimalne vrednosti boje
                background.setColor(Color.parseColor(category.getHexColor()));
            } catch (Exception e) {
                // Ako parsiranje ne uspe (npr. vrednost je null ili neispravna),
                // postavi podrazumevanu sivu boju.
                background.setColor(Color.GRAY);
            }
        }
    }
}