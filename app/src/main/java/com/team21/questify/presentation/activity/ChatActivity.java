package com.team21.questify.presentation.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;
import com.team21.questify.R;
import com.team21.questify.application.model.ChatMessage;
import com.team21.questify.application.model.enums.MissionActionType;
import com.team21.questify.application.service.AllianceService;
import com.team21.questify.application.service.ChatService;
import com.team21.questify.application.service.SpecialMissionService;
import com.team21.questify.presentation.adapter.ChatAdapter;
import com.team21.questify.utils.SharedPrefs;

import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";

    private RecyclerView rvChatMessages;
    private EditText etChatMessage;
    private Button btnSendMessage;

    private ChatService chatService;
    private AllianceService allianceService;
    private SharedPrefs sharedPrefs;
    private ChatAdapter chatAdapter;

    private String allianceId;
    private String currentUserId;
    private String currentUsername;
    private SpecialMissionService missionService;

    private ListenerRegistration messagesListenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        allianceId = getIntent().getStringExtra("allianceId");
        if (allianceId == null || allianceId.isEmpty()) {
            Toast.makeText(this, "Alliance ID not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar_chat);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initServicesAndPrefs();
        initViews();
        setupRecyclerView();
        setupListeners();
    }

    private void initServicesAndPrefs() {
        chatService = new ChatService(this);
        allianceService = new AllianceService(this);
        sharedPrefs = new SharedPrefs(this);
        currentUserId = sharedPrefs.getUserUid();
        currentUsername = sharedPrefs.getUsername();
        missionService = new SpecialMissionService(this);
    }

    private void initViews() {
        rvChatMessages = findViewById(R.id.rv_chat_messages);
        etChatMessage = findViewById(R.id.et_chat_message);
        btnSendMessage = findViewById(R.id.btn_send_message);

        allianceService.getAllianceById(allianceId).addOnSuccessListener(alliance -> {
            if (alliance != null && getSupportActionBar() != null) {
                getSupportActionBar().setTitle(alliance.getName());
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(layoutManager);
        rvChatMessages.setAdapter(chatAdapter);
    }

    private void setupListeners() {
        btnSendMessage.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String messageText = etChatMessage.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        btnSendMessage.setEnabled(false);
        etChatMessage.setText("");

        ChatMessage message = new ChatMessage(allianceId, currentUserId, currentUsername, messageText);

        chatService.sendMessage(message)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Message sent successfully.");
                    recordAllianceMessageSent();
                    allianceService.sendChatMessageNotification(message)
                            .addOnSuccessListener(task -> Log.d(TAG, "Chat notification trigger succeeded."))
                            .addOnFailureListener(e -> Log.e(TAG, "Chat notification trigger failed.", e));

                    btnSendMessage.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send message.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to send message", e);
                    etChatMessage.setText(messageText);
                    btnSendMessage.setEnabled(true);
                });
    }

    private void recordAllianceMessageSent() {
        missionService.recordUserAction(MissionActionType.SENT_ALLIANCE_MESSAGE, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Alliance message sent successfully recorded for special mission.");
            } else if (task.getException() != null) {
                Log.e(TAG, "Failed to record alliance message sent for special mission.", task.getException());
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        messagesListenerRegistration = chatService.getChatMessagesListener(allianceId, new ChatService.MessagesUpdateListener() {
            @Override
            public void onMessagesUpdated(List<ChatMessage> messages) {
                chatAdapter.setMessages(messages);
                rvChatMessages.scrollToPosition(messages.size() - 1);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error listening for chat messages", e);
                Toast.makeText(ChatActivity.this, "Error loading messages.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (messagesListenerRegistration != null) {
            messagesListenerRegistration.remove();
        }
    }
}