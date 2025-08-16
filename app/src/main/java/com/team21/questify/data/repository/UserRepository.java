package com.team21.questify.data.repository;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
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

    public void activateUserInFirebase(String uid) {
        remoteDataSource.updateActivatedFlag(uid, true, task -> {
            if (!task.isSuccessful()) {
                Log.e("UserRepository", "Updating activated flag failed ", task.getException());
            }
        });
    }
}
