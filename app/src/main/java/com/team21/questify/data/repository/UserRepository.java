package com.team21.questify.data.repository;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.team21.questify.application.model.User;
import com.team21.questify.data.database.UserLocalDataSource;
import com.team21.questify.data.firebase.UserRemoteDataSource;

import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private final UserLocalDataSource localDataSource;
    private final UserRemoteDataSource remoteDataSource;

    public UserRepository(Context context) {
        this.localDataSource = new UserLocalDataSource(context);
        this.remoteDataSource = new UserRemoteDataSource();
    }

    public void registerUser(String email, String password, User user, OnCompleteListener<AuthResult> listener) {
        remoteDataSource.createAuthUser(email, password, task -> {
            if (task.isSuccessful() && task.getResult().getUser() != null) {
                String uid = task.getResult().getUser().getUid();
                user.setUserId(uid);

                remoteDataSource.sendVerificationEmail();
                remoteDataSource.saveUserToFirestore(user);
                localDataSource.insertUser(user);
            }
            listener.onComplete(task);
        });
    }

    public void loginUser(String email, String password, OnCompleteListener<AuthResult> listener) {
        remoteDataSource.loginAuthUser(email, password, listener);
    }

    public User getUserFromLocalDb(String userId) {
        return localDataSource.getUserById(userId);
    }

    public void fetchUserFromFirebase(String userId, OnCompleteListener<DocumentSnapshot> listener) {
        remoteDataSource.fetchUserFromFirestore(userId, listener);
    }

    public void activateUser(String uid, boolean status) {
        localDataSource.updateActivatedFlag(uid, status);
        remoteDataSource.updateActivatedFlag(uid, true, task -> {
            if (!task.isSuccessful()) {
                Log.e("UserRepository", "Updating activated flag failed ", task.getException());
            }
        });
    }

    public void isUsernameUnique(String username, OnCompleteListener<QuerySnapshot> listener) {
        remoteDataSource.isUsernameUnique(username, listener);
    }

    public void deleteUser(String userId, OnCompleteListener<Void> listener) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            user.delete().addOnCompleteListener(authDeleteTask -> {
                if (authDeleteTask.isSuccessful()) {
                    remoteDataSource.deleteUserFromFirestore(userId, firestoreDeleteTask -> {
                        if (firestoreDeleteTask.isSuccessful()) {
                            localDataSource.deleteUser(userId, listener);
                            listener.onComplete(firestoreDeleteTask);
                        } else {
                            listener.onComplete(firestoreDeleteTask);
                        }
                    });
                } else {
                    listener.onComplete(authDeleteTask);
                }
            });
        } else {
            listener.onComplete(com.google.android.gms.tasks.Tasks.forException(new Exception("No user is currently signed in to delete.")));
        }
    }

    public void getUserById(String userId, OnCompleteListener<User> onCompleteListener) {
        User localUser = localDataSource.getUserById(userId);
        if (localUser != null) {
            onCompleteListener.onComplete(Tasks.forResult(localUser));
        }

        remoteDataSource.fetchUserFromFirestore(userId, task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                User remoteUser = task.getResult().toObject(User.class);
                if (remoteUser != null) {
                    localDataSource.insertUser(remoteUser);
                    onCompleteListener.onComplete(Tasks.forResult(remoteUser));
                }
            } else {
                if (localUser == null) {
                    onCompleteListener.onComplete(Tasks.forException(task.getException()));
                }
            }
        });
    }

    public Task<User> getUserById(String userId) {
        User localUser = localDataSource.getUserById(userId);
        if (localUser != null) {
            return Tasks.forResult(localUser);
        }
        return remoteDataSource.fetchUserFromFirestore(userId).continueWith(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User remoteUser = task.getResult().toObject(User.class);
                if (remoteUser != null) {
                    localDataSource.insertUser(remoteUser);
                }
                return remoteUser;
            } else {
                throw task.getException();
            }
        });
    }

    public void getAllUsers(OnCompleteListener<List<User>> onCompleteListener) {
        List<User> localUsers = localDataSource.getAllUsers();
        if (!localUsers.isEmpty()) {
            onCompleteListener.onComplete(Tasks.forResult(localUsers));
        }

        remoteDataSource.fetchAllUsers(task -> {
            if (task.isSuccessful()) {
                List<User> remoteUsers = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    User user = document.toObject(User.class);
                    remoteUsers.add(user);
                    localDataSource.insertUser(user);
                }
                onCompleteListener.onComplete(Tasks.forResult(remoteUsers));
            } else {
                if (localUsers.isEmpty()) {
                    onCompleteListener.onComplete(Tasks.forException(task.getException()));
                }
            }
        });
    }

    public void updateUser(User user) {
        localDataSource.updateUser(user);
        remoteDataSource.saveUserToFirestore(user);
    }

    public void searchUsers(String usernamePattern, OnCompleteListener<List<User>> listener) {
        List<User> localUsers = localDataSource.searchUsersByUsername(usernamePattern);
        if (!localUsers.isEmpty()) {
            listener.onComplete(Tasks.forResult(localUsers));
        }

        remoteDataSource.searchUsersByUsername(usernamePattern, task -> {
            if (task.isSuccessful()) {
                List<User> remoteUsers = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    User user = document.toObject(User.class);
                    remoteUsers.add(user);
                    localDataSource.insertUser(user);
                }
                listener.onComplete(Tasks.forResult(remoteUsers));
            } else {
                if (localUsers.isEmpty()) {
                    listener.onComplete(Tasks.forException(task.getException()));
                }
            }
        });
    }

    public Task<Void> addFriend(String currentUserId, String friendIdToAdd) {
        return remoteDataSource.addFriend(currentUserId, friendIdToAdd)
                .addOnSuccessListener(aVoid -> {
                    User localUser = localDataSource.getUserById(currentUserId);
                    if (localUser != null) {
                        List<String> friends = new ArrayList<>(localUser.getFriendsIds());
                        if (!friends.contains(friendIdToAdd)) {
                            friends.add(friendIdToAdd);
                            localUser.setFriendsIds(friends);
                            localDataSource.updateUser(localUser);
                        }
                    }
                });
    }
}
