package com.team21.questify.presentation.adapter;

import static com.team21.questify.application.model.enums.Badge.BRONZE_PARTICIPANT;
import static com.team21.questify.application.model.enums.Badge.GOLD_CONTRIBUTOR;
import static com.team21.questify.application.model.enums.Badge.MISSION_MASTER;
import static com.team21.questify.application.model.enums.Badge.SILVER_CONTRIBUTOR;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.team21.questify.R;
import com.team21.questify.application.model.enums.Badge;

import java.util.ArrayList;
import java.util.List;

public class BadgeAdapter  extends RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder> {

    private final List<Badge> badges = new ArrayList<>();

    public void setBadges(List<Badge> newBadges) {
        this.badges.clear();
        this.badges.addAll(newBadges);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_badge, parent, false);
        return new BadgeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        holder.bind(badges.get(position));
    }

    @Override
    public int getItemCount() {
        return badges.size();
    }

    static class BadgeViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;

        public BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.iv_badge_icon);
            name = itemView.findViewById(R.id.tv_badge_name);
        }

        void bind(Badge badge) {
            name.setText(badge.getBadgeName());
            icon.setImageResource(getBadgeIcon(badge));
        }

        private int getBadgeIcon(Badge badge) {
            switch (badge) {
                case BRONZE_PARTICIPANT: return R.drawable.ic_bronze_badge;
                case SILVER_CONTRIBUTOR: return R.drawable.ic_silver_badge;
                case GOLD_CONTRIBUTOR: return R.drawable.ic_gold_badge;
                case MISSION_MASTER: return R.drawable.ic_diamond_badge;
                default: return R.drawable.ic_help;
            }
        }
    }
}
