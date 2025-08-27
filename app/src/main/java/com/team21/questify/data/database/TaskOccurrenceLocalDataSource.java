package com.team21.questify.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.team21.questify.application.model.TaskOccurrence;
import com.team21.questify.application.model.enums.TaskStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskOccurrenceLocalDataSource {
    private final DatabaseHelper helper;

    public TaskOccurrenceLocalDataSource(Context ctx) {
        this.helper = new DatabaseHelper(ctx);
    }

    public void insertTaskOccurrence(TaskOccurrence occurrence) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id", occurrence.getId());
        cv.put("task_id", occurrence.getTaskId());
        cv.put("user_id", occurrence.getUserId());
        cv.put("date", occurrence.getDate()); // Long epoch millis
        cv.put("status", occurrence.getStatus() != null ? occurrence.getStatus().name() : null);
        db.insert(DatabaseHelper.T_TASK_OCCURRENCES, null, cv);
        db.close();
    }


    public List<TaskOccurrence> getAllOccurrencesForUser(String userId) {
        List<TaskOccurrence> occurrences = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.query(DatabaseHelper.T_TASK_OCCURRENCES, null,
                "user_id = ?", new String[]{userId},
                null, null, null);

        if (c != null) {
            while (c.moveToNext()) {
                TaskOccurrence occurrence = cursorToTaskOccurrence(c);
                occurrences.add(occurrence);
            }
            c.close();
        }

        db.close();
        return occurrences;
    }





/*
    public void updateTaskOccurrence(TaskOccurrence occurrence) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("task_id", occurrence.getTaskId());
        cv.put("user_id", occurrence.getUserId());
        cv.put("date", occurrence.getDate());
        cv.put("status", occurrence.getStatus() != null ? occurrence.getStatus().name() : null);
        db.update(DatabaseHelper.T_TASK_OCCURRENCES, cv, "id = ?", new String[]{occurrence.getId()});
        db.close();
    }

    public void deleteTaskOccurrence(String id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(DatabaseHelper.T_TASK_OCCURRENCES, "id = ?", new String[]{id});
        db.close();
    }
*/





    private TaskOccurrence cursorToTaskOccurrence(Cursor c) {
        TaskOccurrence occurrence = new TaskOccurrence();
        occurrence.setId(c.getString(c.getColumnIndexOrThrow("id")));
        occurrence.setTaskId(c.getString(c.getColumnIndexOrThrow("task_id")));
        occurrence.setUserId(c.getString(c.getColumnIndexOrThrow("user_id")));
        occurrence.setDate(c.isNull(c.getColumnIndexOrThrow("date")) ? null : c.getLong(c.getColumnIndexOrThrow("date")));
        String statusStr = c.getString(c.getColumnIndexOrThrow("status"));
        occurrence.setStatus(statusStr != null ? TaskStatus.valueOf(statusStr) : null);
        return occurrence;
    }

    public Map<String, Integer> getTaskCountsByStatus(String userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Map<String, Integer> counts = new HashMap<>();

        String query = "SELECT " + "status" + ", COUNT(*)" +
                " FROM " + DatabaseHelper.T_TASK_OCCURRENCES +
                " WHERE " + "user_id" + " = ?" +
                " GROUP BY " + "status";

        Cursor cursor = db.rawQuery(query, new String[]{userId});

        if (cursor.moveToFirst()) {
            do {
                String statusString = cursor.getString(0);
                int count = cursor.getInt(1);
                counts.put(statusString, count);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return counts;
    }

    public List<TaskOccurrence> getTaskOccurrencesByUserIdSortedByDate(String userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<TaskOccurrence> occurrences = new ArrayList<>();

        String selection = "user_id" + " = ?";
        String orderBy = "date" + " ASC";

        Cursor c = db.query(DatabaseHelper.T_TASK_OCCURRENCES,
                null,
                selection,
                new String[]{userId},
                null,
                null,
                orderBy);

        if (c.moveToFirst()) {
            do {
                occurrences.add(cursorToTaskOccurrence(c));
            } while (c.moveToNext());
            c.close();
        }
        db.close();
        return occurrences;
    }
}
