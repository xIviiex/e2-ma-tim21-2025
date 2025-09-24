package com.team21.questify.presentation.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.team21.questify.R;

import java.util.List;

public class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ColorViewHolder> {

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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_color_circle, parent, false);
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
            GradientDrawable background = (GradientDrawable) colorCircle.getBackground();
            if (background != null) {
                background.setColor(Color.parseColor(hexColor));
            }
            selectionBorder.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        }
    }


    public interface OnColorSelectedListener {
        void onColorSelected(String hexColor);
    }
}
