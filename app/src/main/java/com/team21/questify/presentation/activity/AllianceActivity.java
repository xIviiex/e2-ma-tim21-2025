package com.team21.questify.presentation.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.team21.questify.R;
import com.team21.questify.application.model.Alliance;
import com.team21.questify.application.model.User;
import com.team21.questify.application.service.AllianceService;
import com.team21.questify.application.service.UserService;
import com.team21.questify.presentation.adapter.InviteFriendsAdapter;
import com.team21.questify.presentation.adapter.UsersAdapter;
import com.team21.questify.utils.SharedPrefs;

import java.util.ArrayList;
import java.util.List;

public class AllianceActivity extends AppCompatActivity {
    private static final String TAG = "AllianceActivity";

    private TextView tvAllianceName, tvLeaderInfo;
    private Button btnInviteFriends, btnDisbandLeave;
    private RecyclerView rvMembers;
    private AllianceService allianceService;
    private UserService userService;
    private SharedPrefs sharedPrefs;
    private String allianceId;
    private Alliance currentAlliance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alliance);

        initServices();
        initViews();

        allianceId = getIntent().getStringExtra("allianceId");
        if (allianceId == null || allianceId.isEmpty()) {
            Toast.makeText(this, "Alliance ID not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllianceDetails(allianceId);
    }

    private void initServices() {
        userService = new UserService(this);
        allianceService = new AllianceService(this);
        sharedPrefs = new SharedPrefs(this);
    }

    private void initViews() {
        tvAllianceName = findViewById(R.id.tv_alliance_name);
        tvLeaderInfo = findViewById(R.id.tv_leader_info);
        btnInviteFriends = findViewById(R.id.btn_invite_friends);
        btnDisbandLeave = findViewById(R.id.btn_disband_leave_alliance);
        rvMembers = findViewById(R.id.rv_alliance_members);
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupListeners() {
        btnInviteFriends.setOnClickListener(v -> showInviteFriendsDialog());
        btnDisbandLeave.setOnClickListener(v -> handleDisbandOrLeave());
    }

    private void loadAllianceDetails(String id) {
        allianceService.getAllianceById(id)
                .addOnSuccessListener(alliance -> {
                    if (alliance == null) {
                        Toast.makeText(this, "Alliance not found.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    currentAlliance = alliance;
                    updateUI(alliance);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load alliance: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "loadAllianceDetails failed", e);
                    finish();
                });
    }

    private void updateUI(Alliance alliance) {
        tvAllianceName.setText(alliance.getName());

        userService.fetchUserProfile(alliance.getLeaderId())
                .addOnSuccessListener(leader -> tvLeaderInfo.setText("Leader: " + (leader != null ? leader.getUsername() : "Unknown")))
                .addOnFailureListener(e -> tvLeaderInfo.setText("Leader: Error"));

        if (alliance.getMembersIds() != null && !alliance.getMembersIds().isEmpty()) {
            userService.getUsersByIds(alliance.getMembersIds())
                    .addOnSuccessListener(members -> {
                        UsersAdapter membersAdapter = new UsersAdapter(members, sharedPrefs.getUserUid());
                        rvMembers.setAdapter(membersAdapter);
                    });
        }

        String currentUserId = sharedPrefs.getUserUid();
        if (currentUserId != null && currentUserId.equals(alliance.getLeaderId())) {
            btnDisbandLeave.setText("Disband Alliance");
        } else {
            btnDisbandLeave.setText("Leave Alliance");
        }
    }

    private void showInviteFriendsDialog() {
        String currentUserId = sharedPrefs.getUserUid();
        if (currentUserId == null || currentAlliance == null) return;

        Task<User> currentUserTask = userService.fetchUserProfile(currentUserId);
        Task<List<String>> pendingInvitesTask = allianceService.getPendingInvitedUserIds(allianceId);


        Tasks.whenAll(currentUserTask, pendingInvitesTask)
                .addOnSuccessListener(aVoid -> {
                    User currentUser = currentUserTask.getResult();
                    List<String> pendingInviteIds = pendingInvitesTask.getResult();

                    List<String> friendsIds = currentUser.getFriendsIds();
                    if (friendsIds == null || friendsIds.isEmpty()) {
                        Toast.makeText(this, "You have no friends to invite.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    friendsIds.removeAll(currentAlliance.getMembersIds());
                    friendsIds.removeAll(pendingInviteIds);

                    if (friendsIds.isEmpty()) {
                        Toast.makeText(this, "All your friends are either in the alliance or have a pending invite.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    userService.getUsersByIds(friendsIds)
                            .addOnSuccessListener(friends -> {
                                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                builder.setTitle("Invite Friends");
                                View dialogView = getLayoutInflater().inflate(R.layout.dialog_invite_friends, null);
                                RecyclerView rvFriends = dialogView.findViewById(R.id.rv_friends_to_invite);

                                AlertDialog dialog = builder.create();

                                InviteFriendsAdapter adapter = new InviteFriendsAdapter(this, friends, userToInvite -> {
                                    inviteUser(userToInvite, dialog);
                                });

                                rvFriends.setLayoutManager(new LinearLayoutManager(this));
                                rvFriends.setAdapter(adapter);

                                builder.setView(dialogView);
                                builder.setNegativeButton("Close", (d, which) -> d.dismiss());
                                dialog.setView(dialogView);
                                dialog.show();
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void inviteUser(User user, AlertDialog dialog) {
        if (allianceId == null || user.getUserId() == null) return;

        String currentUserId = sharedPrefs.getUserUid();
        allianceService.sendInvitation(allianceId, currentUserId, user.getUserId())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Invitation sent to " + user.getUsername(), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to send invitation: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void handleDisbandOrLeave() {
        if (currentAlliance == null) return;

        String currentUserId = sharedPrefs.getUserUid();
        boolean isLeader = currentUserId != null && currentUserId.equals(currentAlliance.getLeaderId());

        String title = isLeader ? "Disband Alliance" : "Leave Alliance";
        String message = isLeader ? "Are you sure you want to permanently disband this alliance? All members will be removed."
                : "Are you sure you want to leave this alliance?";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> {
                    btnDisbandLeave.setEnabled(false);
                    if (isLeader) {
                        disbandAlliance();
                    } else {
                        leaveAlliance();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void disbandAlliance() {
        allianceService.disbandAlliance(allianceId, sharedPrefs.getUserUid())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Alliance disbanded successfully.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to disband: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnDisbandLeave.setEnabled(true);
                });
    }

    private void leaveAlliance() {
        String currentUserId = sharedPrefs.getUserUid();

        Log.d(TAG, "Attempting to leave alliance...");
        Log.d(TAG, "Alliance Service is null: " + (allianceService == null));
        Log.d(TAG, "Alliance ID: " + allianceId);
        Log.d(TAG, "Current User ID: " + currentUserId);

        if (allianceService == null || allianceId == null || currentUserId == null) {
            Toast.makeText(this, "Critical error: Cannot leave alliance. Data is missing.", Toast.LENGTH_LONG).show();
            btnDisbandLeave.setEnabled(true);
            return;
        }

        allianceService.leaveAlliance(allianceId, currentUserId)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully left alliance.");
                    Toast.makeText(this, "You have left the alliance.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to leave alliance", e);
                    Toast.makeText(this, "Failed to leave: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnDisbandLeave.setEnabled(true);
                });
    }

}
