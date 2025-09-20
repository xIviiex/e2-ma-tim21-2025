package com.team21.questify.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.team21.questify.R;
import com.team21.questify.application.model.Equipment;
import com.team21.questify.utils.EquipmentHelper;

import java.util.ArrayList;
import java.util.List;

public class EquipmentAdapter extends RecyclerView.Adapter<EquipmentAdapter.EquipmentViewHolder> {

    private final List<Equipment> items = new ArrayList<>();

    public void setItems(List<Equipment> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EquipmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_equipment, parent, false);
        return new EquipmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipmentViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class EquipmentViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name, bonus, status;

        public EquipmentViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.iv_equipment_icon);
            name = itemView.findViewById(R.id.tv_equipment_name);
            bonus = itemView.findViewById(R.id.tv_equipment_bonus);
            status = itemView.findViewById(R.id.tv_equipment_status);
        }

        void bind(Equipment item) {
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
                status.setVisibility(View.VISIBLE);
                if (item.getUsesLeft() == -1) {
                    status.setText("Active (Permanent)");
                } else {
                    status.setText("Active (" + item.getUsesLeft() + " uses left)");
                }
            } else {
                status.setVisibility(View.GONE);
            }
        }
    }
}
