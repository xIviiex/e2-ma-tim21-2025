package com.team21.questify.application.service;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.team21.questify.application.model.Alliance;
import com.team21.questify.application.model.SpecialMission;
import com.team21.questify.application.model.SpecialMissionUser;
import com.team21.questify.application.model.User;
import com.team21.questify.application.model.enums.Badge;
import com.team21.questify.application.model.enums.MissionStatus;
import com.team21.questify.data.repository.SpecialMissionRepository;
import com.team21.questify.data.repository.UserRepository; // Potrebno za dohvatanje članova

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpecialMissionService {

    private final SpecialMissionRepository repository;
    private final AllianceService allianceService;
    private final UserRepository userRepository;
    private final FirebaseAuth auth;

    public SpecialMissionService(Context context) {
        this.repository = new SpecialMissionRepository(context);
        this.allianceService = new AllianceService(context);
        this.userRepository = new UserRepository(context);
        this.auth = FirebaseAuth.getInstance();
    }


    public void getActiveMissionForCurrentAlliance(@NonNull OnCompleteListener<SpecialMission> listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            listener.onComplete(Tasks.forException(new Exception("User not authenticated.")));
            return;
        }


        // 1. Dohvati ID alijanse trenutnog korisnika
        userRepository.getUserById(currentUser.getUid()).continueWithTask(userTask -> {
            if (!userTask.isSuccessful() || userTask.getResult() == null) {
                throw new Exception("Failed to fetch user profile.");
            }
            User user = userTask.getResult();
            if (user.getCurrentAllianceId() == null || user.getCurrentAllianceId().isEmpty()) {
                // Korisnik nije u alijansi, pa ne može biti ni misije
                return Tasks.forResult(null);
            }

            // 2. Dohvati misiju koristeći Repository (koji interno koristi LiveData, ali mi ga ovde adaptiramo)
            // U realnoj aplikaciji, Repository bi takođe nudio Task-based metodu
            return convertLiveDataToTask(repository.getActiveMissionForAlliance(user.getCurrentAllianceId()));

        }).addOnCompleteListener(listener);
    }


    public void startMissionForAlliance(String allianceId, @NonNull OnCompleteListener<Void> listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            listener.onComplete(Tasks.forException(new Exception("User not authenticated.")));
            return;
        }


        getActiveMissionForCurrentAlliance(missionTask -> {
            if (!missionTask.isSuccessful()) {

                listener.onComplete(Tasks.forException(missionTask.getException() != null ? missionTask.getException() : new Exception("Failed to check for active missions.")));
                return;
            }

            SpecialMission activeMission = missionTask.getResult();


            if (activeMission != null && activeMission.getMissionStatus() == MissionStatus.STARTED) {

                listener.onComplete(Tasks.forException(new Exception("A mission is already in progress for this alliance.")));
                return;
            }


            allianceService.getAllianceById(allianceId).continueWithTask(allianceTask -> {
                if (!allianceTask.isSuccessful() || allianceTask.getResult() == null) {
                    throw new Exception("Alliance not found with the given ID.");
                }
                Alliance currentAlliance = allianceTask.getResult();

                if (!currentAlliance.getLeaderId().equals(currentUser.getUid())) {
                    throw new Exception("Only the alliance leader can start a mission.");
                }



                return userRepository.getUsersByIds(currentAlliance.getMembersIds()).continueWithTask(membersTask -> {
                    if (!membersTask.isSuccessful()) {
                        throw new Exception("Failed to fetch alliance members.", membersTask.getException());
                    }
                    List<User> members = membersTask.getResult();
                    return repository.startSpecialMission(currentAlliance.getAllianceId(), members);
                });
            }).addOnCompleteListener(listener); // Na kraju lanca pozivamo originalni listener
        });
    }


    public void updateMission(SpecialMission mission, @NonNull OnCompleteListener<Void> listener) {
        if (auth.getCurrentUser() == null) {
            listener.onComplete(Tasks.forException(new Exception("User not authenticated.")));
            return;
        }
        repository.updateMission(mission).addOnCompleteListener(listener);
    }




    private <T> Task<T> convertLiveDataToTask(final LiveData<T> liveData) {
        final com.google.android.gms.tasks.TaskCompletionSource<T> taskCompletionSource = new com.google.android.gms.tasks.TaskCompletionSource<>();
        liveData.observeForever(new androidx.lifecycle.Observer<T>() {
            @Override
            public void onChanged(T t) {
                taskCompletionSource.setResult(t);
                liveData.removeObserver(this);
            }
        });
        return taskCompletionSource.getTask();
    }

    public Task<List<Badge>> getAllEarnedBadges(String userId) {
        return userRepository.getUserById(userId).continueWithTask(userTask -> {
            if (!userTask.isSuccessful() || userTask.getResult() == null) {
                return Tasks.forResult(new ArrayList<>());
            }

            User user = userTask.getResult();
            String allianceId = user.getCurrentAllianceId();

            if (allianceId == null || allianceId.isEmpty()) {
                return Tasks.forResult(new ArrayList<>());
            }

            return repository.getMissionsForAlliance(allianceId)
                    .continueWith(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        List<Badge> allBadges = new ArrayList<>();
                        List<SpecialMission> missions = task.getResult();

                        for (SpecialMission mission : missions) {
                            Map<String, SpecialMissionUser> participants = mission.getParticipantsProgress();
                            if (participants != null && participants.containsKey(userId)) {
                                SpecialMissionUser userProgress = participants.get(userId);
                                if (userProgress != null && userProgress.getEarnedBadges() != null) {
                                    allBadges.addAll(userProgress.getEarnedBadges());
                                }
                            }
                        }

                        return allBadges.stream().distinct().collect(Collectors.toList());
                    });
        });
    }
}