package com.team21.questify.application.service;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.messaging.RemoteMessage;
import com.team21.questify.R;
import com.team21.questify.data.firebase.UserRemoteDataSource;
import com.team21.questify.data.repository.UserRepository;
import com.team21.questify.utils.SharedPrefs;

import java.util.Map;

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {
    private static final String CHANNEL_ID = "alliance_invitations";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();

        if (data.containsKey("type") && data.get("type").equals("INVITATION")) {
            String allianceName = data.get("allianceName");
            String senderName = data.get("senderName");
            String invitationId = data.get("invitationId");
            String allianceId = data.get("allianceId");

            showInvitationNotification(allianceName, senderName, invitationId, allianceId);
        } else if (data.containsKey("type") && data.get("type").equals("INVITATION_ACCEPTED")) {
            String receiverName = data.get("receiverName");
            String allianceName = data.get("allianceName");
            showInfoNotification(receiverName, allianceName);
        }
    }

    private void showInvitationNotification(String allianceName, String senderName, String invitationId, String allianceId) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Alliance Invitations",
                NotificationManager.IMPORTANCE_HIGH
        );
        notificationManager.createNotificationChannel(channel);
        int notificationId = invitationId.hashCode();

        Intent acceptIntent = new Intent(this, NotificationActionReceiver.class);
        acceptIntent.setAction("com.team21.questify.ACTION_ACCEPT_INVITATION");
        acceptIntent.setData(Uri.parse("questify://invitation/accept/" + invitationId));
        acceptIntent.putExtra("invitationId", invitationId);
        acceptIntent.putExtra("notificationId", notificationId);
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(this, notificationId, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent declineIntent = new Intent(this, NotificationActionReceiver.class);
        declineIntent.setAction("com.team21.questify.ACTION_DECLINE_INVITATION");
        declineIntent.setData(Uri.parse("questify://invitation/decline/" + invitationId));
        declineIntent.putExtra("invitationId", invitationId);
        declineIntent.putExtra("notificationId", notificationId);
        PendingIntent declinePendingIntent = PendingIntent.getBroadcast(this, notificationId + 1, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Alliance Invitation")
                .setContentText(senderName + " has invited you to join " + allianceName + "!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setAutoCancel(false)
                .addAction(R.drawable.ic_accept, "Accept", acceptPendingIntent)
                .addAction(R.drawable.ic_decline, "Decline", declinePendingIntent);


        notificationManager.notify(invitationId.hashCode(), builder.build());
    }

    private void showInfoNotification(String receiverName, String allianceName) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Alliance Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Invitation Accepted")
                .setContentText(receiverName + " has joined your alliance " + allianceName + "!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        notificationManager.notify(0, builder.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        sendTokenToServer(token);
    }

    private void sendTokenToServer(String token) {
        SharedPrefs sharedPrefs = new SharedPrefs(this);
        String userId = sharedPrefs.getUserUid();

        if (userId != null) {
            UserRepository userRepository = new UserRepository(this);
            userRepository.getUserById(userId)
                    .onSuccessTask(user -> {
                        if (user != null) {
                            user.setFcmToken(token);
                            return userRepository.updateUser(user);
                        }
                        return Tasks.forException(new Exception("User not found locally to update token."));
                    })
                    .addOnSuccessListener(aVoid -> {
                        Log.d("FCMService", "FCM token successfully saved to Firestore and local DB.");
                        sharedPrefs.saveFCMToken(token);
                    })
                    .addOnFailureListener(e -> Log.e("FCMService", "Failed to save FCM token", e));
        }
    }
}
