package com.team21.questify.data.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.team21.questify.application.model.Alliance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllianceRemoteDataSource {
    private final FirebaseFirestore db;

    public AllianceRemoteDataSource() {
        this.db = FirebaseFirestore.getInstance();
    }

    public Task<Void> saveAlliance(Alliance alliance) {
        return db.collection("alliances").document(alliance.getAllianceId()).set(alliance);
    }

    public Task<DocumentSnapshot> getAllianceById(String allianceId) {
        return db.collection("alliances").document(allianceId).get();
    }

    public Task<Void> updateAllianceMembers(String allianceId, List<String> membersIds) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("membersIds", membersIds);
        return db.collection("alliances").document(allianceId).set(updates, SetOptions.merge());
    }

    public Task<Void> deleteAlliance(String allianceId) {
        return db.collection("alliances").document(allianceId).delete();
    }

    public Task<String> getAllianceName(String allianceId) {
        return db.collection("alliances").document(allianceId).get().continueWith(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                return task.getResult().getString("name");
            }
            return "Unknown Alliance";
        });
    }

    public Task<List<String>> getAllianceMembersIds(String allianceId) {
        return db.collection("alliances").document(allianceId).get().continueWith(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Alliance alliance = task.getResult().toObject(Alliance.class);
                if (alliance != null) {
                    return alliance.getMembersIds();
                }
            }
            return new ArrayList<>();
        });
    }
}
