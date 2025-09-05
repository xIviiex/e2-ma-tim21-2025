package com.team21.questify.data.firebase;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.team21.questify.application.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRemoteDataSource {
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private static final String USERS_COLLECTION = "users";

    public UserRemoteDataSource() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public Task<AuthResult> createAuthUser(String email, String password) {
        return auth.createUserWithEmailAndPassword(email, password);
    }

    public Task<AuthResult> loginAuthUser(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }

    public Task<Void> sendVerificationEmail() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            return user.sendEmailVerification();
        }
        return com.google.android.gms.tasks.Tasks.forException(new Exception("User is not authenticated."));
    }

    public Task<DocumentSnapshot> fetchUserFromFirestore(String userId) {
        return db.collection(USERS_COLLECTION).document(userId).get();
    }

    public Task<Void> saveUserToFirestore(User user) {
        return db.collection(USERS_COLLECTION).document(user.getUserId()).set(user);
    }

    public Task<Void> updateUser(User user) {
        return db.collection(USERS_COLLECTION).document(user.getUserId()).set(user, com.google.firebase.firestore.SetOptions.merge());
    }

    public Task<Void> updateActivatedFlag(String uid, boolean activated) {
        return db.collection(USERS_COLLECTION).document(uid).update("activated", activated);
    }

    public Task<QuerySnapshot> checkUsernameExists(String username) {
        return db.collection(USERS_COLLECTION)
                .whereEqualTo("username", username)
                .limit(1)
                .get();
    }

    public Task<Void> deleteUserFromFirestore(String userId) {
        return db.collection(USERS_COLLECTION).document(userId).delete();
    }

    public Task<QuerySnapshot> fetchAllUsers() {
        return db.collection(USERS_COLLECTION).get();
    }

    public Task<QuerySnapshot> searchUsersByUsername(String usernamePattern) {
        return db.collection(USERS_COLLECTION)
                .whereGreaterThanOrEqualTo("username", usernamePattern)
                .whereLessThanOrEqualTo("username", usernamePattern + "\uf8ff")
                .get();
    }

    public Task<Void> createFriendship(String userId1, String userId2) {
        DocumentReference user1Ref = db.collection(USERS_COLLECTION).document(userId1);
        DocumentReference user2Ref = db.collection(USERS_COLLECTION).document(userId2);

        WriteBatch batch = db.batch();
        batch.update(user1Ref, "friendsIds", FieldValue.arrayUnion(userId2));
        batch.update(user2Ref, "friendsIds", FieldValue.arrayUnion(userId1));

        return batch.commit();
    }

    public Task<Void> removeFriendship(String userId1, String userId2) {
        DocumentReference user1Ref = db.collection(USERS_COLLECTION).document(userId1);
        DocumentReference user2Ref = db.collection(USERS_COLLECTION).document(userId2);

        WriteBatch batch = db.batch();
        batch.update(user1Ref, "friendsIds", FieldValue.arrayRemove(userId2));
        batch.update(user2Ref, "friendsIds", FieldValue.arrayRemove(userId1));

        return batch.commit();
    }

    public Task<Void> updateUserAllianceId(String userId, String allianceId) {
        return db.collection(USERS_COLLECTION).document(userId).update("currentAllianceId", allianceId);
    }

    public Task<Void> updateUserFCMToken(String userId, String fcmToken) {
        return db.collection(USERS_COLLECTION).document(userId).update("fcmToken", fcmToken);
    }

}
