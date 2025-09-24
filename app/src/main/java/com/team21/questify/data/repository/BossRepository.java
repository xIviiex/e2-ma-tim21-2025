package com.team21.questify.data.repository;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.team21.questify.application.model.Boss;
import com.team21.questify.data.database.BossLocalDataSource;
import com.team21.questify.data.firebase.BossRemoteDataSource;

import java.util.ArrayList;
import java.util.List;

public class BossRepository {
    private final BossLocalDataSource localDataSource;
    private final BossRemoteDataSource remoteDataSource;

    public BossRepository(Context context) {
        this.localDataSource = new BossLocalDataSource(context);
        this.remoteDataSource = new BossRemoteDataSource();
    }


    public void createBoss(Boss boss, OnCompleteListener<Void> listener) {
        remoteDataSource.insertBoss(boss, task -> {
            if (!task.isSuccessful()) {
                Log.e("BossRepository", "Failed to insert boss to remote db: " + task.getException());
            }

            localDataSource.insertBoss(boss);
            listener.onComplete(task);
        });
    }


    public void updateBoss(Boss boss, OnCompleteListener<Void> listener) {
        remoteDataSource.updateBoss(boss, task -> {
            if (task.isSuccessful()) {

                localDataSource.updateBoss(boss);
            } else {
                Log.e("BossRepository", "Failed to update boss in remote db: " + task.getException());
            }
            listener.onComplete(task);
        });
    }


    public void getAllBossesForUser(String userId, OnCompleteListener<List<Boss>> listener) {
        List<Boss> localBosses = localDataSource.getAllBossesForUser(userId);
        if (!localBosses.isEmpty()) {
            listener.onComplete(Tasks.forResult(localBosses));
        }

        remoteDataSource.getAllBossesForUser(userId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Boss> remoteBosses = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Boss boss = document.toObject(Boss.class);
                    remoteBosses.add(boss);

                    localDataSource.insertBoss(boss);
                }
                listener.onComplete(Tasks.forResult(remoteBosses));
            } else {

                if (localBosses.isEmpty()) {
                    listener.onComplete(Tasks.forException(task.getException()));
                }
            }
        });
    }


    public void getNextUndefeatedBoss(String userId, OnCompleteListener<Boss> listener) {
        Boss localBoss = localDataSource.getNextUndefeatedBoss(userId);
        if (localBoss != null) {
            listener.onComplete(Tasks.forResult(localBoss));
            return;
        }

        remoteDataSource.getNextUndefeatedBoss(userId, task -> {
            if (task.isSuccessful()) {
                QuerySnapshot result = task.getResult();
                if (result != null && !result.isEmpty()) {
                    Boss remoteBoss = result.getDocuments().get(0).toObject(Boss.class);
                    if (remoteBoss != null) {
                        localDataSource.insertBoss(remoteBoss);
                        listener.onComplete(Tasks.forResult(remoteBoss));
                    } else {
                        listener.onComplete(Tasks.forException(new Exception("Failed to convert document to Boss.")));
                    }
                } else {

                    listener.onComplete(Tasks.forResult(null));
                }
            } else {
                listener.onComplete(Tasks.forException(task.getException() != null ? task.getException() : new Exception("Boss not found.")));
            }
        });
    }
}
