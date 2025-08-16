package com.team21.questify.data.firebase;

import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseManager {

    private static FirebaseFirestore db;

    public static FirebaseFirestore getDatabaseInstance() {
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }
        return db;
    }
}
