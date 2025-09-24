package com.team21.questify.application.service;

import android.content.Context;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.team21.questify.application.model.Boss;
import com.team21.questify.application.model.User;
import com.team21.questify.data.repository.BossRepository;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class BossService {

    private final BossRepository repository;
    private final FirebaseAuth auth;
    private final UserService userService;

    private static final double FIRST_BOSS_HP = 200.0;
    public BossService(Context context) {
        this.repository = new BossRepository(context);
        this.auth = FirebaseAuth.getInstance();
        this.userService = new UserService(context);
    }


    public void getBossForNextFight(OnCompleteListener<Boss> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onComplete(Tasks.forException(new Exception("User not authenticated.")));
            return;
        }


        repository.getNextUndefeatedBoss(user.getUid(), task -> {
            if (task.isSuccessful()) {
                Boss undefeatedBoss = task.getResult();
                if (undefeatedBoss != null) {

                    listener.onComplete(Tasks.forResult(undefeatedBoss));
                } else {

                    generateNewBossForCurrentUser(user.getUid(), listener);
                }
            } else {
                listener.onComplete(Tasks.forException(task.getException()));
            }
        });
    }


    private void generateNewBossForCurrentUser(String userId, OnCompleteListener<Boss> listener) {

        repository.getAllBossesForUser(userId, allBossesTask -> {
            if (!allBossesTask.isSuccessful()) {
                listener.onComplete(Tasks.forException(allBossesTask.getException()));
                return;
            }

            List<Boss> allBosses = allBossesTask.getResult();
            Boss lastDefeatedBoss = allBosses.stream()
                    .filter(Boss::getIsDefeated)
                    .max(Comparator.comparingInt(Boss::getLevel))
                    .orElse(null);


            userService.fetchUserProfile(userId).addOnCompleteListener(userTask -> {
                if (!userTask.isSuccessful() || userTask.getResult() == null) {
                    listener.onComplete(Tasks.forException(new Exception("Failed to fetch user profile.")));
                    return;
                }
                User currentUser = userTask.getResult();


                boolean bossForCurrentLevelExists = allBosses.stream()
                        .anyMatch(boss -> boss.getLevel() == currentUser.getLevel());

                if (bossForCurrentLevelExists) {

                    listener.onComplete(Tasks.forResult(null));
                    return;
                }



                Boss newBoss = createBossObject(userId, currentUser.getLevel(), lastDefeatedBoss);


                repository.createBoss(newBoss, createBossTask -> {
                    if (createBossTask.isSuccessful()) {
                        listener.onComplete(Tasks.forResult(newBoss));
                    } else {
                        listener.onComplete(Tasks.forException(createBossTask.getException()));
                    }
                });
            });
        });
    }


    private Boss createBossObject(String userId, int currentUserLevel, Boss previousBoss) {

        Boss newBoss = new Boss();
        newBoss.setBossId(UUID.randomUUID().toString());
        newBoss.setUserId(userId);
        newBoss.setIsDefeated(false);
        newBoss.setLevel(currentUserLevel);

        if (previousBoss == null) {
            newBoss.setMaxHp(FIRST_BOSS_HP);
        } else {
            double previousHp = previousBoss.getMaxHp();
            double newHp = previousHp * 2 + previousHp / 2;
            newBoss.setMaxHp(newHp);

        }
        newBoss.setCurrentHp(newBoss.getMaxHp());
        return newBoss;
    }



    public void updateBoss(Boss boss, OnCompleteListener<Void> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onComplete(Tasks.forException(new Exception("User not authenticated.")));
            return;
        }
        repository.updateBoss(boss, listener);
    }


    public void getAllBossesForCurrentUser(OnCompleteListener<List<Boss>> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            repository.getAllBossesForUser(userId, listener);
        } else {
            listener.onComplete(Tasks.forException(new Exception("User not authenticated.")));
        }
    }


    public void getNextUndefeatedBossForCurrentUser(OnCompleteListener<Boss> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            repository.getNextUndefeatedBoss(userId, listener);
        } else {
            listener.onComplete(Tasks.forException(new Exception("User not authenticated.")));
        }
    }
}
