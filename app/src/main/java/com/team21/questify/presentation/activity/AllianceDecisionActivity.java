package com.team21.questify.presentation.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.team21.questify.R;
import com.team21.questify.application.service.AllianceService;
import com.team21.questify.utils.SharedPrefs;

public class AllianceDecisionActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String invitationId = getIntent().getStringExtra("invitationId");
        String currentUserId = new SharedPrefs(this).getUserUid();

        if (invitationId == null || currentUserId == null) {
            finish();
            return;
        }

        AllianceService allianceService = new AllianceService(this);
        new AlertDialog.Builder(this)
                .setTitle("Confirm Action")
                .setMessage("You are already in an alliance. Do you want to leave it and join the new one?")
                .setCancelable(false)
                .setPositiveButton("Yes, Join New", (dialog, which) -> {
                    allianceService.forceAcceptInvitationAndLeaveOld(invitationId, currentUserId)
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Successfully joined new alliance!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    finish();
                })
                .setNegativeButton("No, Stay", (dialog, which) -> {
                    allianceService.declineInvitation(invitationId)
                            .addOnSuccessListener(aVoid -> Toast.makeText(getApplicationContext(), "Invitation declined.", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getApplicationContext(), "Failed to decline: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                    Log.d("AllianceDecisionActivity", "Dismissing dialog and finishing activity.");
                    dialog.dismiss();
                    finish();
                })
                .setOnCancelListener(dialog -> finish())
                .show();
    }
}
