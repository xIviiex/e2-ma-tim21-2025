package com.team21.questify.data.repository;

import android.content.Context;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.team21.questify.application.model.User;
import com.team21.questify.data.database.UserLocalDataSource;
import com.team21.questify.data.firebase.UserRemoteDataSource;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class UserRepository {
    private final UserLocalDataSource localDataSource;
    private final UserRemoteDataSource remoteDataSource;
    private final Executor executor;

    public UserRepository(Context context) {
        this.localDataSource = new UserLocalDataSource(context);
        this.remoteDataSource = new UserRemoteDataSource();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public Task<Void> registerUser(String email, String password, User user) {
        return remoteDataSource.createAuthUser(email, password)
                .onSuccessTask(authResult -> {
                    String uid = authResult.getUser().getUid();
                    user.setUserId(uid);
                    return remoteDataSource.saveUserToFirestore(user)
                            .onSuccessTask(aVoid -> Tasks.call(executor, () -> {
                                localDataSource.insertUser(user);
                                return null;
                            }));
                })
                .onSuccessTask(aVoid -> remoteDataSource.sendVerificationEmail());
    }

    public Task<AuthResult> loginUser(String email, String password) {
        return remoteDataSource.loginAuthUser(email, password);
    }

    public Task<User> getUserById(String userId) {
        return Tasks.call(executor, () -> localDataSource.getUserById(userId))
                .continueWithTask(task -> {
                    User localUser = task.getResult();
                    if (localUser != null) {
                        return Tasks.forResult(localUser);
                    } else {
                        return remoteDataSource.fetchUserFromFirestore(userId)
                                .onSuccessTask(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        User remoteUser = documentSnapshot.toObject(User.class);
                                        return Tasks.call(executor, () -> {
                                            localDataSource.insertUser(remoteUser);
                                            return remoteUser;
                                        });
                                    } else {
                                        return Tasks.forException(new Exception("User not found in remote source."));
                                    }
                                });
                    }
                });
    }

    public Task<Void> updateUser(User user) {
        return remoteDataSource.updateUser(user)
                .onSuccessTask(aVoid ->
                        Tasks.call(executor, () -> {
                            localDataSource.updateUser(user);
                            return null;
                        })
                );
    }

    public Task<Void> deleteUser(String userId) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null || !firebaseUser.getUid().equals(userId)) {
            return Tasks.forException(new Exception("Cannot delete another user or user not logged in."));
        }

        return firebaseUser.delete()
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        return remoteDataSource.deleteUserFromFirestore(userId);
                    } else {
                        throw task.getException();
                    }
                })
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        return Tasks.call(executor, () -> {
                            localDataSource.deleteUser(userId);
                            return null;
                        });
                    } else {
                        throw task.getException();
                    }
                });
    }

    public Task<List<User>> searchUsers(String usernamePattern) {
        return remoteDataSource.searchUsersByUsername(usernamePattern)
                .onSuccessTask(querySnapshot -> {
                    List<User> users = querySnapshot.toObjects(User.class);
                    return Tasks.call(executor, () -> {
                        for (User user : users) {
                            localDataSource.insertUser(user);
                        }
                        return users;
                    });
                });
    }

    public Task<Void> createFriendship(String currentUserId, String friendIdToAdd) {
        return remoteDataSource.createFriendship(currentUserId, friendIdToAdd)
                .onSuccessTask(aVoid ->
                        Tasks.whenAll(
                                fetchAndUpdateLocalUser(currentUserId),
                                fetchAndUpdateLocalUser(friendIdToAdd)
                        ).continueWith(task -> null) // Vraćamo Task<Void>
                );
    }

    public Task<Void> removeFriendship(String currentUserId, String friendIdToRemove) {
        return remoteDataSource.removeFriendship(currentUserId, friendIdToRemove)
                .onSuccessTask(aVoid ->
                        Tasks.whenAll(
                                fetchAndUpdateLocalUser(currentUserId),
                                fetchAndUpdateLocalUser(friendIdToRemove)
                        ).continueWith(task -> null)
                );
    }

    private Task<User> fetchAndUpdateLocalUser(String userId) {
        return remoteDataSource.fetchUserFromFirestore(userId).onSuccessTask(snapshot -> {
            User user = snapshot.toObject(User.class);
            if (user != null) {
                return Tasks.call(executor, () -> {
                    localDataSource.insertUser(user);
                    return user;
                });
            }
            return Tasks.forResult(null);
        });
    }

    public Task<Void> updateUserAllianceId(String userId, String allianceId) {
        return remoteDataSource.updateUserAllianceId(userId, allianceId)
                .onSuccessTask(aVoid ->
                        Tasks.call(executor, () -> {
                            localDataSource.updateUserAllianceId(userId, allianceId);
                            return null;
                        })
                );
    }
}
