package com.team21.questify.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.team21.questify.application.model.User;
import com.team21.questify.data.database.UserLocalDataSource;
import com.team21.questify.data.firebase.UserRemoteDataSource;

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

    public User getUserFromLocalDb(String email) {
        return localDataSource.getUserByEmail(email);
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
}
