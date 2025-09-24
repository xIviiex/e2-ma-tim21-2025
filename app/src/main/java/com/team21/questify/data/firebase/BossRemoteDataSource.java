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

    /**
     * Ubacuje novog bosa u Firestore. Ako dokument sa istim ID-jem već postoji, biće pregažen.
     * @param boss Objekat Boss koji se ubacuje.
     * @param listener Listener koji se poziva po završetku operacije.
     */
    public void insertBoss(Boss boss, OnCompleteListener<Void> listener) {
        if (boss.getBossId() != null) {
            db.collection(BOSSES_COLLECTION)
                    .document(boss.getBossId())
                    .set(boss)
                    .addOnCompleteListener(listener);
        }
    }

    /**
     * Vraća sve bosove povezane sa određenim korisnikom.
     * @param userId ID korisnika.
     * @param listener Listener koji vraća QuerySnapshot sa rezultatima.
     */
    public void getAllBossesForUser(String userId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection(BOSSES_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("level", Query.Direction.ASCENDING) // Sortiramo po nivou
                .get()
                .addOnCompleteListener(listener);
    }

    /**
     * Vraća prvog neporaženog bosa za korisnika (sa najnižim nivoom).
     * @param userId ID korisnika.
     * @param listener Listener koji vraća QuerySnapshot. Rezultat može biti prazan ili sadržati jednog bosa.
     */
    public void getNextUndefeatedBoss(String userId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection(BOSSES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDefeated", false) // Tražimo samo neporažene
                .orderBy("level", Query.Direction.ASCENDING) // Uzimamo onog sa najnižim nivoom
                .limit(1) // Ograničavamo na samo jedan rezultat
                .get()
                .addOnCompleteListener(listener);
    }

    /**
     * Ažurira podatke o bosu. Korisno za promenu HP-a i statusa nakon borbe.
     * @param boss Objekat Boss čije podatke ažuriramo.
     * @param listener Listener koji se poziva po završetku operacije.
     */
    public void updateBoss(Boss boss, OnCompleteListener<Void> listener) {
        if (boss.getBossId() == null) return;

        // Ažuriramo samo polja koja se menjaju tokom borbe
        Map<String, Object> updates = new HashMap<>();
        updates.put("currentHp", boss.getCurrentHp());
        updates.put("isDefeated", boss.getIsDefeated());

        db.collection(BOSSES_COLLECTION)
                .document(boss.getBossId())
                .update(updates)
                .addOnCompleteListener(listener);
    }

    /**
     * Briše bosa iz baze podataka.
     * @param bossId ID bosa koji se briše.
     * @param listener Listener koji se poziva po završetku operacije.
     */
    public void deleteBoss(String bossId, OnCompleteListener<Void> listener) {
        db.collection(BOSSES_COLLECTION)
                .document(bossId)
                .delete()
                .addOnCompleteListener(listener);
    }
}
