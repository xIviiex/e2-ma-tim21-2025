package com.team21.questify.data.firebase;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.team21.questify.application.model.User;

import java.util.ArrayList;
import java.util.List;

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

    public void loginAuthUser(String email, String password, OnCompleteListener<AuthResult> listener) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(listener);
    }

    public void fetchUserFromFirestore(String userId, OnCompleteListener<DocumentSnapshot> listener) {
        db.collection("users").document(userId).get().addOnCompleteListener(listener);
    }

    public Task<DocumentSnapshot> fetchUserFromFirestore(String userId) {
        return db.collection("users").document(userId).get();
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

    public void isUsernameUnique(String username, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnCompleteListener(listener);
    }

    public void deleteUserFromFirestore(String userId, OnCompleteListener<Void> listener) {
        db.collection("users").document(userId).delete().addOnCompleteListener(listener);
    }

    public void fetchAllUsers(OnCompleteListener<QuerySnapshot> listener) {
        db.collection("users").get().addOnCompleteListener(listener);
    }

    public void searchUsersByUsername(String usernamePattern, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("users")
                .whereGreaterThanOrEqualTo("username", usernamePattern)
                .whereLessThanOrEqualTo("username", usernamePattern + "\uf8ff")
                .get()
                .addOnCompleteListener(listener);
    }

    public Task<Void> addFriend(String currentUserId, String friendIdToAdd) {
        return db.collection("users").document(currentUserId).get()
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        User user = task.getResult().toObject(User.class);
                        if (user != null) {
                            List<String> friendsIds = user.getFriendsIds();
                            if (friendsIds == null) {
                                friendsIds = new ArrayList<>();
                            }
                            if (!friendsIds.contains(friendIdToAdd)) {
                                friendsIds.add(friendIdToAdd);
                                return db.collection("users").document(currentUserId)
                                        .update("friendsIds", friendsIds);
                            } else {
                                return Tasks.forException(new Exception("User is already a friend."));
                            }
                        }
                    }
                    return Tasks.forException(task.getException());
                });
    }

    public Task<Void> removeFriend(String userId, String friendIdToRemove) {
        return db.collection("users").document(userId).get()
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        User user = task.getResult().toObject(User.class);
                        if (user != null) {
                            List<String> friendsIds = user.getFriendsIds();
                            if (friendsIds != null) {
                                friendsIds.remove(friendIdToRemove);
                                return db.collection("users").document(userId)
                                        .update("friendsIds", friendsIds);
                            }
                        }
                    }
                    return Tasks.forException(new Exception("Failed to remove friend."));
                });
    }

}
