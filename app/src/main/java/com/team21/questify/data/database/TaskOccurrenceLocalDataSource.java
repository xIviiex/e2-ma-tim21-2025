package com.team21.questify.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.team21.questify.application.model.TaskOccurrence;
import com.team21.questify.application.model.enums.TaskDifficulty;
import com.team21.questify.application.model.enums.TaskPriority;
import com.team21.questify.application.model.enums.TaskStatus;

import java.util.ArrayList;
import java.util.Calendar;
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

    public void replaceAllForUser(String userId, List<TaskOccurrence> newOccurrences) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(DatabaseHelper.T_TASK_OCCURRENCES, "user_id = ?", new String[]{userId});

            for (TaskOccurrence occurrence : newOccurrences) {
                ContentValues cv = new ContentValues();
                cv.put("id", occurrence.getId());
                cv.put("task_id", occurrence.getTaskId());
                cv.put("user_id", occurrence.getUserId());
                cv.put("date", occurrence.getDate());
                cv.put("status", occurrence.getStatus().name());
                db.insert(DatabaseHelper.T_TASK_OCCURRENCES, null, cv);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }





    public List<TaskOccurrence> getOccurrencesByTaskId(String taskId) {
        List<TaskOccurrence> occurrences = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.T_TASK_OCCURRENCES, null,
                "task_id = ?", new String[]{taskId},
                null, null, "date ASC"); // Sortirano po datumu

        if (c != null) {
            while (c.moveToNext()) {
                occurrences.add(cursorToTaskOccurrence(c));
            }
            c.close();
        }
        db.close();
        return occurrences;
    }


    public List<TaskOccurrence> findFutureOccurrences(String taskId, long fromDate) {
        List<TaskOccurrence> occurrences = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        String selection = "task_id = ? AND date >= ? AND (status IS NULL OR status != ?)";
        String[] selectionArgs = {taskId, String.valueOf(fromDate), TaskStatus.COMPLETED.name()};

        Cursor c = db.query(DatabaseHelper.T_TASK_OCCURRENCES, null,
                selection, selectionArgs,
                null, null, "date ASC");

        if (c != null) {
            while (c.moveToNext()) {
                occurrences.add(cursorToTaskOccurrence(c));
            }
            c.close();
        }
        db.close();
        return occurrences;
    }



    public void updateTaskOccurrence(TaskOccurrence occurrence) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        // Ne ažuriramo ID i user_id
        cv.put("task_id", occurrence.getTaskId());
        cv.put("date", occurrence.getDate());
        cv.put("status", occurrence.getStatus() != null ? occurrence.getStatus().name() : null);
        db.update(DatabaseHelper.T_TASK_OCCURRENCES, cv, "id = ?", new String[]{occurrence.getId()});
        db.close();
    }


    public void updateTaskOccurrenceTaskId(String occurrenceId, String newTaskId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("task_id", newTaskId);
        db.update(DatabaseHelper.T_TASK_OCCURRENCES, cv, "id = ?", new String[]{occurrenceId});
        db.close();
    }


    public void deleteTaskOccurrence(String id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(DatabaseHelper.T_TASK_OCCURRENCES, "id = ?", new String[]{id});
        db.close();
    }


    public List<TaskOccurrence> getTodaysCompletedOccurrencesForUser(String userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<TaskOccurrence> occurrences = new ArrayList<>();


        Calendar calendar = Calendar.getInstance();


        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDayMillis = calendar.getTimeInMillis();


        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endOfDayMillis = calendar.getTimeInMillis();


        String selection = "user_id = ? AND status = ? AND date >= ? AND date <= ?";
        String[] selectionArgs = {
                userId,
                TaskStatus.COMPLETED.name(),
                String.valueOf(startOfDayMillis),
                String.valueOf(endOfDayMillis)
        };


        Cursor c = db.query(DatabaseHelper.T_TASK_OCCURRENCES,
                null,
                selection,
                selectionArgs,
                null,
                null,
                "date ASC");


        if (c != null) {
            while (c.moveToNext()) {
                occurrences.add(cursorToTaskOccurrence(c));
            }
            c.close();
        }


        db.close();
        return occurrences;
    }

    public void updateOccurrenceStatus(String occurrenceId, TaskStatus status) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("status", status.name());
        db.update(DatabaseHelper.T_TASK_OCCURRENCES, cv, "id = ?", new String[]{occurrenceId});
        db.close();
    }

    public List<TaskOccurrence> getCompletedOccurrencesForDateRange(String userId, long fromDate, long toDate) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<TaskOccurrence> occurrences = new ArrayList<>();
        String selection = "user_id = ? AND status = ? AND date >= ? AND date <= ?";
        String[] selectionArgs = {userId, TaskStatus.COMPLETED.name(), String.valueOf(fromDate), String.valueOf(toDate)};
        Cursor c = db.query(DatabaseHelper.T_TASK_OCCURRENCES, null, selection, selectionArgs, null, null, "date ASC");
        if (c != null) {
            while (c.moveToNext()) {
                occurrences.add(cursorToTaskOccurrence(c));
            }
            c.close();
        }
        db.close();
        return occurrences;
    }

    public int updateOldActiveOccurrencesToUncompleted() {
        SQLiteDatabase db = helper.getWritableDatabase();


        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -3);

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long threeDaysAgoMillis = calendar.getTimeInMillis();


        ContentValues cv = new ContentValues();
        cv.put("status", TaskStatus.UNCOMPLETED.name());


        String whereClause = "status = ? AND date < ?";
        String[] whereArgs = new String[]{
                TaskStatus.ACTIVE.name(),
                String.valueOf(threeDaysAgoMillis)
        };


        int rowsAffected = db.update(DatabaseHelper.T_TASK_OCCURRENCES, cv, whereClause, whereArgs);

        db.close();
        return rowsAffected;
    }


    //==========================================================
    //BORBA SA BOSS-OM
    //======================================================
    public List<TaskOccurrence> getCompletedOccurrencesInDateRange(String userId, long fromDate, long toDate) {
        List<TaskOccurrence> occurrences = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;

        try {
            String selection = "user_id = ? AND status = ? AND date >= ? AND date < ?";
            String[] selectionArgs = {
                    userId,
                    TaskStatus.COMPLETED.name(),
                    String.valueOf(fromDate),
                    String.valueOf(toDate)
            };

            c = db.query(DatabaseHelper.T_TASK_OCCURRENCES, null, selection, selectionArgs, null, null, "date ASC");

            if (c != null) {
                while (c.moveToNext()) {
                    occurrences.add(cursorToTaskOccurrence(c));
                }
            }
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return occurrences;
    }

    public List<TaskOccurrence> getUncompletedOccurrencesInDateRange(String userId, long fromDate, long toDate) {
        List<TaskOccurrence> occurrences = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;

        try {
            String selection = "user_id = ? AND status = ? AND date >= ? AND date < ?";
            String[] selectionArgs = {
                    userId,
                    TaskStatus.UNCOMPLETED.name(),
                    String.valueOf(fromDate),
                    String.valueOf(toDate)
            };

            c = db.query(DatabaseHelper.T_TASK_OCCURRENCES, null, selection, selectionArgs, null, null, "date ASC");

            if (c != null) {
                while (c.moveToNext()) {
                    occurrences.add(cursorToTaskOccurrence(c));
                }
            }
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return occurrences;
    }



    // =================================================================
    // NEW METHODS FOR XP QUOTA CHECKING
    // =================================================================

    public int getTodaysCompletedTaskCountByDifficulty(String userId, TaskDifficulty difficulty) {
        SQLiteDatabase db = helper.getReadableDatabase();
        int count = 0;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDay = calendar.getTimeInMillis();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endOfDay = calendar.getTimeInMillis();

        String query = "SELECT COUNT(T1.id) FROM " + DatabaseHelper.T_TASK_OCCURRENCES + " T1" +
                " JOIN " + DatabaseHelper.T_TASKS + " T2 ON T1.task_id = T2.id" +
                " WHERE T1.user_id = ? AND T1.status = ? AND T2.task_difficulty = ?" +
                " AND T1.date BETWEEN ? AND ?";

        Cursor c = db.rawQuery(query, new String[]{
                userId, TaskStatus.COMPLETED.name(), difficulty.name(), String.valueOf(startOfDay), String.valueOf(endOfDay)
        });

        if (c != null && c.moveToFirst()) {
            count = c.getInt(0);
            c.close();
        }
        db.close();
        return count;
    }


    public int getTodaysCompletedTaskCountByPriority(String userId, TaskPriority priority) {
        SQLiteDatabase db = helper.getReadableDatabase();
        int count = 0;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDay = calendar.getTimeInMillis();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endOfDay = calendar.getTimeInMillis();

        String query = "SELECT COUNT(T1.id) FROM " + DatabaseHelper.T_TASK_OCCURRENCES + " T1" +
                " JOIN " + DatabaseHelper.T_TASKS + " T2 ON T1.task_id = T2.id" +
                " WHERE T1.user_id = ? AND T1.status = ? AND T2.task_priority = ?" +
                " AND T1.date BETWEEN ? AND ?";

        Cursor c = db.rawQuery(query, new String[]{
                userId, TaskStatus.COMPLETED.name(), priority.name(), String.valueOf(startOfDay), String.valueOf(endOfDay)
        });

        if (c != null && c.moveToFirst()) {
            count = c.getInt(0);
            c.close();
        }
        db.close();
        return count;
    }


    public int getThisWeeksCompletedTaskCount(String userId, TaskDifficulty difficulty) {
        SQLiteDatabase db = helper.getReadableDatabase();
        int count = 0;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfWeek = calendar.getTimeInMillis();

        calendar.add(Calendar.WEEK_OF_YEAR, 1);
        calendar.add(Calendar.DAY_OF_WEEK, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endOfWeek = calendar.getTimeInMillis();

        String query = "SELECT COUNT(T1.id) FROM " + DatabaseHelper.T_TASK_OCCURRENCES + " T1" +
                " JOIN " + DatabaseHelper.T_TASKS + " T2 ON T1.task_id = T2.id" +
                " WHERE T1.user_id = ? AND T1.status = ? AND T2.task_difficulty = ?" +
                " AND T1.date BETWEEN ? AND ?";

        Cursor c = db.rawQuery(query, new String[]{
                userId,
                TaskStatus.COMPLETED.name(),
                difficulty.name(),
                String.valueOf(startOfWeek),
                String.valueOf(endOfWeek)
        });

        if (c != null && c.moveToFirst()) {
            count = c.getInt(0);
            c.close();
        }
        db.close();
        return count;
    }


    public int getThisMonthsCompletedTaskCount(String userId, TaskPriority priority) {
        SQLiteDatabase db = helper.getReadableDatabase();
        int count = 0;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfMonth = calendar.getTimeInMillis();

        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        long endOfMonth = calendar.getTimeInMillis();

        String query = "SELECT COUNT(T1.id) FROM " + DatabaseHelper.T_TASK_OCCURRENCES + " T1" +
                " JOIN " + DatabaseHelper.T_TASKS + " T2 ON T1.task_id = T2.id" +
                " WHERE T1.user_id = ? AND T1.status = ? AND T2.task_priority = ?" +
                " AND T1.date BETWEEN ? AND ?";

        Cursor c = db.rawQuery(query, new String[]{
                userId,
                TaskStatus.COMPLETED.name(),
                priority.name(),
                String.valueOf(startOfMonth),
                String.valueOf(endOfMonth)
        });

        if (c != null && c.moveToFirst()) {
            count = c.getInt(0);
            c.close();
        }
        db.close();
        return count;
    }




}
