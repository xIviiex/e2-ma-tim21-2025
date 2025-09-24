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
import com.team21.questify.application.model.enums.FinalizationOutcome;
import com.team21.questify.application.model.enums.MissionActionType;
import com.team21.questify.application.model.enums.Badge;
import com.team21.questify.application.model.enums.MissionStatus;
import com.team21.questify.data.repository.SpecialMissionRepository;
import com.team21.questify.data.repository.UserRepository; // Potrebno za dohvatanje članova

import java.util.ArrayList;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpecialMissionService {

    private final SpecialMissionRepository repository;
    private final AllianceService allianceService;
    private final UserRepository userRepository;
    private final FirebaseAuth auth;
    private final RewardService rewardService;
    private final TaskOccurrenceService taskOccurrenceService;
    public SpecialMissionService(Context context) {
        this.repository = new SpecialMissionRepository(context);
        this.allianceService = new AllianceService(context);
        this.userRepository = new UserRepository(context);
        this.auth = FirebaseAuth.getInstance();
        this.rewardService = new RewardService(context);
        this.taskOccurrenceService = new TaskOccurrenceService(context);
    }


    public void getActiveMissionForCurrentAlliance(@NonNull OnCompleteListener<SpecialMission> listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            listener.onComplete(Tasks.forException(new Exception("User not authenticated.")));
            return;
        }



        userRepository.getUserById(currentUser.getUid()).continueWithTask(userTask -> {
            if (!userTask.isSuccessful() || userTask.getResult() == null) {
                throw new Exception("Failed to fetch user profile.");
            }
            User user = userTask.getResult();
            if (user.getCurrentAllianceId() == null || user.getCurrentAllianceId().isEmpty()) {

                return Tasks.forResult(null);
            }


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
            }).addOnCompleteListener(listener);
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



    public void recordUserAction(MissionActionType actionType, int amount, @NonNull OnCompleteListener<Void> listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            listener.onComplete(Tasks.forException(new Exception("User not authenticated.")));
            return;
        }
        String userId = currentUser.getUid();

        getActiveMissionForCurrentAlliance(missionTask -> {
            if (!missionTask.isSuccessful()) {
                listener.onComplete(Tasks.forException(new Exception("Failed to get active mission.")));
                return;
            }
            SpecialMission mission = missionTask.getResult();
            if (mission == null || mission.getMissionStatus() != MissionStatus.STARTED) {

                listener.onComplete(Tasks.forResult(null));
                return;
            }

            SpecialMissionUser userProgress = mission.getParticipantsProgress().get(userId);
            if (userProgress == null) {
                listener.onComplete(Tasks.forException(new Exception("User is not a participant in this mission.")));
                return;
            }

            int damageDealt = 0;
            boolean requiresUpdate = false;

            switch (actionType) {
                case STORE_PURCHASE:
                    if (userProgress.getStorePurchases() > 0) {
                        userProgress.setStorePurchases(userProgress.getStorePurchases() - 1);
                        damageDealt = 2;
                        requiresUpdate = true;
                    }
                    break;

                case REGULAR_BOSS_HIT:
                    if (userProgress.getSuccessfulRegularBossHits() > 0) {
                        userProgress.setSuccessfulRegularBossHits(userProgress.getSuccessfulRegularBossHits() - 1);
                        damageDealt = 2;
                        requiresUpdate = true;
                    }
                    break;

                case SOLVED_PRIMARY_TASK:
                    if (userProgress.getSolvedVeryEasyEasyNormalOrImportantTasks() > 0) {
                        int currentRemaining = userProgress.getSolvedVeryEasyEasyNormalOrImportantTasks();
                        int tasksToComplete = Math.min(currentRemaining, amount);

                        userProgress.setSolvedVeryEasyEasyNormalOrImportantTasks(currentRemaining - tasksToComplete);
                        damageDealt = tasksToComplete * 1; // 1 HP štete po urađenom zadatku
                        requiresUpdate = true;
                    }
                    break;

                case SOLVED_OTHER_TASK:
                    if (userProgress.getSolvedOtherTasks() > 0) {
                        userProgress.setSolvedOtherTasks(userProgress.getSolvedOtherTasks() - 1);
                        damageDealt = 4;
                        requiresUpdate = true;
                    }
                    break;

                case NO_UNSOLVED_TASKS_BONUS:
                    if (!userProgress.isHasNoUnsolvedTasks()) {
                        userProgress.setHasNoUnsolvedTasks(true);
                        damageDealt = 10;
                        requiresUpdate = true;
                    }
                    break;

                case SENT_ALLIANCE_MESSAGE:
                    long todayStartTimestamp = getStartOfDayInMillis();
                    if (!userProgress.getDaysWithMessageSent().contains(todayStartTimestamp)) {
                        userProgress.getDaysWithMessageSent().add(todayStartTimestamp);
                        damageDealt = 4;
                        requiresUpdate = true;
                    }
                    break;
            }

            if (requiresUpdate) {

                mission.setCurrentBossHp(Math.max(0, mission.getCurrentBossHp() - damageDealt));
                userProgress.setTotalDamageContributed(userProgress.getTotalDamageContributed() + damageDealt);


                if (mission.getCurrentBossHp() == 0) {
                    grantRewardsAndFinalizeMission(mission).addOnCompleteListener(listener);
                } else {

                    repository.updateMission(mission).addOnCompleteListener(listener);
                }
            } else {
                listener.onComplete(Tasks.forResult(null));
            }
        });
    }

    private Task<Void> grantRewardsAndFinalizeMission(@NonNull SpecialMission mission) {
        List<Task<Void>> allRewardTasks = new ArrayList<>();

        for (SpecialMissionUser progress : mission.getParticipantsProgress().values()) {

            allRewardTasks.add(rewardService.grantMissionRewards(progress.getUserId()));


            if (progress.getTotalDamageContributed() < 10 ) {
                progress.getEarnedBadges().add(Badge.BRONZE_PARTICIPANT);
            }
            if (progress.getTotalDamageContributed() >= 10 && progress.getTotalDamageContributed() < 20 ) {
                progress.getEarnedBadges().add(Badge.SILVER_CONTRIBUTOR);
            }
            if (progress.getTotalDamageContributed() >= 20 && progress.getTotalDamageContributed() < 30 ) {
                progress.getEarnedBadges().add(Badge.GOLD_CONTRIBUTOR);
            }
            if (progress.getTotalDamageContributed() >= 30 && progress.getTotalDamageContributed() < 100 ) {
                progress.getEarnedBadges().add(Badge.MISSION_MASTER);
            }


        }


        return Tasks.whenAll(allRewardTasks).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw new Exception("Failed to grant rewards.", task.getException());
            }


            mission.setMissionStatus(MissionStatus.COMPLETED);
            return repository.updateMission(mission);
        });
    }
    public void recordUserAction(MissionActionType actionType, @NonNull OnCompleteListener<Void> listener) {
        recordUserAction(actionType, 1, listener);
    }


    private long getStartOfDayInMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }



    public void checkAndFinalizeExpiredMission(@NonNull OnCompleteListener<FinalizationOutcome> listener) {
        getActiveMissionForCurrentAlliance(missionTask -> {
            if (!missionTask.isSuccessful()) {

                listener.onComplete(Tasks.forException(missionTask.getException()));
                return;
            }

            SpecialMission mission = missionTask.getResult();

            if (mission == null || mission.getMissionStatus() != MissionStatus.STARTED || System.currentTimeMillis() <= mission.getEndTime()) {

                listener.onComplete(Tasks.forResult(FinalizationOutcome.NO_ACTION_NEEDED));
                return;
            }


            List<Task<Void>> participantChecks = new ArrayList<>();
            for (SpecialMissionUser progress : mission.getParticipantsProgress().values()) {
                Task<Void> checkTask = taskOccurrenceService.getUncompletedOccurrencesCountInDateRange(
                        progress.getUserId(), mission.getStartTime(), mission.getEndTime()
                ).onSuccessTask(uncompletedCount -> {
                    if (uncompletedCount == 0) {
                        progress.setHasNoUnsolvedTasks(true);
                    }
                    return Tasks.forResult(null);
                });
                participantChecks.add(checkTask);
            }

            Tasks.whenAll(participantChecks).continueWithTask(allChecksTask -> {
                if (!allChecksTask.isSuccessful()) {
                    throw new Exception("Failed to check tasks.", allChecksTask.getException());
                }

                if (mission.getCurrentBossHp() <= 0) {

                    return grantRewardsAndFinalizeMission(mission)
                            .continueWith(task -> FinalizationOutcome.REWARDS_GRANTED);
                } else {

                    mission.setMissionStatus(MissionStatus.FAILED);
                    return repository.updateMission(mission)
                            .continueWith(task -> FinalizationOutcome.MISSION_FAILED);
                }
            }).addOnCompleteListener(listener);
        });
    }
}