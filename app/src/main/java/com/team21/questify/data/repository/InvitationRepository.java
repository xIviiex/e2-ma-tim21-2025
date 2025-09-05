package com.team21.questify.data.repository;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.team21.questify.application.model.Invitation;
import com.team21.questify.application.model.enums.RequestStatus;
import com.team21.questify.data.database.InvitationLocalDataSource;
import com.team21.questify.data.firebase.InvitationRemoteDataSource;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class InvitationRepository {
    private final InvitationRemoteDataSource remoteDataSource;
    private final InvitationLocalDataSource localDataSource;
    private final Executor executor;

    public InvitationRepository(Context context) {
        this.remoteDataSource = new InvitationRemoteDataSource();
        this.localDataSource = new InvitationLocalDataSource(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public Task<Void> saveInvitation(Invitation invitation) {
        return remoteDataSource.saveInvitation(invitation)
                .onSuccessTask(aVoid -> Tasks.call(executor, () -> {
                    localDataSource.insertInvitation(invitation);
                    return null;
                }));
    }

    public Task<DocumentSnapshot> getInvitationById(String invitationId) {
        return remoteDataSource.getInvitationById(invitationId);
    }

    public Task<Void> updateInvitationStatus(String invitationId, RequestStatus status) {
        return remoteDataSource.updateInvitationStatus(invitationId, status)
                .onSuccessTask(aVoid -> Tasks.call(executor, () -> {
                    localDataSource.updateInvitationStatus(invitationId, status);
                    return null;
                }));
    }

    public Task<QuerySnapshot> getPendingInvitationsForAlliance(String allianceId) {
        return remoteDataSource.getPendingInvitationsForAlliance(allianceId);
    }
}
