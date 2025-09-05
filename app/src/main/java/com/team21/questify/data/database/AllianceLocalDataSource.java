package com.team21.questify.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.team21.questify.application.model.Alliance;
import com.team21.questify.application.model.enums.MissionStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AllianceLocalDataSource {
    private final DatabaseHelper helper;

    public AllianceLocalDataSource(Context ctx) {
        this.helper = new DatabaseHelper(ctx);
    }

    public void insertAlliance(Alliance alliance) {
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            ContentValues cv = allianceToContentValues(alliance);
            cv.put("id", alliance.getAllianceId());
            db.insertWithOnConflict(DatabaseHelper.T_ALLIANCES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } finally {
            if (db != null) db.close();
        }
    }

    public void updateAlliance(Alliance alliance) {
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            ContentValues cv = allianceToContentValues(alliance);
            db.update(DatabaseHelper.T_ALLIANCES, cv, "id=?", new String[]{alliance.getAllianceId()});
        } finally {
            if (db != null) db.close();
        }
    }

    public Alliance getAllianceById(String allianceId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        Alliance alliance = null;
        try {
            c = db.query(DatabaseHelper.T_ALLIANCES, null, "id=?", new String[]{allianceId}, null, null, null);
            if (c != null && c.moveToFirst()) {
                alliance = cursorToAlliance(c);
            }
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return alliance;
    }


    public void deleteAlliance(String allianceId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            db.delete(DatabaseHelper.T_ALLIANCES, "id=?", new String[]{allianceId});
        } finally {
            if (db != null) db.close();
        }
    }

    public void updateAllianceMembers(String allianceId, List<String> membersIds) {
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put("members_ids", TextUtils.join(",", membersIds));
            db.update(DatabaseHelper.T_ALLIANCES, cv, "id=?", new String[]{allianceId});
        } finally {
            if (db != null) db.close();
        }
    }

    private Alliance cursorToAlliance(Cursor c) {
        Alliance a = new Alliance();
        a.setAllianceId(c.getString(c.getColumnIndexOrThrow("id")));
        a.setName(c.getString(c.getColumnIndexOrThrow("name")));
        a.setLeaderId(c.getString(c.getColumnIndexOrThrow("leader_id")));
        a.setCurrentMissionId(c.getString(c.getColumnIndexOrThrow("current_mission_id")));
        a.setMissionStatus(MissionStatus.valueOf(c.getString(c.getColumnIndexOrThrow("mission_status"))));

        String membersIdsString = c.getString(c.getColumnIndexOrThrow("members_ids"));
        if (membersIdsString != null && !membersIdsString.isEmpty()) {
            String[] idsArray = membersIdsString.split(",");
            a.setMembersIds(new ArrayList<>(Arrays.asList(idsArray)));
        } else {
            a.setMembersIds(new ArrayList<>());
        }
        return a;
    }

    private ContentValues allianceToContentValues(Alliance alliance) {
        ContentValues cv = new ContentValues();
        cv.put("name", alliance.getName());
        cv.put("leader_id", alliance.getLeaderId());
        cv.put("members_ids", TextUtils.join(",", alliance.getMembersIds()));
        cv.put("current_mission_id", alliance.getCurrentMissionId());
        cv.put("mission_status", alliance.getMissionStatus().name());
        return cv;
    }

}
