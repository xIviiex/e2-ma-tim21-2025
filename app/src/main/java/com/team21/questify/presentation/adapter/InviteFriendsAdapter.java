package com.team21.questify.presentation.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.team21.questify.R;
import com.team21.questify.application.model.User;

import java.util.List;

public class InviteFriendsAdapter extends RecyclerView.Adapter<InviteFriendsAdapter.InviteViewHolder> {

    public interface OnInviteClickListener {
        void onInviteClick(User user);
    }

    private final List<User> friendsList;
    private final OnInviteClickListener listener;
    private final Context context;

    public InviteFriendsAdapter(Context context, List<User> friendsList, OnInviteClickListener listener) {
        this.context = context;
        this.friendsList = friendsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public InviteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_list, parent, false);
        return new InviteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InviteViewHolder holder, int position) {
        User user = friendsList.get(position);
        holder.bind(user);
        holder.inviteButton.setOnClickListener(v -> listener.onInviteClick(user));
    }

    @Override
    public int getItemCount() {
        return friendsList.size();
    }

    static class InviteViewHolder extends RecyclerView.ViewHolder {
        private final ImageView avatarImageView;
        private final TextView usernameTextView;
        private final TextView titleTextView;
        private final Button inviteButton;

        public InviteViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.iv_user_avatar);
            usernameTextView = itemView.findViewById(R.id.tv_username);
            titleTextView = itemView.findViewById(R.id.tv_user_title);
            inviteButton = itemView.findViewById(R.id.btn_add_friend);
        }

        public void bind(final User user) {
            usernameTextView.setText(user.getUsername());
            titleTextView.setText(String.format("(%s)", user.getTitle()));
            inviteButton.setText("Invite");

            int resId = itemView.getContext().getResources().getIdentifier(user.getAvatarName(), "drawable", itemView.getContext().getPackageName());
            if (resId != 0) {
                avatarImageView.setImageResource(resId);
            } else {
                avatarImageView.setImageResource(R.drawable.default_avatar);
            }
        }
    }
}
