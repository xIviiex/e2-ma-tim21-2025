package com.team21.questify.presentation.adapter;

import android.annotation.SuppressLint;
import android.util.Log;
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
import java.util.ArrayList;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    private final List<User> usersList;
    private final OnItemClickListener listener;
    private String currentUserId;
    private List<String> friendsIds;

    public List<String> getFriendsIds() {
        return friendsIds;
    }

    public void setFriendsIds(List<String> currentFriends) {
        this.friendsIds = currentFriends;
    }

    public interface OnItemClickListener {
        void onAddFriendClick(User user);
        void onUserClick(User user);
        void onRemoveFriendClick(User user);
    }

    public UsersAdapter(List<User> userList, List<String> friendsIds, OnItemClickListener listener, String currentUserId) {
        this.usersList = userList;
        this.friendsIds = friendsIds;
        this.listener = listener;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d("UsersAdapter", "onCreateViewHolder called, position: ");
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_list, parent, false);
        return new UserViewHolder(view, listener, usersList);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        Log.d("UsersAdapter", "onBindViewHolder called for position: " + position);
        User user = usersList.get(position);
        holder.bind(user);

        boolean isFriend = friendsIds.contains(user.getUserId());
        boolean isCurrentUser = user.getUserId().equals(currentUserId);

        if (isCurrentUser) {
            holder.addFriendButton.setVisibility(View.GONE);
        } else {
            holder.addFriendButton.setVisibility(View.VISIBLE);
            if (isFriend) {
                holder.addFriendButton.setText(R.string.remove_friend_button);
            } else {
                holder.addFriendButton.setText(R.string.add_friend_button);
            }
        }
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateLists(List<User> newList, List<String> newFriendsIds) {
        usersList.clear();
        usersList.addAll(newList);
        this.friendsIds = newFriendsIds;
        notifyDataSetChanged();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private final ImageView avatarImageView;
        private final TextView usernameTextView;
        private final TextView titleTextView;
        private final Button addFriendButton;

        public UserViewHolder(@NonNull View itemView, OnItemClickListener listener, List<User> usersList) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.iv_user_avatar);
            usernameTextView = itemView.findViewById(R.id.tv_username);
            titleTextView = itemView.findViewById(R.id.tv_user_title);
            addFriendButton = itemView.findViewById(R.id.btn_add_friend);

            addFriendButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    if (((Button)v).getText().toString().equals(v.getContext().getString(R.string.add_friend_button))) {
                        listener.onAddFriendClick(usersList.get(position));
                    } else {
                        listener.onRemoveFriendClick(usersList.get(position));
                    }
                }
            });

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onUserClick(usersList.get(position));
                }
            });
        }

        public void bind(final User user) {
            usernameTextView.setText(user.getUsername());
            titleTextView.setText(String.format("(%s)", user.getTitle()));

            int resId = itemView.getContext().getResources().getIdentifier(user.getAvatarName(), "drawable", itemView.getContext().getPackageName());
            if (resId != 0) {
                avatarImageView.setImageResource(resId);
            } else {
                avatarImageView.setImageResource(R.drawable.default_avatar);
            }
        }
    }
}
