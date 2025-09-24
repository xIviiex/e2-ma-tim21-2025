package com.team21.questify.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import com.team21.questify.application.model.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserLocalDataSource {
    private final DatabaseHelper helper;

    public UserLocalDataSource(Context ctx) {
        this.helper = new DatabaseHelper(ctx);
    }

    public void insertUser(User u) {
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            ContentValues cv = userToContentValues(u);
            cv.put("id", u.getUserId());
            db.insertWithOnConflict(DatabaseHelper.T_USERS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    public User getUserByEmail(String email) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        User user = null;
        try {
            c = db.query(DatabaseHelper.T_USERS, null, "email=?", new String[]{email}, null, null, null);
            if (c != null && c.moveToFirst()) {
                user = cursorToUser(c);
            }
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return user;
    }

    public User getUserById(String userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        User user = null;
        try {
            c = db.query(DatabaseHelper.T_USERS, null, "id=?", new String[]{userId}, null, null, null);
            if (c != null && c.moveToFirst()) {
                user = cursorToUser(c);
            }
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return user;
    }

    public void updateUser(User u) {
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            ContentValues cv = userToContentValues(u);
            db.update(DatabaseHelper.T_USERS, cv, "id=?", new String[]{u.getUserId()});
        } finally {
            if (db != null) db.close();
        }
    }

    public void deleteUser(String userId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            db.delete(DatabaseHelper.T_USERS, "id=?", new String[]{userId});
        } finally {
            if (db != null) db.close();
        }
    }

    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(DatabaseHelper.T_USERS, null, null, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                do {
                    userList.add(cursorToUser(c));
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return userList;
    }

    public List<User> searchUsersByUsername(String usernamePattern) {
        List<User> userList = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(DatabaseHelper.T_USERS, null, "username LIKE ?", new String[]{"%" + usernamePattern + "%"}, null, null, "username ASC");
            if (c != null && c.moveToFirst()) {
                do {
                    userList.add(cursorToUser(c));
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return userList;
    }

    public void updateUserAllianceId(String userId, String allianceId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("current_alliance_id", allianceId);
            db.update(DatabaseHelper.T_USERS, values, "id=?", new String[]{userId});
        } finally {
            if (db != null) db.close();
        }
    }

    private ContentValues userToContentValues(User u) {
        ContentValues cv = new ContentValues();
        cv.put("email", u.getEmail());
        cv.put("username", u.getUsername());
        cv.put("avatar_name", u.getAvatarName());
        cv.put("is_activated", u.isActivated() ? 1 : 0);
        cv.put("created_at", u.getCreatedAt());
        cv.put("level", u.getLevel());
        cv.put("xp", u.getXp());
        cv.put("title", u.getTitle());
        cv.put("power_points", u.getPowerPoints());
        cv.put("coins", u.getCoins());
        cv.put("last_active_date", u.getLastActiveDate());
        cv.put("consecutive_active_days", u.getConsecutiveActiveDays());
        cv.put("friends_ids", TextUtils.join(",", u.getFriendsIds()));
        cv.put("current_alliance_id", u.getCurrentAllianceId());
        cv.put("fcm_token", u.getFcmToken());
        cv.put("previous_level_up_timestamp", u.getPreviousLevelUpTimestamp());
        cv.put("current_level_up_timestamp", u.getCurrentLevelUpTimestamp());
        return cv;
    }

    private User cursorToUser(Cursor c) {
        User u = new User();
        u.setUserId(c.getString(c.getColumnIndexOrThrow("id")));
        u.setEmail(c.getString(c.getColumnIndexOrThrow("email")));
        u.setUsername(c.getString(c.getColumnIndexOrThrow("username")));
        u.setAvatarName(c.getString(c.getColumnIndexOrThrow("avatar_name")));
        u.setActivated(c.getInt(c.getColumnIndexOrThrow("is_activated")) == 1);
        u.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        u.setXp(c.getInt(c.getColumnIndexOrThrow("xp")));
        u.setLevel(c.getInt(c.getColumnIndexOrThrow("level")));
        u.setTitle(c.getString(c.getColumnIndexOrThrow("title")));
        u.setPowerPoints(c.getInt(c.getColumnIndexOrThrow("power_points")));
        u.setCoins(c.getInt(c.getColumnIndexOrThrow("coins")));
        u.setLastActiveDate(c.getLong(c.getColumnIndexOrThrow("last_active_date")));
        u.setConsecutiveActiveDays(c.getInt(c.getColumnIndexOrThrow("consecutive_active_days")));
        String friendsIdsString = c.getString(c.getColumnIndexOrThrow("friends_ids"));
        if (friendsIdsString != null && !friendsIdsString.isEmpty()) {
            u.setFriendsIds(new ArrayList<>(Arrays.asList(friendsIdsString.split(","))));
        } else {
            u.setFriendsIds(new ArrayList<>());
        }
        u.setCurrentAllianceId(c.getString(c.getColumnIndexOrThrow("current_alliance_id")));
        int fcmTokenIndex = c.getColumnIndex("fcm_token");
        if (fcmTokenIndex != -1) {
            u.setFcmToken(c.getString(fcmTokenIndex));
        }
        int prevLevelUpIndex = c.getColumnIndex("previous_level_up_timestamp");
        if (prevLevelUpIndex != -1 && !c.isNull(prevLevelUpIndex)) {
            u.setPreviousLevelUpTimestamp(c.getLong(prevLevelUpIndex));
        }

        int currLevelUpIndex = c.getColumnIndex("current_level_up_timestamp");
        if (currLevelUpIndex != -1 && !c.isNull(currLevelUpIndex)) {
            u.setCurrentLevelUpTimestamp(c.getLong(currLevelUpIndex));
        }
        return u;
    }
}
