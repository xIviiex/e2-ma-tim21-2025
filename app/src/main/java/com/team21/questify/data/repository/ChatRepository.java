package com.team21.questify.data.repository;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.ListenerRegistration;
import com.team21.questify.application.model.ChatMessage;
import com.team21.questify.data.database.ChatLocalDataSource;
import com.team21.questify.data.firebase.ChatRemoteDataSource;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChatRepository {
    private final ChatRemoteDataSource remoteDataSource;
    private final ChatLocalDataSource localDataSource;
    private final Executor executor;

    public ChatRepository(Context context) {
        this.remoteDataSource = new ChatRemoteDataSource();
        this.localDataSource = new ChatLocalDataSource(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public Task<Void> sendMessage(ChatMessage message) {
        return remoteDataSource.sendMessage(message);
    }

    public ListenerRegistration getChatMessagesListener(String allianceId, com.google.firebase.firestore.EventListener<com.google.firebase.firestore.QuerySnapshot> listener) {
        return remoteDataSource.getChatMessagesListener(allianceId, (snapshots, error) -> {
            if (snapshots != null) {
                Tasks.call(executor, () -> {
                    localDataSource.insertOrUpdateMessages(snapshots.toObjects(ChatMessage.class));
                    return null;
                });
            }
            listener.onEvent(snapshots, error);
        });
    }
}
