package com.team21.questify.presentation.adapter;

import android.annotation.SuppressLint;
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
import com.team21.questify.application.service.UserService;
import com.team21.questify.utils.SharedPrefs;

import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    private final List<User> usersList;
    private final OnItemClickListener listener;
    private final SharedPrefs sharedPrefs;
    private String currentUserId;
    private UserService userService;
    private List<String> friendsIds;

    public interface OnItemClickListener {
        void onAddFriendClick(User user);
        void onUserClick(User user);
    }

    public UsersAdapter(List<User> userList, OnItemClickListener listener, SharedPrefs sharedPrefs, UserService userService) {
        this.usersList = userList;
        this.listener = listener;
        this.sharedPrefs = sharedPrefs;
        this.currentUserId = sharedPrefs.getUserUid();
        this.userService = userService;
        this.friendsIds = userService.getUserById(currentUserId).getFriendsIds();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_list, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = usersList.get(position);
        holder.bind(user);

        List<String> friendsIds = userService.getUserById(currentUserId).getFriendsIds();
        boolean isFriend = friendsIds != null && friendsIds.contains(user.getUserId());

        boolean isCurrentUser = user.getUserId().equals(currentUserId);

        if (isCurrentUser || isFriend) {
            holder.addFriendButton.setVisibility(View.GONE);
        } else {
            holder.addFriendButton.setVisibility(View.VISIBLE);
        }

        holder.addFriendButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddFriendClick(user);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<User> newList) {
        usersList.clear();
        usersList.addAll(newList);
        notifyDataSetChanged();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private final ImageView avatarImageView;
        private final TextView usernameTextView;
        private final TextView titleTextView;
        private final Button addFriendButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.iv_user_avatar);
            usernameTextView = itemView.findViewById(R.id.tv_username);
            titleTextView = itemView.findViewById(R.id.tv_user_title);
            addFriendButton = itemView.findViewById(R.id.btn_add_friend);
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

            boolean isMyProfile = user.getUserId().equals(currentUserId);

            if (isMyProfile) {
                addFriendButton.setVisibility(View.GONE);
            } else {
                addFriendButton.setVisibility(View.VISIBLE);
                addFriendButton.setText(R.string.add_friend_button);
                addFriendButton.setEnabled(true);
            }

            addFriendButton.setOnClickListener(v -> listener.onAddFriendClick(user));
        }
    }
}