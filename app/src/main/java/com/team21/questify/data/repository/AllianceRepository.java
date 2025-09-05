package com.team21.questify.data.repository;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.team21.questify.application.model.Alliance;
import com.team21.questify.application.model.User;
import com.team21.questify.data.database.AllianceLocalDataSource;
import com.team21.questify.data.database.UserLocalDataSource;
import com.team21.questify.data.firebase.AllianceRemoteDataSource;
import com.team21.questify.data.firebase.UserRemoteDataSource;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AllianceRepository {
    private final AllianceRemoteDataSource allianceRemoteDataSource;
    private final AllianceLocalDataSource allianceLocalDataSource;
    private final UserRemoteDataSource userRemoteDataSource;
    private final UserLocalDataSource userLocalDataSource;
    private final FirebaseFirestore db;
    private final Executor executor;

    public AllianceRepository(Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.allianceRemoteDataSource = new AllianceRemoteDataSource();
        this.allianceLocalDataSource = new AllianceLocalDataSource(context);
        this.userRemoteDataSource = new UserRemoteDataSource();
        this.userLocalDataSource = new UserLocalDataSource(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public Task<Void> createAlliance(String allianceName, String leaderId) {
        return userRemoteDataSource.fetchUserFromFirestore(leaderId).continueWithTask(userTask -> {
            if (!userTask.isSuccessful() || userTask.getResult() == null) {
                throw userTask.getException() != null ? userTask.getException() : new Exception("User not found.");
            }
            User user = userTask.getResult().toObject(User.class);
            if (user != null && user.getCurrentAllianceId() != null && !user.getCurrentAllianceId().isEmpty()) {
                throw new Exception("User is already in an alliance.");
            }

            DocumentReference newAllianceRef = FirebaseFirestore.getInstance().collection("alliances").document();
            Alliance newAlliance = new Alliance(newAllianceRef.getId(), allianceName, leaderId);

            Task<Void> saveAllianceTask = allianceRemoteDataSource.saveAlliance(newAlliance);
            Task<Void> updateUserTask = userRemoteDataSource.updateUserAllianceId(leaderId, newAlliance.getAllianceId());

            return Tasks.whenAll(saveAllianceTask, updateUserTask)
                    .onSuccessTask(aVoid -> Tasks.call(executor, () -> {
                        userLocalDataSource.updateUserAllianceId(leaderId, newAlliance.getAllianceId());
                        allianceLocalDataSource.insertAlliance(newAlliance);
                        return null;
                    }));
        });
    }

    public Task<Void> addMember(String allianceId, String userId) {
        return allianceRemoteDataSource.getAllianceById(allianceId).continueWithTask(allianceTask -> {
            if (allianceTask.isSuccessful() && allianceTask.getResult() != null) {
                Alliance alliance = allianceTask.getResult().toObject(Alliance.class);

                assert alliance != null;
                List<String> members = alliance.getMembersIds();
                if (!members.contains(userId)) {
                    members.add(userId);
                    return allianceRemoteDataSource.updateAllianceMembers(allianceId, members)
                            .addOnSuccessListener(aVoid -> allianceLocalDataSource.updateAllianceMembers(allianceId, members));
                }
                return Tasks.forException(new Exception("User is already a member of this alliance."));
            } else {
                return Tasks.forException(new Exception("Alliance not found."));
            }
        });
    }

    public Task<Void> deleteAlliance(String allianceId) {
        return allianceRemoteDataSource.deleteAlliance(allianceId)
                .onSuccessTask(aVoid -> Tasks.call(executor, () -> {
                    allianceLocalDataSource.deleteAlliance(allianceId);
                    return null;
                }));
    }

    public Task<Alliance> getAllianceById(String allianceId) {
        return Tasks.call(executor, () -> allianceLocalDataSource.getAllianceById(allianceId))
                .continueWithTask(task -> {
                    Alliance localAlliance = task.getResult();
                    if (localAlliance != null) {
                        return Tasks.forResult(localAlliance);
                    }
                    return allianceRemoteDataSource.getAllianceById(allianceId)
                            .onSuccessTask(documentSnapshot -> {
                                if (documentSnapshot != null && documentSnapshot.exists()) {
                                    Alliance remoteAlliance = documentSnapshot.toObject(Alliance.class);
                                    return Tasks.call(executor, () -> {
                                        allianceLocalDataSource.insertAlliance(remoteAlliance);
                                        return remoteAlliance;
                                    });
                                } else {
                                    throw new Exception("Alliance not found.");
                                }
                            });
                });
    }

    public Task<Void> updateAllianceMembers(String allianceId, List<String> membersIds) {
        return allianceRemoteDataSource.updateAllianceMembers(allianceId, membersIds)
                .onSuccessTask(aVoid -> Tasks.call(executor, () -> {
                    allianceLocalDataSource.updateAllianceMembers(allianceId, membersIds);
                    return null;
                }));
    }
}
