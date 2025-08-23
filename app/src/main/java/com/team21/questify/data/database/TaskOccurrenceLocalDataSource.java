package com.team21.questify.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.team21.questify.application.model.TaskOccurrence;
import com.team21.questify.application.model.enums.TaskStatus;

import java.util.ArrayList;
import java.util.List;

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
/*
    public TaskOccurrence getTaskOccurrenceById(String id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.T_TASK_OCCURRENCES, null,
                "id = ?", new String[]{id},
                null, null, null);
        TaskOccurrence occurrence = null;
        if (c != null && c.moveToFirst()) {
            occurrence = cursorToTaskOccurrence(c);
            c.close();
        }
        db.close();
        return occurrence;
    }

    public List<TaskOccurrence> getAllTaskOccurrences() {
        List<TaskOccurrence> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.T_TASK_OCCURRENCES, null,
                null, null, null, null, null);
        if (c != null && c.moveToFirst()) {
            do {
                list.add(cursorToTaskOccurrence(c));
            } while (c.moveToNext());
            c.close();
        }
        db.close();
        return list;
    }

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
}
