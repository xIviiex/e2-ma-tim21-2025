package com.team21.questify.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.team21.questify.application.model.User;

public class UserLocalDataSource {
    private final DatabaseHelper helper;

    public UserLocalDataSource(Context ctx) {
        this.helper = new DatabaseHelper(ctx);
    }
    public void insertUser(User u) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id", u.getUserId());
        cv.put("email", u.getEmail());
        cv.put("username", u.getUsername());
        cv.put("avatar_name", u.getAvatarName());
        cv.put("is_activated", u.isActivated() ? 1 : 0);
        cv.put("created_at", u.getCreatedAt());
        cv.put("level", u.getLevel());
        cv.put("xp", u.getXp());
        db.insert(DatabaseHelper.T_USERS, null, cv);
        db.close();
    }

    public User getUserByEmail(String email) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.T_USERS, null,
                "email" + "=?", new String[]{email},
                null, null, null);
        User user = null;
        if (c != null && c.moveToFirst()) {
            user = cursorToUser(c);
            c.close();
        }
        db.close();
        return user;
    }

    public void updateActivatedFlag(String userId, boolean activated) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_activated", activated ? 1 : 0);
        db.update(DatabaseHelper.T_USERS, values,
                "id" + " = ?", new String[]{userId});
        db.close();
    }

    private User cursorToUser(Cursor c) {
        User u = new User();
        u.setUserId(c.getString(c.getColumnIndexOrThrow("id")));
        u.setEmail(c.getString(c.getColumnIndexOrThrow("email")));
        u.setUsername(c.getString(c.getColumnIndexOrThrow("username")));
        u.setAvatarName(c.getString(c.getColumnIndexOrThrow("avatar_name")));
        u.setIsActivated(c.getInt(c.getColumnIndexOrThrow("is_activated")) == 1);
        u.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        u.setXp(c.getInt(c.getColumnIndexOrThrow("xp")));
        u.setLevel(c.getInt(c.getColumnIndexOrThrow("level")));
        return u;
    }
}
