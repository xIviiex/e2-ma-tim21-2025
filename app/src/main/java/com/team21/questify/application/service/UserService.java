package com.team21.questify.application.service;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.team21.questify.application.model.User;
import com.team21.questify.data.repository.UserRepository;
import com.team21.questify.utils.SharedPrefs;

public class UserService {
    private final UserRepository userRepository;
    private final Context context;
    private final SharedPrefs sharedPreferences;

    public UserService(Context ctx) {
        this.context = ctx.getApplicationContext();
        this.userRepository = new UserRepository(ctx);
        this.sharedPreferences = new SharedPrefs(ctx);
    }

    public void registerUser(String email, String password, String confirmPassword, String username, String avatarName, OnCompleteListener<AuthResult> listener) {

        String validationError = validateRegistrationData(email, password, confirmPassword, username, avatarName);
        if (validationError != null) {
            listener.onComplete(com.google.android.gms.tasks.Tasks.forException(new Exception(validationError)));
            return;
        }

        long createdAt = System.currentTimeMillis();
        User newUser = new User(null, username, email, avatarName, 1, 0, false, createdAt);

        userRepository.registerUser(email, password, newUser, task -> {
            if (task.isSuccessful() && task.getResult().getUser() != null) {
                FirebaseUser firebaseUser = task.getResult().getUser();
                Log.d("UserService", "Successful registration for user: " + firebaseUser.getUid());

                sharedPreferences.saveUserSession(firebaseUser.getUid(), email);
                sharedPreferences.lockUsername();
                sharedPreferences.lockAvatar();
            }
            listener.onComplete(task);
        });
    }

    private String validateRegistrationData(String email, String password, String confirmPassword, String username, String avatarName) {
        if (email == null || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Invalid email.";
        }
        if (password == null || password.length() < 8) {
            return "Password has to have at least 8 characters.";
        }
        if (!password.equals(confirmPassword)) {
            return "Passwords are not matching.";
        }
        if (username == null || username.trim().isEmpty() || username.length() < 3) {
            return "Username has to have 3-20 characters, without spaces.";
        }
        if (avatarName == null || avatarName.isEmpty()) {
            return "Choose avatar.";
        }
        return null;
    }
}
