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
import com.team21.questify.application.service.UserService;
import com.team21.questify.utils.SharedPrefs;

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

        if (sharedPreferences.getUserUid() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Button btnViewProfile = findViewById(R.id.btn_view_profile);
        TextView welcomeTextView = findViewById(R.id.tv_welcome_title);

        welcomeTextView.setText("Welcome to Questify, " + sharedPreferences.getUsername() + "!");

        btnViewProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });
    }
}