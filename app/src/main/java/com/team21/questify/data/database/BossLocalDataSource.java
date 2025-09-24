package com.team21.questify.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.team21.questify.application.model.Boss;

import java.util.ArrayList;
import java.util.List;

public class BossLocalDataSource {

    private final DatabaseHelper helper;

    public BossLocalDataSource(Context ctx) {
        this.helper = new DatabaseHelper(ctx);
    }

    /**
     * Ubacuje novog bosa u bazu podataka.
     * @param boss Objekat Boss koji se ubacuje.
     */
    public void insertBoss(Boss boss) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("id", boss.getBossId());
        cv.put("user_id", boss.getUserId());
        cv.put("max_hp", boss.getMaxHp());
        cv.put("current_hp", boss.getCurrentHp());
        cv.put("is_defeated", boss.getIsDefeated() ? 1 : 0);
        cv.put("level", boss.getLevel());


        db.insert(DatabaseHelper.T_BOSSES, null, cv);
        db.close();
    }

    /**
     * Pomoćna metoda koja konvertuje red iz Cursora u Boss objekat.
     * @param c Kursor pozicioniran na željeni red.
     * @return Popunjen Boss objekat.
     */
    private Boss cursorToBoss(Cursor c) {
        Boss boss = new Boss();
        boss.setBossId(c.getString(c.getColumnIndexOrThrow("id")));
        boss.setUserId(c.getString(c.getColumnIndexOrThrow("user_id")));
        boss.setMaxHp(c.getDouble(c.getColumnIndexOrThrow("max_hp")));
        boss.setCurrentHp(c.getDouble(c.getColumnIndexOrThrow("current_hp")));
        boss.setLevel(c.getInt(c.getColumnIndexOrThrow("level")));

        // Konvertujemo integer (0 ili 1) nazad u boolean
        int isDefeatedInt = c.getInt(c.getColumnIndexOrThrow("is_defeated"));
        boss.setIsDefeated(isDefeatedInt == 1);

        return boss;
    }

    /**
     * Vraća listu svih bosova za određenog korisnika.
     * @param userId ID korisnika.
     * @return Lista Boss objekata.
     */
    public List<Boss> getAllBossesForUser(String userId) {
        List<Boss> bossList = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.T_BOSSES, null,
                "user_id" + "=?", new String[]{userId},
                null, null, "level ASC"); // Sortirano po nivou
        if (c != null && c.moveToFirst()) {
            do {
                bossList.add(cursorToBoss(c));
            } while (c.moveToNext());
            c.close();
        }
        db.close();
        return bossList;
    }

    /**
     * Pronalazi prvog neporaženog bosa za korisnika, sortirano po nivou.
     * Ovo je ključna metoda za logiku "ponavljanja" bosa.
     * @param userId ID korisnika.
     * @return Boss objekat ako postoji neporaženi bos, inače null.
     */
    public Boss getNextUndefeatedBoss(String userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Boss boss = null;
        String selection = "user_id" + "=? AND " + "is_defeated" + "=?";
        String[] selectionArgs = {userId, "0"}; // "0" za false

        Cursor c = db.query(DatabaseHelper.T_BOSSES, null,
                selection, selectionArgs,
                null, null, "level ASC", "1"); // Uzmi samo prvog

        if (c != null && c.moveToFirst()) {
            boss = cursorToBoss(c);
            c.close();
        }
        db.close();
        return boss;
    }


    /**
     * Ažurira postojećeg bosa u bazi.
     * Najčešće će se koristiti za promenu currentHp i isDefeated statusa.
     * @param boss Objekat Boss sa ažuriranim podacima.
     */
    public void updateBoss(Boss boss) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();

        // Stavljamo polja koja se menjaju tokom igre
        cv.put("current_hp", boss.getCurrentHp());
        cv.put("is_defeated", boss.getIsDefeated() ? 1 : 0);

        String whereClause = "id = ?";
        String[] whereArgs = { boss.getBossId() };

        db.update(DatabaseHelper.T_BOSSES, cv, whereClause, whereArgs);
        db.close();
    }
}
