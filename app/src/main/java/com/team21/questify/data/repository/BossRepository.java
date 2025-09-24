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

    /**
     * Kreira novog bosa. Prvo na serveru, a zatim sinhronizuje sa lokalnom bazom.
     * @param boss Objekat Boss koji se kreira.
     * @param listener Listener koji se poziva po završetku remote operacije.
     */
    public void createBoss(Boss boss, OnCompleteListener<Void> listener) {
        remoteDataSource.insertBoss(boss, task -> {
            if (!task.isSuccessful()) {
                Log.e("BossRepository", "Failed to insert boss to remote db: " + task.getException());
            }
            // Sinhronizuj sa lokalnom bazom bez obzira na ishod remote operacije.
            // NAPOMENA: DB operacije bi trebalo izvršavati na pozadinskoj niti.
            localDataSource.insertBoss(boss);
            listener.onComplete(task);
        });
    }

    /**
     * Ažurira postojećeg bosa. Prvo na serveru, a zatim sinhronizuje sa lokalnom bazom.
     * @param boss Objekat Boss sa ažuriranim podacima.
     * @param listener Listener koji se poziva po završetku remote operacije.
     */
    public void updateBoss(Boss boss, OnCompleteListener<Void> listener) {
        remoteDataSource.updateBoss(boss, task -> {
            if (task.isSuccessful()) {
                // Ako je remote operacija uspela, ažuriraj i lokalnu bazu.
                localDataSource.updateBoss(boss);
            } else {
                Log.e("BossRepository", "Failed to update boss in remote db: " + task.getException());
            }
            listener.onComplete(task);
        });
    }

    /**
     * Vraća sve bosove za korisnika. Prvo vraća podatke iz lokalne baze za brzi prikaz,
     * a zatim dohvata sveže podatke sa servera i sinhronizuje ih.
     * @param userId ID korisnika.
     * @param listener Listener koji može biti pozvan dva puta: prvo sa lokalnim, a zatim sa remote podacima.
     */
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
                    // Sinhronizacija u lokalnu bazu (upsert - update or insert).
                    // Pretpostavka je da insertBoss rukuje konfliktima.
                    localDataSource.insertBoss(boss);
                }
                listener.onComplete(Tasks.forResult(remoteBosses));
            } else {
                // Ako remote ne uspe, a lokalna baza je bila prazna, javi grešku.
                if (localBosses.isEmpty()) {
                    listener.onComplete(Tasks.forException(task.getException()));
                }
            }
        });
    }

    /**
     * Vraća sledećeg neporaženog bosa. Prvo proverava lokalnu bazu.
     * Ako ga ne nađe, traži na serveru.
     * @param userId ID korisnika.
     * @param listener Listener koji vraća pronađenog bosa ili null.
     */
    public void getNextUndefeatedBoss(String userId, OnCompleteListener<Boss> listener) {
        Boss localBoss = localDataSource.getNextUndefeatedBoss(userId);
        if (localBoss != null) {
            listener.onComplete(Tasks.forResult(localBoss));
            return; // Pronađen lokalno, ne nastavljaj na remote.
        }

        remoteDataSource.getNextUndefeatedBoss(userId, task -> {
            if (task.isSuccessful()) {
                QuerySnapshot result = task.getResult();
                if (result != null && !result.isEmpty()) {
                    Boss remoteBoss = result.getDocuments().get(0).toObject(Boss.class);
                    if (remoteBoss != null) {
                        localDataSource.insertBoss(remoteBoss); // Sinhronizuj pronađenog bosa
                        listener.onComplete(Tasks.forResult(remoteBoss));
                    } else {
                        listener.onComplete(Tasks.forException(new Exception("Failed to convert document to Boss.")));
                    }
                } else {
                    // Nema neporaženih bosova ni na serveru.
                    listener.onComplete(Tasks.forResult(null));
                }
            } else {
                listener.onComplete(Tasks.forException(task.getException() != null ? task.getException() : new Exception("Boss not found.")));
            }
        });
    }
}
