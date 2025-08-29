package com.team21.questify.presentation.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.team21.questify.R;
import com.team21.questify.application.model.User;
import com.team21.questify.application.service.UserService;
import com.team21.questify.utils.SharedPrefs;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private UserService userService;
    private SharedPrefs sharedPreferences;

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
        String userId = sharedPreferences.getUserUid();
        if (userId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        updateActiveDaysStreak(userId);

        initViews();
    }
    private void updateActiveDaysStreak(String userId) {
        User user = userService.getUserById(userId);

        long today = System.currentTimeMillis();
        if (user.getLastActiveDate() != null && isSameDay(user.getLastActiveDate(), today)) {
            return;
        }

        long yesterday = today - 24L * 60 * 60 * 1000;

        if (user.getLastActiveDate() != null && isSameDay(user.getLastActiveDate(), yesterday)) {
            user.setConsecutiveActiveDays(user.getConsecutiveActiveDays() + 1);
        }
        else if (user.getLastActiveDate() == null || !isSameDay(user.getLastActiveDate(), today)) {
            user.setConsecutiveActiveDays(1);
        }

        user.setLastActiveDate(today);
        userService.updateUser(user);
    }

    private boolean isSameDay(Long timestamp1, Long timestamp2) {
        if (timestamp1 == null || timestamp2 == null) {
            return false;
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(timestamp1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeInMillis(timestamp2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
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

        findViewById(R.id.iv_calendar_icon).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ViewTasksActivity.class);
            startActivity(intent);
        });
    }

}