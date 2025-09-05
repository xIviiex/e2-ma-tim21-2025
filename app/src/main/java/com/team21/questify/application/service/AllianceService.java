package com.team21.questify.application.service;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.auth.oauth2.GoogleCredentials;
import com.team21.questify.R;
import com.team21.questify.application.model.Alliance;
import com.team21.questify.application.model.Invitation;
import com.team21.questify.application.model.User;
import com.team21.questify.application.model.enums.MissionStatus;
import com.team21.questify.application.model.enums.RequestStatus;
import com.team21.questify.data.repository.AllianceRepository;
import com.team21.questify.data.repository.InvitationRepository;
import com.team21.questify.data.repository.UserRepository;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class AllianceService {
    private static final String TAG = "AllianceService";
    private final AllianceRepository allianceRepository;
    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final Context context;
    private static String accessToken;

    public AllianceService(Context context) {
        this.allianceRepository = new AllianceRepository(context);
        this.invitationRepository = new InvitationRepository(context);
        this.userRepository = new UserRepository(context);
        this.context = context.getApplicationContext();
    }

    public Task<Alliance> getAllianceById(String allianceId) {
        return allianceRepository.getAllianceById(allianceId);
    }

    public Task<Void> createAlliance(String allianceName, String leaderId) {
        return allianceRepository.createAlliance(allianceName, leaderId);
    }

    public Task<Void> sendInvitation(String allianceId, String senderId, String receiverId) {
        Invitation invitation = new Invitation(null, allianceId, senderId, receiverId);

        return invitationRepository.saveInvitation(invitation).onSuccessTask(aVoid -> {
            return sendInvitationNotification(invitation);
        });
    }

    //isidorap-ivaz-firebase-adminsdk-fbsvc-0352b4d5ac
    private Task<Void> sendInvitationNotification(Invitation invitation) {
        Task<User> senderTask = userRepository.getUserById(invitation.getSenderId());
        Task<User> receiverTask = userRepository.getUserById(invitation.getReceiverId());
        Task<Alliance> allianceTask = allianceRepository.getAllianceById(invitation.getAllianceId());

        return Tasks.whenAll(senderTask, receiverTask, allianceTask).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Failed to fetch data for notification", task.getException());
                return Tasks.forResult(null);
            }

            User sender = senderTask.getResult();
            User receiver = receiverTask.getResult();
            Alliance alliance = allianceTask.getResult();

            if (sender == null || receiver == null || alliance == null) {
                Log.e(TAG, "Sender, Receiver or Alliance is null. Aborting notification.");
                return Tasks.forResult(null);
            }
            String receiverToken = receiver.getFcmToken();
            if (receiverToken == null || receiverToken.isEmpty()) {
                Log.e(TAG, "Receiver does not have an FCM token.");
                return Tasks.forResult(null);
            }

            try {
                JSONObject notificationJson = new JSONObject();
                notificationJson.put("title", "Alliance Invitation");
                notificationJson.put("body", sender.getUsername() + " has invited you to join " + alliance.getName() + "!");

                JSONObject dataJson = new JSONObject();
                dataJson.put("type", "INVITATION");
                dataJson.put("invitationId", invitation.getInvitationId());
                dataJson.put("allianceId", invitation.getAllianceId());
                dataJson.put("senderName", sender.getUsername());
                dataJson.put("allianceName", alliance.getName());

                JSONObject messageJson = new JSONObject();
                messageJson.put("token", receiverToken);
                messageJson.put("notification", notificationJson);
                messageJson.put("data", dataJson);

                JSONObject mainJson = new JSONObject();
                mainJson.put("message", messageJson);

                sendFcmRequest(mainJson);

            } catch (Exception e) {
                Log.e(TAG, "Error creating V1 notification JSON", e);
            }

            return Tasks.forResult(null);
        });
    }

    private void sendFcmRequest(JSONObject mainBody) {
        String projectId = "isidorap-ivaz";
        String fcmApiUrl = "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send";

        getAccessToken().addOnSuccessListener(token -> {
            RequestQueue queue = Volley.newRequestQueue(context);
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, fcmApiUrl, mainBody,
                    response -> Log.d(TAG, "V1 Notification sent successfully: " + response.toString()),
                    error -> {
                        Log.e(TAG, "V1 Notification send error: " + error.toString());
                        if (error.networkResponse != null) {
                            Log.e(TAG, "Error Code: " + error.networkResponse.statusCode);
                            Log.e(TAG, "Error Data: " + new String(error.networkResponse.data));
                        }
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Authorization", "Bearer " + token);
                    return headers;
                }
            };
            queue.add(request);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get Access Token", e);
        });
    }

    private Task<String> getAccessToken() {
        if (accessToken != null) {
            return Tasks.forResult(accessToken);
        }

        return Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            try {
                InputStream serviceAccountStream = context.getResources().openRawResource(R.raw.service_account);

                GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccountStream)
                        .createScoped(Collections.singleton("https://www.googleapis.com/auth/firebase.messaging"));

                credentials.refreshIfExpired();
                accessToken = credentials.getAccessToken().getTokenValue();
                return accessToken;

            } catch (Exception e) {
                Log.e(TAG, "Error generating access token", e);
                throw e;
            }
        });
    }

    public Task<Void> acceptInvitation(String invitationId, String userId) {
        return invitationRepository.getInvitationById(invitationId).continueWithTask(invitationTask -> {
            if (invitationTask.isSuccessful() && invitationTask.getResult() != null) {
                Invitation invitation = invitationTask.getResult().toObject(Invitation.class);

                if (invitation.getStatus() != RequestStatus.PENDING) {
                    return Tasks.forException(new Exception("Invitation is no longer pending."));
                }

                return userRepository.getUserById(userId).continueWithTask(userTask -> {
                    if (!userTask.isSuccessful() || userTask.getResult() == null) {
                        throw new Exception("Failed to fetch your user data.", userTask.getException());
                    }
                    User user = userTask.getResult();
                    String oldAllianceId = user.getCurrentAllianceId();

                    if (oldAllianceId != null && !oldAllianceId.isEmpty()) {
                        throw new Exception("ALREADY_IN_ALLIANCE");
                    } else {
                        return joinNewAlliance(invitation, userId);
                    }
                });
            }
            return null;
        });
    }

    public Task<Void> forceAcceptInvitationAndLeaveOld(String invitationId, String userId) {
        return invitationRepository.getInvitationById(invitationId).continueWithTask(invitationTask -> {
            Invitation invitation = invitationTask.getResult().toObject(Invitation.class);
            return userRepository.getUserById(userId).continueWithTask(userTask -> {
                User user = userTask.getResult();
                String oldAllianceId = user.getCurrentAllianceId();

                return allianceRepository.getAllianceById(oldAllianceId).continueWithTask(oldAllianceTask -> {
                    Alliance oldAlliance = oldAllianceTask.getResult();
                    if (oldAlliance.getMissionStatus() == MissionStatus.STARTED) {
                        throw new Exception("Cannot leave your current alliance while a mission is in progress.");
                    }

                    return leaveAlliance(oldAllianceId, userId).continueWithTask(leaveTask -> {
                        if (!leaveTask.isSuccessful()) {
                            throw new Exception("Failed to leave your previous alliance.", leaveTask.getException());
                        }
                        return joinNewAlliance(invitation, userId);
                    });
                });
            });
        });
    }

    private Task<Void> joinNewAlliance(Invitation invitation, String userId) {
        String allianceId = invitation.getAllianceId();

        Task<Void> addMemberTask = allianceRepository.getAllianceById(allianceId)
                .continueWithTask(allianceTask -> {
                    if (!allianceTask.isSuccessful() || allianceTask.getResult() == null) {
                        throw new Exception("Alliance to join not found.", allianceTask.getException());
                    }
                    Alliance alliance = allianceTask.getResult();
                    List<String> members = new ArrayList<>(alliance.getMembersIds());
                    if (!members.contains(userId)) {
                        members.add(userId);
                    }
                    return allianceRepository.updateAllianceMembers(allianceId, members);
                });

        Task<Void> updateInvitationTask = invitationRepository.updateInvitationStatus(invitation.getInvitationId(), RequestStatus.ACCEPTED);
        Task<Void> updateUserTask = userRepository.updateUserAllianceId(userId, allianceId);

        return Tasks.whenAll(updateInvitationTask, addMemberTask, updateUserTask)
                .onSuccessTask(aVoid -> sendAcceptanceNotification(allianceId, userId));
    }

    private Task<Void> sendAcceptanceNotification(String allianceId, String newMemberId) {
        Task<Alliance> allianceTask = allianceRepository.getAllianceById(allianceId);
        Task<User> newMemberTask = userRepository.getUserById(newMemberId);

        return Tasks.whenAll(allianceTask, newMemberTask).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Failed to get data for acceptance notification.", task.getException());
                return Tasks.forResult(null);
            }

            Alliance alliance = allianceTask.getResult();
            User newMember = newMemberTask.getResult();

            if (alliance == null || newMember == null) return Tasks.forResult(null);

            return userRepository.getUserById(alliance.getLeaderId()).continueWithTask(leaderTask -> {
                if (!leaderTask.isSuccessful() || leaderTask.getResult() == null) {
                    Log.e(TAG, "Could not find leader to send notification.", leaderTask.getException());
                    return Tasks.forResult(null);
                }

                User leader = leaderTask.getResult();
                String leaderToken = leader.getFcmToken();
                if (leaderToken == null || leaderToken.isEmpty()) {
                    Log.e(TAG, "Leader does not have an FCM token.");
                    return Tasks.forResult(null);
                }

                try {
                    JSONObject notificationJson = new JSONObject();
                    notificationJson.put("title", "Invitation Accepted!");
                    notificationJson.put("body", newMember.getUsername() + " has joined your alliance, " + alliance.getName() + "!");

                    JSONObject dataJson = new JSONObject();
                    dataJson.put("type", "INVITATION_ACCEPTED");
                    dataJson.put("allianceName", alliance.getName());
                    dataJson.put("receiverName", newMember.getUsername());

                    JSONObject messageJson = new JSONObject();
                    messageJson.put("token", leaderToken);
                    messageJson.put("notification", notificationJson);
                    messageJson.put("data", dataJson);

                    JSONObject mainJson = new JSONObject();
                    mainJson.put("message", messageJson);

                    sendFcmRequest(mainJson);
                } catch (Exception e) {
                    Log.e(TAG, "Error creating acceptance notification JSON", e);
                }

                return Tasks.forResult(null);
            });
        });
    }

    public Task<Void> declineInvitation(String invitationId) {
        return invitationRepository.updateInvitationStatus(invitationId, RequestStatus.REJECTED);
    }

    public Task<Void> disbandAlliance(String allianceId, String leaderId) {
        return allianceRepository.getAllianceById(allianceId).continueWithTask(allianceTask -> {
            if (allianceTask.isSuccessful() && allianceTask.getResult() != null) {
                Alliance alliance = allianceTask.getResult();

                if (alliance == null || !alliance.getLeaderId().equals(leaderId)) {
                    return Tasks.forException(new Exception("Only the leader can disband the alliance."));
                }

                if (alliance.getMissionStatus() == MissionStatus.STARTED) {
                    return Tasks.forException(new Exception("Cannot disband alliance. A mission is currently in progress."));
                }

                List<String> memberIds = alliance.getMembersIds();
                List<Task<Void>> userUpdateTasks = new ArrayList<>();

                for (String memberId : memberIds) {
                    userUpdateTasks.add(userRepository.updateUserAllianceId(memberId, null));
                }

                Task<Void> deleteAllianceTask = allianceRepository.deleteAlliance(allianceId);

                userUpdateTasks.add(deleteAllianceTask);
                return Tasks.whenAll(userUpdateTasks);

            } else {
                return Tasks.forException(new Exception("Alliance not found."));
            }
        });
    }

    public Task<Void> leaveAlliance(String allianceId, String userId) {
        return allianceRepository.getAllianceById(allianceId).continueWithTask(allianceTask -> {
            if (allianceTask.isSuccessful() && allianceTask.getResult() != null) {
                Alliance alliance = allianceTask.getResult();

                if (alliance == null) {
                    return Tasks.forException(new Exception("Alliance not found."));
                }

                if (alliance.getLeaderId().equals(userId)) {
                    return Tasks.forException(new Exception("The leader cannot leave the alliance. Use the 'disband' option."));
                }

                if (alliance.getMissionStatus() == MissionStatus.STARTED) {
                    return Tasks.forException(new Exception("Cannot leave alliance. A mission is currently in progress."));
                }

                List<String> members = new ArrayList<>(alliance.getMembersIds());
                if (members.contains(userId)) {
                    members.remove(userId);
                    Task<Void> updateMembersTask = allianceRepository.updateAllianceMembers(allianceId, members);
                    Task<Void> updateUserTask = userRepository.updateUserAllianceId(userId, null);

                    return Tasks.whenAll(updateMembersTask, updateUserTask);
                } else {
                    return Tasks.forException(new Exception("User is not a member of this alliance."));
                }
            } else {
                return Tasks.forException(new Exception("Failed to fetch alliance data."));
            }
        });
    }

    public Task<List<String>> getPendingInvitedUserIds(String allianceId) {
        return invitationRepository.getPendingInvitationsForAlliance(allianceId)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    List<String> invitedIds = new ArrayList<>();
                    for (Invitation invitation : task.getResult().toObjects(Invitation.class)) {
                        invitedIds.add(invitation.getReceiverId());
                    }
                    return invitedIds;
                });
    }
}
