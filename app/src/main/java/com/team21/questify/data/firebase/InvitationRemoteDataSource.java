package com.team21.questify.data.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.team21.questify.application.model.Invitation;
import com.team21.questify.application.model.enums.RequestStatus;

import java.util.HashMap;
import java.util.Map;

public class InvitationRemoteDataSource {
    private final FirebaseFirestore db;

    public InvitationRemoteDataSource() {
        this.db = FirebaseFirestore.getInstance();
    }

    public Task<Void> saveInvitation(Invitation invitation) {
        DocumentReference invitationRef = db.collection("invitations").document();
        invitation.setInvitationId(invitationRef.getId());
        return invitationRef.set(invitation);
    }

    public Task<DocumentSnapshot> getInvitationById(String invitationId) {
        return db.collection("invitations").document(invitationId).get();
    }

    public Task<Void> updateInvitationStatus(String invitationId, RequestStatus status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status.name());
        return db.collection("invitations").document(invitationId).update(updates);
    }

    public Task<QuerySnapshot> getPendingInvitationsForAlliance(String allianceId) {
        return db.collection("invitations")
                .whereEqualTo("allianceId", allianceId)
                .whereEqualTo("status", RequestStatus.PENDING.name())
                .get();
    }
}
