package com.team21.questify.application.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.team21.questify.application.model.User;
import com.team21.questify.data.repository.UserRepository;
import com.team21.questify.utils.SharedPrefs;

import java.util.Objects;

public class UserService {
    private final UserRepository userRepository;
    private final SharedPrefs sharedPreferences;

    public UserService(Context ctx) {
        this.userRepository = new UserRepository(ctx);
        this.sharedPreferences = new SharedPrefs(ctx);
    }

    public void loginUser(String email, String password, OnCompleteListener<AuthResult> listener) {
        if (email.isEmpty() || password.isEmpty()) {
            listener.onComplete(com.google.android.gms.tasks.Tasks.forException(new Exception("Please fill all fields")));
            return;
        }

        userRepository.loginUser(email, password, task -> {
            if (!task.isSuccessful()) {
                listener.onComplete(task);
                return;
            }

            FirebaseUser firebaseUser = task.getResult().getUser();
            if (firebaseUser == null) {
                listener.onComplete(com.google.android.gms.tasks.Tasks.forException(new Exception("User not found.")));
                return;
            }

            firebaseUser.reload().addOnCompleteListener(reloadTask -> {
                if (!reloadTask.isSuccessful()) {
                    listener.onComplete(com.google.android.gms.tasks.Tasks.forException(new Exception("Failed to refresh user data.")));
                    return;
                }

                FirebaseUser refreshedUser = FirebaseAuth.getInstance().getCurrentUser();
                if (refreshedUser == null) {
                    listener.onComplete(com.google.android.gms.tasks.Tasks.forException(new Exception("User session expired.")));
                    return;
                }

                if (!refreshedUser.isEmailVerified()) {
                    long creationTimeMillis = refreshedUser.getMetadata().getCreationTimestamp();
                    long currentTimeMillis = System.currentTimeMillis();
                    long accountAgeMillis = currentTimeMillis - creationTimeMillis;

                    if (accountAgeMillis > 2 * 60 * 1000) {
                        userRepository.deleteUser(refreshedUser.getUid(), deleteTask -> {
                            FirebaseAuth.getInstance().signOut();
                            if (deleteTask.isSuccessful()) {
                                listener.onComplete(com.google.android.gms.tasks.Tasks.forException(new Exception("Link has expired, please register again.")));
                            } else {
                                listener.onComplete(com.google.android.gms.tasks.Tasks.forException(new Exception("Link has expired, please register again.")));
                            }
                        });
                        return;
                    } else {
                        listener.onComplete(com.google.android.gms.tasks.Tasks.forException(new Exception("Please activate your account via email.")));
                        return;
                    }
                }

                userRepository.fetchUserFromFirebase(refreshedUser.getUid(), fetchTask -> {
                    if (!fetchTask.isSuccessful() || fetchTask.getResult() == null) {
                        listener.onComplete(com.google.android.gms.tasks.Tasks.forException(new Exception("Failed to fetch user data.")));
                        return;
                    }
                    User user = fetchTask.getResult().toObject(User.class);
                    if (user == null) {
                        listener.onComplete(com.google.android.gms.tasks.Tasks.forException(new Exception("User data not found.")));
                        return;
                    }
                    sharedPreferences.saveUserSession(user.getUserId(), user.getEmail(), user.getUsername());
                    userRepository.activateUser(user.getUserId(), true);
                    listener.onComplete(task);
                });
            });
        });
    }

    public void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        sharedPreferences.clearSession();
    }

    public void registerUser(String email, String password, String confirmPassword, String username, String avatarName, OnCompleteListener<AuthResult> listener) {

        String validationError = validateRegistrationData(email, password, confirmPassword, username, avatarName);
        if (validationError != null) {
            listener.onComplete(com.google.android.gms.tasks.Tasks.forException(new Exception(validationError)));
            return;
        }

        userRepository.isUsernameUnique(username, isUniqueTask -> {
            if (isUniqueTask.isSuccessful()) {
                if (!isUniqueTask.getResult().isEmpty()) {
                    listener.onComplete(com.google.android.gms.tasks.Tasks.forException(new Exception("Username already exists.")));
                    return;
                }

                long createdAt = System.currentTimeMillis();
                User newUser = new User(null, username, email, avatarName, 1, 0, false, createdAt);

                userRepository.registerUser(email, password, newUser, task -> {
                    if (task.isSuccessful() && task.getResult().getUser() != null) {
                        FirebaseUser firebaseUser = task.getResult().getUser();
                        Log.d("UserService", "Successful registration for user: " + firebaseUser.getUid());

                        sharedPreferences.lockUsername();
                        sharedPreferences.lockAvatar();

                        FirebaseAuth.getInstance().signOut();
                    }
                    listener.onComplete(task);
                });
            } else {
                listener.onComplete(com.google.android.gms.tasks.Tasks.forException(new Exception("Failed to check username uniqueness.")));
            }
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
        if (username == null || username.trim().isEmpty() || username.length() < 3 || username.length() > 20) {
            return "Username has to have 3-20 characters.";
        }
        if (avatarName == null || avatarName.isEmpty()) {
            return "Choose avatar.";
        }
        return null;
    }
}
