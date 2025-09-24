package com.team21.questify.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QuerySnapshot;
import com.team21.questify.application.model.SpecialMission;
import com.team21.questify.application.model.SpecialMissionUser;
import com.team21.questify.application.model.User; // Pretpostavka da imate User model
import com.team21.questify.application.model.enums.MissionStatus;
import com.team21.questify.data.database.SpecialMissionLocalDataSource;
import com.team21.questify.data.firebase.SpecialMissionRemoteDataSource;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SpecialMissionRepository {

    private final SpecialMissionLocalDataSource localDataSource;
    private final SpecialMissionRemoteDataSource remoteDataSource;
    private final Executor executor;

    public SpecialMissionRepository(Context context) {
        this.localDataSource = new SpecialMissionLocalDataSource(context);
        this.remoteDataSource = new SpecialMissionRemoteDataSource();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<SpecialMission> getActiveMissionForAlliance(String allianceId) {
        MutableLiveData<SpecialMission> missionLiveData = new MutableLiveData<>();


        executor.execute(() -> {
            SpecialMission cachedMission = localDataSource.getActiveMissionForAlliance(allianceId);
            if (cachedMission != null) {
                missionLiveData.postValue(cachedMission);
            }
        });


        remoteDataSource.getActiveMissionForAlliance(allianceId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {

                        SpecialMission remoteMission = queryDocumentSnapshots.getDocuments().get(0).toObject(SpecialMission.class);
                        missionLiveData.postValue(remoteMission);


                        executor.execute(() -> localDataSource.saveOrUpdateSpecialMission(remoteMission));
                    } else {

                        missionLiveData.postValue(null);
                    }
                });

        return missionLiveData;
    }


    public Task<Void> startSpecialMission(String allianceId, List<User> members) {

        SpecialMission newMission = new SpecialMission();
        newMission.setMissionId(UUID.randomUUID().toString());
        newMission.setAllianceId(allianceId);
        newMission.setMissionStatus(MissionStatus.STARTED);
        newMission.setInitialBossHp(100 * members.size());
        newMission.setCurrentBossHp(100 * members.size());

        long currentTime = System.currentTimeMillis();
        long twoWeeksInMillis = 14 * 24 * 60 * 60 * 1000L;
        newMission.setStartTime(currentTime);
        newMission.setEndTime(currentTime + twoWeeksInMillis);


        for (User member : members) {
            SpecialMissionUser userProgress = new SpecialMissionUser();
            userProgress.setUserId(member.getUserId());

            userProgress.setStorePurchases(5);
            userProgress.setSuccessfulRegularBossHits(10);
            userProgress.setSolvedVeryEasyEasyNormalOrImportantTasks(10);
            userProgress.setSolvedOtherTasks(6);

            newMission.getParticipantsProgress().put(member.getUserId(), userProgress);
        }


        Task<Void> remoteTask = remoteDataSource.saveOrUpdateMission(newMission);

        remoteTask.addOnSuccessListener(aVoid -> {

            executor.execute(() -> localDataSource.saveOrUpdateSpecialMission(newMission));
        });

        return remoteTask;
    }


    public Task<Void> updateMission(SpecialMission updatedMission) {

        Task<Void> remoteTask = remoteDataSource.saveOrUpdateMission(updatedMission);

        remoteTask.addOnSuccessListener(aVoid -> {
            executor.execute(() -> localDataSource.saveOrUpdateSpecialMission(updatedMission));
        });

        return remoteTask;
    }
}
