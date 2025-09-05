package com.team21.questify.application.service;

import static android.content.ContentValues.TAG;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.team21.questify.presentation.activity.AllianceDecisionActivity;
import com.team21.questify.utils.SharedPrefs;

public class NotificationActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            Log.e(TAG, "Intent is NULL. Aborting.");
            return;
        }

        String invitationId = intent.getStringExtra("invitationId");
        int notificationId = intent.getIntExtra("notificationId", -1);
        dismissNotification(context, notificationId);

        AllianceService allianceService = new AllianceService(context);
        SharedPrefs sharedPrefs = new SharedPrefs(context);
        String currentUserId = sharedPrefs.getUserUid();

        if (intent.getAction() != null) {
            if (intent.getAction().equals("com.team21.questify.ACTION_ACCEPT_INVITATION")) {
                allianceService.acceptInvitation(invitationId, currentUserId)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(context, "You have successfully joined the alliance!", Toast.LENGTH_SHORT).show();
                            } else {
                                String errorMessage = task.getException().getMessage();
                                if (errorMessage != null && errorMessage.contains("ALREADY_IN_ALLIANCE")) {
                                    Intent decisionIntent = new Intent(context, AllianceDecisionActivity.class);
                                    decisionIntent.putExtra("invitationId", invitationId);
                                    decisionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(decisionIntent);
                                } else {
                                    Toast.makeText(context, "Failed to join alliance: " + errorMessage, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            } else if (intent.getAction().equals("com.team21.questify.ACTION_DECLINE_INVITATION")) {
                allianceService.declineInvitation(invitationId)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(context, "You have declined the invitation.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "Failed to decline invitation: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        }
    }
    private void dismissNotification(Context context, int notificationId) {
        if (notificationId != -1) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
        }
    }
}
