package com.team21.questify.application.service;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.messaging.FirebaseMessaging;
import com.team21.questify.application.model.BattleStats;
import com.team21.questify.data.firebase.UserRemoteDataSource;
import com.team21.questify.utils.LevelCalculator;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.team21.questify.application.model.User;
import com.team21.questify.data.repository.UserRepository;
import com.team21.questify.utils.SharedPrefs;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class UserService {
    private final UserRepository userRepository;
    private final SharedPrefs sharedPreferences;
    private final FirebaseAuth firebaseAuth;

    public UserService(Context ctx) {
        this.userRepository = new UserRepository(ctx);
        this.sharedPreferences = new SharedPrefs(ctx);
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    public Task<User> loginUser(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            return Tasks.forException(new Exception("Please fill all fields"));
        }

        return userRepository.loginUser(email, password)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    FirebaseUser firebaseUser = task.getResult().getUser();
                    if (firebaseUser == null) throw new Exception("User not found.");

                    return firebaseUser.reload().continueWith(reloadTask -> firebaseUser);
                })
                .continueWithTask(task -> {
                    FirebaseUser refreshedUser = task.getResult();
                    if (!refreshedUser.isEmailVerified()) {
                        long creationTime = refreshedUser.getMetadata().getCreationTimestamp();
                        long ageInMillis = System.currentTimeMillis() - creationTime;

                        if (ageInMillis > TimeUnit.HOURS.toMillis(24)) {
                            return userRepository.deleteUser(refreshedUser.getUid())
                                    .continueWithTask(deleteTask -> {
                                        firebaseAuth.signOut();
                                        throw new Exception("Link has expired, please register again.");
                                    });
                        } else {
                            throw new Exception("Please activate your account via email.");
                        }
                    }
                    return userRepository.getUserById(refreshedUser.getUid());
                })
                .onSuccessTask(user -> {
                    sharedPreferences.saveUserSession(user.getUserId(), user.getEmail(), user.getUsername());
                    return updateFcmToken(user);
                });
    }

    public Task<Void> registerUser(String email, String password, String confirmPassword, String username, String avatarName) {
        String validationError = validateRegistrationData(email, password, confirmPassword, username, avatarName);
        if (validationError != null) {
            return Tasks.forException(new Exception(validationError));
        }

        long createdAt = System.currentTimeMillis();
        User newUser = new User("", username, email, avatarName, createdAt);

        return userRepository.registerUser(email, password, newUser)
                .addOnSuccessListener(aVoid -> {
                    sharedPreferences.lockUsername();
                    sharedPreferences.lockAvatar();
                    firebaseAuth.signOut();
                });
    }

    public void logoutUser() {
        firebaseAuth.signOut();
        sharedPreferences.clearSession();
    }

    public Task<Void> changePassword(String oldPassword, String newPassword, String confirmNewPassword) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            return Tasks.forException(new Exception("No user is currently logged in."));
        }
        if (newPassword == null || newPassword.length() < 8) {
            return Tasks.forException(new Exception("New password must be at least 8 characters long."));
        }
        if (!newPassword.equals(confirmNewPassword)) {
            return Tasks.forException(new Exception("New passwords do not match."));
        }

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);
        return user.reauthenticate(credential)
                .continueWithTask(reauthTask -> {
                    if (reauthTask.isSuccessful()) {
                        return user.updatePassword(newPassword);
                    } else {
                        throw new Exception("Incorrect old password.", reauthTask.getException());
                    }
                });
    }

    public Task<Void> addXpAndCheckLevelUp(String userId, int xpToAdd) {
        return userRepository.getUserById(userId).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();

            User user = task.getResult();
            int newXp = user.getXp() + xpToAdd;
            int requiredXp = LevelCalculator.getRequiredXpForNextLevel(user.getLevel());

            if (newXp >= requiredXp) {
                int newLevel = user.getLevel() + 1;
                user.setLevel(newLevel);
                user.setXp(newXp - requiredXp);
                user.setTitle(LevelCalculator.getTitleForLevel(newLevel));
                // this is probably not needed anymore, user is rewarded with pp after boss battle
                // user.setPowerPoints(user.getPowerPoints() + LevelCalculator.getPowerPointsForLevel(newLevel));
                Log.d("UserService", "User " + user.getUsername() + " leveled up to level " + newLevel);
            } else {
                user.setXp(newXp);
            }
            return userRepository.updateUser(user);
        });
    }

    public Task<User> fetchUserProfile(String userId) { return userRepository.getUserById(userId); }
    public Task<Void> updateUser(User user) { return userRepository.updateUser(user); }
    public Task<List<User>> searchUsers(String query) { return userRepository.searchUsers(query); }
    public Task<Void> addFriendship(String currentUserId, String friendIdToAdd) { return userRepository.createFriendship(currentUserId, friendIdToAdd); }
    public Task<Void> removeFriendship(String currentUserId, String friendIdToRemove) { return userRepository.removeFriendship(currentUserId, friendIdToRemove); }

    private Task<User> updateFcmToken(User user) {
        return FirebaseMessaging.getInstance().getToken().onSuccessTask(token -> {
            sharedPreferences.saveFCMToken(token);
            user.setFcmToken(token);
            return userRepository.updateUser(user).continueWith(task -> user);
        });
    }

    private String validateRegistrationData(String email, String password, String confirmPassword, String username, String avatarName) {
        if (email == null || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Invalid email.";
        if (password == null || password.length() < 8) return "Password has to have at least 8 characters.";
        if (!password.equals(confirmPassword)) return "Passwords are not matching.";
        if (username == null || username.trim().length() < 3 || username.length() > 20) return "Username has to have 3-20 characters.";
        if (avatarName == null || avatarName.isEmpty()) return "Choose avatar.";
        return null;
    }

    public Task<Pair<Boolean, String>> checkUserAllianceStatus(String userId) {
        return userRepository.getUserById(userId).continueWith(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User user = task.getResult();
                String currentAllianceId = user.getCurrentAllianceId();
                boolean isInAlliance = currentAllianceId != null && !currentAllianceId.isEmpty();
                return Pair.create(isInAlliance, currentAllianceId);
            }
            Log.e("UserService", "Failed to check alliance status", task.getException());
            return Pair.create(false, null);
        });
    }

    public Task<List<User>> getUsersByIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Tasks.forResult(new java.util.ArrayList<>());
        }

        List<Task<User>> tasks = new java.util.ArrayList<>();
        for (String id : userIds) {
            tasks.add(userRepository.getUserById(id));
        }

        return Tasks.whenAllSuccess(tasks).continueWith(task -> {
            List<User> userList = new java.util.ArrayList<>();
            for (Object obj : task.getResult()) {
                userList.add((User) obj);
            }
            return userList;
        });
    }

    // ovo pozovi nakon borbe, smanji vrednosti mnozilaca u battle stats ako borba nije 100% uspesna pa ih onda prosledi
    public Task<Void> addPPAndCoinsAfterBossBattle(String userId, BattleStats stats, boolean battleWon) {
        return userRepository.getUserById(userId).continueWithTask(userTask -> {
            if (!userTask.isSuccessful() || userTask.getResult() == null) {
                throw new Exception("User not found to update stats.", userTask.getException());
            }
            User user = userTask.getResult();

            if (battleWon) {
                int coinReward = LevelCalculator.getCoinsForLevel(user.getLevel());
                int finalCoinReward = (int) (coinReward * stats.getCoinsMultiplier());
                user.setCoins(user.getCoins() + finalCoinReward);
            }

            int ppReward = LevelCalculator.getPowerPointsForLevel(user.getLevel());
            user.setPowerPoints(user.getPowerPoints() + ppReward);

            return userRepository.updateUser(user);
        });
    }
}
