package com.team21.questify.presentation.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.team21.questify.R;
import com.team21.questify.application.model.User;
import com.team21.questify.application.service.UserService;
import com.team21.questify.presentation.adapter.UsersAdapter;
import com.team21.questify.utils.SharedPrefs;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {
    private UserService userService;
    private SharedPrefs sharedPreferences;
    private RecyclerView rvUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        userService = new UserService(this);
        sharedPreferences = new SharedPrefs(this);

        if (sharedPreferences.getUserUid() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        fetchUsers();
    }

    private void initViews() {
        TextView welcomeTextView = findViewById(R.id.tv_welcome_title);
        welcomeTextView.setText("Welcome to Questify, " + sharedPreferences.getUsername() + "!");

        Button btnViewProfile = findViewById(R.id.btn_view_my_profile);
        btnViewProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("user_id", sharedPreferences.getUserUid());
            startActivity(intent);
        });


        Button btnCreateTask = findViewById(R.id.btn_create_task);
        btnCreateTask.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateTaskActivity.class);
            startActivity(intent);
        });

        Button btnCreateCategory = findViewById(R.id.btn_create_category);
        btnCreateCategory.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateTaskCategoryActivity.class);
            startActivity(intent);
        });

        rvUsers = findViewById(R.id.rv_users);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
    }

    private void fetchUsers() {
        userService.fetchAllUsers(task -> {
            if (task.isSuccessful()) {
                List<User> userList = task.getResult();
                setupRecyclerView(userList);
            } else {
                Toast.makeText(this, "Failed to load users.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView(List<User> userList) {
        String currentUserId = sharedPreferences.getUserUid();

        List<User> otherUsers = userList.stream()
                .filter(user -> !Objects.equals(user.getUserId(), currentUserId))
                .collect(Collectors.toList());

        UsersAdapter usersAdapter = new UsersAdapter(otherUsers, user -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("user_id", user.getUserId());
            startActivity(intent);
        });
        rvUsers.setAdapter(usersAdapter);
    }
}