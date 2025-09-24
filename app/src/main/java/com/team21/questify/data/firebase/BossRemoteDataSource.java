package com.team21.questify.data.firebase;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.team21.questify.application.model.Boss;

import java.util.HashMap;
import java.util.Map;

public class BossRemoteDataSource {
    private final FirebaseFirestore db;
    private static final String BOSSES_COLLECTION = "bosses";

    public BossRemoteDataSource() {
        this.db = FirebaseFirestore.getInstance();
    }


    public void insertBoss(Boss boss, OnCompleteListener<Void> listener) {
        if (boss.getBossId() != null) {
            db.collection(BOSSES_COLLECTION)
                    .document(boss.getBossId())
                    .set(boss)
                    .addOnCompleteListener(listener);
        }
    }


    public void getAllBossesForUser(String userId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection(BOSSES_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("level", Query.Direction.ASCENDING) // Sortiramo po nivou
                .get()
                .addOnCompleteListener(listener);
    }


    public void getNextUndefeatedBoss(String userId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection(BOSSES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDefeated", false) // Tražimo samo neporažene
                .orderBy("level", Query.Direction.ASCENDING) // Uzimamo onog sa najnižim nivoom
                .limit(1) // Ograničavamo na samo jedan rezultat
                .get()
                .addOnCompleteListener(listener);
    }


    public void updateBoss(Boss boss, OnCompleteListener<Void> listener) {
        if (boss.getBossId() == null) return;


        Map<String, Object> updates = new HashMap<>();
        updates.put("currentHp", boss.getCurrentHp());
        updates.put("isDefeated", boss.getIsDefeated());

        db.collection(BOSSES_COLLECTION)
                .document(boss.getBossId())
                .update(updates)
                .addOnCompleteListener(listener);
    }


    public void deleteBoss(String bossId, OnCompleteListener<Void> listener) {
        db.collection(BOSSES_COLLECTION)
                .document(bossId)
                .delete()
                .addOnCompleteListener(listener);
    }
}
