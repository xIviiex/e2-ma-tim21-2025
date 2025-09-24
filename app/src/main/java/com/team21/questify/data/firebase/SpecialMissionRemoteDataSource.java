package com.team21.questify.data.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.team21.questify.application.model.SpecialMission;
import com.team21.questify.application.model.enums.MissionStatus;

public class SpecialMissionRemoteDataSource {

    private final FirebaseFirestore db;
    private static final String SPECIAL_MISSIONS_COLLECTION = "special_missions";

    public SpecialMissionRemoteDataSource() {
        this.db = FirebaseFirestore.getInstance();
    }


    private CollectionReference getMissionsCollection() {
        return db.collection(SPECIAL_MISSIONS_COLLECTION);
    }


    public Task<Void> saveOrUpdateMission(SpecialMission mission) {
        // Koristimo mission.getMissionId() da ciljamo specifičan dokument.
        return getMissionsCollection().document(mission.getMissionId()).set(mission);
    }


    public Task<QuerySnapshot> getActiveMissionForAlliance(String allianceId) {
        // Pravimo upit koji traži misije za datu alijansu I koje su u toku.
        return getMissionsCollection()
                .whereEqualTo("allianceId", allianceId)
                .whereEqualTo("missionStatus", MissionStatus.STARTED.name())
                .limit(1)
                .get();
    }


    public Task<Void> deleteMission(String missionId) {
        return getMissionsCollection().document(missionId).delete();
    }

    public Task<QuerySnapshot> getCompletedMissionsForAlliance(String allianceId) {
        return getMissionsCollection()
                .whereEqualTo("allianceId", allianceId)
                .whereEqualTo("missionStatus", MissionStatus.COMPLETED.name())
                .get();
    }

    public Task<QuerySnapshot> getStartedMissionsForAlliance(String allianceId) {
        return getMissionsCollection()
                .whereEqualTo("allianceId", allianceId)
                .whereEqualTo("missionStatus", MissionStatus.STARTED.name())
                .get();
    }

    public Task<QuerySnapshot> getMissionsForUser(String allianceId) {
        return getMissionsCollection()
                .whereEqualTo("allianceId", allianceId)
                .get();
    }
}
