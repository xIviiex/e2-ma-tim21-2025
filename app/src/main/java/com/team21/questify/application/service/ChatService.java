package com.team21.questify.application.service;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.ListenerRegistration;
import com.team21.questify.application.model.ChatMessage;
import com.team21.questify.data.repository.ChatRepository;

import java.util.List;

public class ChatService {
    private final ChatRepository chatRepository;

    public interface MessagesUpdateListener {
        void onMessagesUpdated(List<ChatMessage> messages);
        void onError(Exception e);
    }

    public ChatService(Context context) {
        this.chatRepository = new ChatRepository(context);
    }

    public Task<Void> sendMessage(ChatMessage message) {
        return chatRepository.sendMessage(message);
    }

    public ListenerRegistration getChatMessagesListener(String allianceId, MessagesUpdateListener listener) {
        return chatRepository.getChatMessagesListener(allianceId, (snapshots, error) -> {
            if (error != null) {
                listener.onError(error);
                return;
            }
            if (snapshots != null) {
                List<ChatMessage> messages = snapshots.toObjects(ChatMessage.class);
                listener.onMessagesUpdated(messages);
            }
        });
    }
}
