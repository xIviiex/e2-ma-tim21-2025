package com.team21.questify.presentation.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.team21.questify.R;
import com.team21.questify.application.service.UserService;

public class RegistrationActivity extends AppCompatActivity {
    private TextInputLayout tilEmail, tilPassword, tilConfirmPassword, tilUsername;
    private ImageView ivAvatar1, ivAvatar2, ivAvatar3, ivAvatar4, ivAvatar5;
    private UserService userService;
    private Button registerButton;

    private ImageView selectedAvatar = null;
    private String selectedAvatarName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        userService = new UserService(this);

        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        tilUsername = findViewById(R.id.til_username);
        registerButton = findViewById(R.id.btn_register);
        TextView loginLink = findViewById(R.id.tv_login_link);

        ivAvatar1 = findViewById(R.id.iv_avatar1);
        ivAvatar2 = findViewById(R.id.iv_avatar2);
        ivAvatar3 = findViewById(R.id.iv_avatar3);
        ivAvatar4 = findViewById(R.id.iv_avatar4);
        ivAvatar5 = findViewById(R.id.iv_avatar5);

        addTextWatchers();

        ivAvatar1.setOnClickListener(v -> selectAvatar(ivAvatar1, "avatar_1"));
        ivAvatar2.setOnClickListener(v -> selectAvatar(ivAvatar2, "avatar_2"));
        ivAvatar3.setOnClickListener(v -> selectAvatar(ivAvatar3, "avatar_3"));
        ivAvatar4.setOnClickListener(v -> selectAvatar(ivAvatar4, "avatar_4"));
        ivAvatar5.setOnClickListener(v -> selectAvatar(ivAvatar5, "avatar_5"));

        registerButton.setOnClickListener(v -> handleRegistration());

        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void handleRegistration() {
        if (!validateAllFields()) {
            return;
        }

        String email = tilEmail.getEditText().getText().toString().trim();
        String password = tilPassword.getEditText().getText().toString().trim();
        String confirmPassword = tilConfirmPassword.getEditText().getText().toString().trim();
        String username = tilUsername.getEditText().getText().toString().trim();

        userService.registerUser(email, password, confirmPassword, username, selectedAvatarName)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegistrationActivity.this, "Verification email sent. Please activate your account.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegistrationActivity.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void addTextWatchers() {
        if (tilEmail.getEditText() != null) {
            tilEmail.getEditText().addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validateEmail(); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
        if (tilUsername.getEditText() != null) {
            tilUsername.getEditText().addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validateUsername(); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
        if (tilPassword.getEditText() != null) {
            tilPassword.getEditText().addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validatePassword(); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
        if (tilConfirmPassword.getEditText() != null) {
            tilConfirmPassword.getEditText().addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validateConfirmPassword(); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private boolean validateAllFields() {
        boolean validEmail = validateEmail();
        boolean validUsername = validateUsername();
        boolean validPassword = validatePassword();
        boolean validConfirmPassword = validateConfirmPassword();

        if (selectedAvatarName == null) {
            Toast.makeText(this, "Please choose an avatar.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return validEmail && validUsername && validPassword && validConfirmPassword;
    }

    private boolean validateEmail() {
        String email = tilEmail.getEditText() != null ? tilEmail.getEditText().getText().toString().trim() : "";
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Invalid email.");
            return false;
        } else {
            tilEmail.setError(null);
            return true;
        }
    }

    private boolean validateUsername() {
        String username = tilUsername.getEditText() != null ? tilUsername.getEditText().getText().toString().trim() : "";
        if (username.isEmpty() || username.length() < 3 || username.length() > 20) {
            tilUsername.setError("Username has to have 3 - 20 characters.");
            return false;
        } else {
            tilUsername.setError(null);
            return true;
        }
    }

    private boolean validatePassword() {
        String password = tilPassword.getEditText() != null ? tilPassword.getEditText().getText().toString().trim() : "";
        if (password.length() < 8) {
            tilPassword.setError("Password has to have at least 8 characters.");
            return false;
        } else {
            tilPassword.setError(null);
            return true;
        }
    }

    private boolean validateConfirmPassword() {
        String password = tilPassword.getEditText() != null ? tilPassword.getEditText().getText().toString().trim() : "";
        String confirmPassword = tilConfirmPassword.getEditText() != null ? tilConfirmPassword.getEditText().getText().toString().trim() : "";
        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("The passwords do not match.");
            return false;
        } else {
            tilConfirmPassword.setError(null);
            return true;
        }
    }

    private void selectAvatar(ImageView avatarView, String avatarName) {
        if (selectedAvatar != null) {
            selectedAvatar.setBackgroundResource(R.drawable.avatar_background_unselected);
        }
        avatarView.setBackgroundResource(R.drawable.avatar_background_selected);

        selectedAvatar = avatarView;
        selectedAvatarName = avatarName;
    }
}
