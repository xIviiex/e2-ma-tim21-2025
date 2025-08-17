package com.team21.questify.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.team21.questify.R;
import com.team21.questify.application.model.User;

import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    private final List<User> userList;
    private final OnUserClickListener onUserClickListener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public UsersAdapter(List<User> userList, OnUserClickListener onUserClickListener) {
        this.userList = userList;
        this.onUserClickListener = onUserClickListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_list, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername;
        UserViewHolder(View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tv_user_item);
        }
        void bind(User user) {
            tvUsername.setText(user.getUsername());
            itemView.setOnClickListener(v -> onUserClickListener.onUserClick(user));
        }
    }
}