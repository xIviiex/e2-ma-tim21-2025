package com.team21.questify.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.team21.questify.R;
import com.team21.questify.application.model.Equipment;
import com.team21.questify.utils.EquipmentHelper;

import java.util.ArrayList;
import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    public interface OnItemActionClickListener {
        void onActionClick(Equipment item);
    }

    private final List<Equipment> items = new ArrayList<>();
    private final OnItemActionClickListener listener;

    public InventoryAdapter(OnItemActionClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Equipment> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inventory, parent, false);
        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class InventoryViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name, bonus;
        Button actionButton;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.iv_equipment_icon);
            name = itemView.findViewById(R.id.tv_equipment_name);
            bonus = itemView.findViewById(R.id.tv_equipment_bonus);
            actionButton = itemView.findViewById(R.id.btn_action);
        }

        void bind(Equipment item, OnItemActionClickListener listener) {
            name.setText(EquipmentHelper.getName(item.getEquipmentId()));
            icon.setImageResource(EquipmentHelper.getIcon(item.getEquipmentId()));

            String bonusText = EquipmentHelper.getBonusText(item);
            if (!bonusText.isEmpty()) {
                bonus.setText(bonusText);
                bonus.setVisibility(View.VISIBLE);
            } else {
                bonus.setVisibility(View.GONE);
            }

            if (item.isActive()) {
                actionButton.setText("Deactivate");
            } else {
                actionButton.setText("Activate");
            }

            actionButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onActionClick(item);
                }
            });
        }
    }
}
