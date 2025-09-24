package com.team21.questify.data.database;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.team21.questify.application.model.SpecialMission;
import com.team21.questify.application.model.SpecialMissionUser;
import com.team21.questify.application.model.enums.Badge;
import com.team21.questify.application.model.enums.MissionStatus;
import com.team21.questify.data.database.DatabaseHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpecialMissionLocalDataSource {

    private final DatabaseHelper dbHelper;

    public SpecialMissionLocalDataSource(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }


    public void saveOrUpdateSpecialMission(SpecialMission mission) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {

            ContentValues missionValues = missionToContentValues(mission);
            db.insertWithOnConflict(DatabaseHelper.T_SPECIAL_MISSIONS, null, missionValues, SQLiteDatabase.CONFLICT_REPLACE);


            db.delete(DatabaseHelper.T_SPECIAL_MISSION_PROGRESS, "mission_id=?", new String[]{mission.getMissionId()});


            for (SpecialMissionUser userProgress : mission.getParticipantsProgress().values()) {
                ContentValues progressValues = userProgressToContentValues(mission.getMissionId(), userProgress);
                db.insert(DatabaseHelper.T_SPECIAL_MISSION_PROGRESS, null, progressValues);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }


    public SpecialMission getActiveMissionForAlliance(String allianceId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SpecialMission mission = null;
        Cursor missionCursor = null;

        try {

            String selection = "alliance_id = ? AND status = ?";
            String[] selectionArgs = new String[]{allianceId, MissionStatus.STARTED.name()};


            missionCursor = db.query(DatabaseHelper.T_SPECIAL_MISSIONS, null, selection, selectionArgs, null, null, null, "1");

            if (missionCursor.moveToFirst()) {
                mission = cursorToSpecialMission(missionCursor);
            }
        } finally {
            if (missionCursor != null) missionCursor.close();
        }

        if (mission == null) {
            return null;
        }

        Map<String, SpecialMissionUser> progressMap = getProgressForMission(mission.getMissionId());
        mission.setParticipantsProgress(progressMap);

        return mission;
    }



    private Map<String, SpecialMissionUser> getProgressForMission(String missionId) {
        Map<String, SpecialMissionUser> progressMap = new HashMap<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor progressCursor = null;
        try {
            progressCursor = db.query(DatabaseHelper.T_SPECIAL_MISSION_PROGRESS, null, "mission_id = ?", new String[]{missionId}, null, null, null);
            while (progressCursor.moveToNext()) {
                SpecialMissionUser userProgress = cursorToSpecialMissionUser(progressCursor);
                progressMap.put(userProgress.getUserId(), userProgress);
            }
        } finally {
            if (progressCursor != null) progressCursor.close();
        }
        return progressMap;
    }

    private ContentValues missionToContentValues(SpecialMission mission) {
        ContentValues cv = new ContentValues();
        cv.put("mission_id", mission.getMissionId());
        cv.put("alliance_id", mission.getAllianceId());
        cv.put("status", mission.getMissionStatus().name());
        cv.put("initial_hp", mission.getInitialBossHp());
        cv.put("current_hp", mission.getCurrentBossHp());
        cv.put("start_time", mission.getStartTime());
        cv.put("end_time", mission.getEndTime());
        return cv;
    }

    private SpecialMission cursorToSpecialMission(Cursor c) {
        SpecialMission mission = new SpecialMission();
        mission.setMissionId(c.getString(c.getColumnIndexOrThrow("mission_id")));
        mission.setAllianceId(c.getString(c.getColumnIndexOrThrow("alliance_id")));
        mission.setMissionStatus(MissionStatus.valueOf(c.getString(c.getColumnIndexOrThrow("status"))));
        mission.setInitialBossHp(c.getInt(c.getColumnIndexOrThrow("initial_hp")));
        mission.setCurrentBossHp(c.getInt(c.getColumnIndexOrThrow("current_hp")));
        mission.setStartTime(c.getLong(c.getColumnIndexOrThrow("start_time")));
        mission.setEndTime(c.getLong(c.getColumnIndexOrThrow("end_time")));
        return mission;
    }

    private ContentValues userProgressToContentValues(String missionId, SpecialMissionUser userProgress) {
        ContentValues cv = new ContentValues();
        cv.put("mission_id", missionId);
        cv.put("user_id", userProgress.getUserId());
        cv.put("store_purchases", userProgress.getStorePurchases());
        cv.put("successful_boss_hits", userProgress.getSuccessfulRegularBossHits());
        cv.put("solved_basic_tasks", userProgress.getSolvedVeryEasyEasyNormalOrImportantTasks());
        cv.put("solved_other_tasks", userProgress.getSolvedOtherTasks());
        cv.put("has_no_unsolved_tasks", userProgress.isHasNoUnsolvedTasks() ? 1 : 0);
        cv.put("total_damage_contributed", userProgress.getTotalDamageContributed());
        cv.put("days_with_message_sent", convertLongListToString(userProgress.getDaysWithMessageSent()));
        cv.put("earned_badges", convertBadgeListToString(userProgress.getEarnedBadges()));
        return cv;
    }

    private SpecialMissionUser cursorToSpecialMissionUser(Cursor c) {
        SpecialMissionUser userProgress = new SpecialMissionUser();
        userProgress.setUserId(c.getString(c.getColumnIndexOrThrow("user_id")));
        userProgress.setStorePurchases(c.getInt(c.getColumnIndexOrThrow("store_purchases")));
        userProgress.setSuccessfulRegularBossHits(c.getInt(c.getColumnIndexOrThrow("successful_boss_hits")));
        userProgress.setSolvedVeryEasyEasyNormalOrImportantTasks(c.getInt(c.getColumnIndexOrThrow("solved_basic_tasks")));
        userProgress.setSolvedOtherTasks(c.getInt(c.getColumnIndexOrThrow("solved_other_tasks")));
        userProgress.setHasNoUnsolvedTasks(c.getInt(c.getColumnIndexOrThrow("has_no_unsolved_tasks")) == 1);
        userProgress.setTotalDamageContributed(c.getInt(c.getColumnIndexOrThrow("total_damage_contributed")));
        userProgress.setDaysWithMessageSent(convertStringToLongList(c.getString(c.getColumnIndexOrThrow("days_with_message_sent"))));
        userProgress.setEarnedBadges(convertStringToBadgeList(c.getString(c.getColumnIndexOrThrow("earned_badges"))));
        return userProgress;
    }



    private String convertLongListToString(List<Long> list) {
        if (list == null || list.isEmpty()) return null;
        return TextUtils.join(",", list);
    }

    private List<Long> convertStringToLongList(String s) {
        if (TextUtils.isEmpty(s)) return new ArrayList<>();
        return Arrays.stream(s.split(",")).map(Long::parseLong).collect(Collectors.toList());
    }

    private String convertBadgeListToString(List<Badge> list) {
        if (list == null || list.isEmpty()) return null;
        return list.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    private List<Badge> convertStringToBadgeList(String s) {
        if (TextUtils.isEmpty(s)) return new ArrayList<>();
        return Arrays.stream(s.split(",")).map(Badge::valueOf).collect(Collectors.toList());
    }
}