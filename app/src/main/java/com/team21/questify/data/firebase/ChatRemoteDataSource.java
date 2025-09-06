package com.team21.questify.data.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.team21.questify.application.model.ChatMessage;

public class ChatRemoteDataSource {
    private final FirebaseFirestore db;

    public ChatRemoteDataSource() {
        this.db = FirebaseFirestore.getInstance();
    }

    private CollectionReference getMessagesCollection(String allianceId) {
        return db.collection("alliances").document(allianceId).collection("messages");
    }

    public Task<Void> sendMessage(ChatMessage message) {
        DocumentReference docRef = getMessagesCollection(message.getAllianceId()).document();
        message.setMessageId(docRef.getId());
        return docRef.set(message);
    }

    public ListenerRegistration getChatMessagesListener(String allianceId, com.google.firebase.firestore.EventListener<com.google.firebase.firestore.QuerySnapshot> listener) {
        return getMessagesCollection(allianceId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limitToLast(50)
                .addSnapshotListener(listener);
    }
}
