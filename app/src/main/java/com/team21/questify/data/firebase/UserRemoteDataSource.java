package com.team21.questify.data.firebase;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.team21.questify.application.model.User;

public class UserRemoteDataSource {
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public UserRemoteDataSource() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public void createAuthUser(String email, String password, OnCompleteListener<AuthResult> listener) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(listener);
    }

    public void sendVerificationEmail() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification();
        }
    }

    public void saveUserToFirestore(User user) {
        if (user.getUserId() != null) {
            db.collection("users").document(user.getUserId()).set(user);
        }
    }

    public void updateActivatedFlag(String uid, boolean activated, OnCompleteListener<Void> listener) {
        db.collection("users").document(uid)
                .update("activated", activated)
                .addOnCompleteListener(listener);

    }
}
