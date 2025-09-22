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
import com.team21.questify.application.service.EquipmentService;
import com.team21.questify.utils.EquipmentHelper;
import com.team21.questify.utils.LevelCalculator;

import java.util.List;

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ShopViewHolder> {

    public interface OnItemBuyClickListener {
        void onBuyClick(EquipmentHelper.ShopItem item, Button buyButton);
    }

    private final List<EquipmentHelper.ShopItem> items;
    private final EquipmentService equipmentService;
    private final int userLevel;
    private final OnItemBuyClickListener listener;

    public ShopAdapter(List<EquipmentHelper.ShopItem> items, int userLevel, EquipmentService equipmentService, OnItemBuyClickListener listener) {
        this.items = items;
        this.userLevel = userLevel;
        this.equipmentService = equipmentService;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ShopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shop, parent, false);
        return new ShopViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShopViewHolder holder, int position) {
        EquipmentHelper.ShopItem item = items.get(position);
        holder.bind(item, userLevel, equipmentService, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ShopViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name, description;
        Button buyButton;

        public ShopViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.iv_item_icon);
            name = itemView.findViewById(R.id.tv_item_name);
            description = itemView.findViewById(R.id.tv_item_description);
            buyButton = itemView.findViewById(R.id.btn_buy);
        }

        void bind(EquipmentHelper.ShopItem item, int userLevel, EquipmentService equipmentService, OnItemBuyClickListener listener) {
            name.setText(item.name);
            description.setText(item.description);
            icon.setImageResource(EquipmentHelper.getIcon(item.equipmentId));
            int previousLevelReward = LevelCalculator.getCoinsForLevel(userLevel - 1);
            int price = (int) (previousLevelReward * item.priceMultiplier);
            buyButton.setText("Buy (" + price + ")");

            buyButton.setOnClickListener(v -> listener.onBuyClick(item, buyButton));
        }
    }
}
