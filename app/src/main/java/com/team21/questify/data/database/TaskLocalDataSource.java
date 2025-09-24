package com.team21.questify.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.team21.questify.application.model.Task;
import com.team21.questify.application.model.enums.TaskDifficulty;
import com.team21.questify.application.model.enums.TaskPriority;
import com.team21.questify.application.model.enums.TaskStatus;
import com.team21.questify.application.model.enums.TaskType;
import com.team21.questify.application.model.enums.RecurrenceUnit;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskLocalDataSource {
    private final DatabaseHelper helper;

    public TaskLocalDataSource(Context context) {
        this.helper = new DatabaseHelper(context);
    }

    public void insertTask(Task task) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id", task.getId());
        cv.put("user_id", task.getUserId());
        cv.put("name", task.getName());
        cv.put("description", task.getDescription());
        cv.put("task_category_id", task.getTaskCategoryId());
        cv.put("task_type", task.getTaskType().name());
        cv.put("recurrence_unit", task.getRecurrenceUnit() != null ? task.getRecurrenceUnit().name() : null);
        cv.put("recurring_interval", task.getRecurringInterval());
        cv.put("recurring_start_date", task.getRecurringStartDate());
        cv.put("recurring_end_date", task.getRecurringEndDate());
        cv.put("execution_time", task.getExecutionTime());
        cv.put("task_difficulty", task.getTaskDifficulty().name());
        cv.put("task_priority", task.getTaskPriority().name());
        cv.put("xp", task.getXp());
        db.insert(DatabaseHelper.T_TASKS, null, cv);
        db.close();
    }

    public List<Task> getAllTasksForUser(String userId) {
        List<Task> tasks = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.query(DatabaseHelper.T_TASKS, null,
                "user_id = ?", new String[]{userId},
                null, null, null);

        if (c != null) {
            while (c.moveToNext()) {
                Task task = cursorToTask(c);
                tasks.add(task);
            }
            c.close();
        }

        db.close();
        return tasks;
    }

    public Task getTaskById(String taskId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Task task = null;

        Cursor c = db.query(DatabaseHelper.T_TASKS,
                null, // all columns
                "id = ?", // where clause
                new String[]{taskId}, // where args
                null, null, null);

        if (c != null && c.moveToFirst()) {
            task = cursorToTask(c);
            c.close();
        }

        db.close();
        return task;
    }


    private Task cursorToTask(Cursor c) {
        Task task = new Task();
        task.setId(c.getString(c.getColumnIndexOrThrow("id")));
        task.setUserId(c.getString(c.getColumnIndexOrThrow("user_id")));
        task.setName(c.getString(c.getColumnIndexOrThrow("name")));
        task.setDescription(c.getString(c.getColumnIndexOrThrow("description")));
        task.setTaskCategoryId(c.getString(c.getColumnIndexOrThrow("task_category_id")));
        task.setTaskType(TaskType.valueOf(c.getString(c.getColumnIndexOrThrow("task_type"))));

        String recUnit = c.getString(c.getColumnIndexOrThrow("recurrence_unit"));
        task.setRecurrenceUnit(recUnit != null ? RecurrenceUnit.valueOf(recUnit) : null);

        task.setRecurringInterval(c.getInt(c.getColumnIndexOrThrow("recurring_interval")));
        task.setRecurringStartDate(c.getLong(c.getColumnIndexOrThrow("recurring_start_date")));
        task.setRecurringEndDate(c.getLong(c.getColumnIndexOrThrow("recurring_end_date")));
        task.setExecutionTime(c.getLong(c.getColumnIndexOrThrow("execution_time")));
        task.setTaskDifficulty(TaskDifficulty.valueOf(c.getString(c.getColumnIndexOrThrow("task_difficulty"))));
        task.setTaskPriority(TaskPriority.valueOf(c.getString(c.getColumnIndexOrThrow("task_priority"))));
        task.setXp(c.getInt(c.getColumnIndexOrThrow("xp")));
        return task;
    }

    public Map<String, Integer> getCompletedTaskCountsByCategory(String userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Map<String, Integer> counts = new HashMap<>();

        String query = "SELECT " + DatabaseHelper.T_TASK_CATEGORIES + "." + "name" + ", COUNT(" + DatabaseHelper.T_TASKS + "." + "id" + ")" +
                " FROM " + DatabaseHelper.T_TASKS +
                " JOIN " + DatabaseHelper.T_TASK_OCCURRENCES + " ON " + DatabaseHelper.T_TASKS + "." + "id" + " = " + DatabaseHelper.T_TASK_OCCURRENCES + "." + "task_id" +
                " JOIN " + DatabaseHelper.T_TASK_CATEGORIES + " ON " + DatabaseHelper.T_TASKS + "." + "task_category_id" + " = " + DatabaseHelper.T_TASK_CATEGORIES + "." + "id" +
                " WHERE " + DatabaseHelper.T_TASK_OCCURRENCES + "." + "user_id" + " = ?" +
                " AND " + DatabaseHelper.T_TASK_OCCURRENCES + "." + "status" + " = '" + TaskStatus.COMPLETED.name() + "'" +
                " GROUP BY " + DatabaseHelper.T_TASK_CATEGORIES + "." + "name";

        Cursor cursor = db.rawQuery(query, new String[]{userId});

        if (cursor.moveToFirst()) {
            do {
                String categoryName = cursor.getString(0);
                int count = cursor.getInt(1);
                counts.put(categoryName, count);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return counts;
    }

    public Map<Long, String> getCompletedTaskDifficultiesWithDates(String userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Map<Long, String> completedTasks = new HashMap<>();

        String query = "SELECT " + DatabaseHelper.T_TASKS + "." + "task_difficulty" + ", " + DatabaseHelper.T_TASK_OCCURRENCES + "." + "date" +
                " FROM " + DatabaseHelper.T_TASKS +
                " JOIN " + DatabaseHelper.T_TASK_OCCURRENCES + " ON " + DatabaseHelper.T_TASKS + "." + "id" + " = " + DatabaseHelper.T_TASK_OCCURRENCES + "." + "task_id" +
                " WHERE " + DatabaseHelper.T_TASK_OCCURRENCES + "." + "user_id" + " = ?" +
                " AND " + DatabaseHelper.T_TASK_OCCURRENCES + "." + "status" + " = '" + TaskStatus.COMPLETED.name() + "'";

        Cursor cursor = db.rawQuery(query, new String[]{userId});

        if (cursor.moveToFirst()) {
            do {
                completedTasks.put(cursor.getLong(1), cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return completedTasks;
    }

    public Map<String, Integer> getWeeklyXp(String userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Map<String, Integer> weeklyXp = new HashMap<>();

        long sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;

        String query = "SELECT " + DatabaseHelper.T_TASK_OCCURRENCES + "." + "date" + ", SUM(" + DatabaseHelper.T_TASKS + "." + "xp" + ")" +
                " FROM " + DatabaseHelper.T_TASKS +
                " JOIN " + DatabaseHelper.T_TASK_OCCURRENCES + " ON " + DatabaseHelper.T_TASKS + "." + "id" + " = " + DatabaseHelper.T_TASK_OCCURRENCES + "." + "task_id" +
                " WHERE " + DatabaseHelper.T_TASK_OCCURRENCES + "." + "user_id" + " = ?" +
                " AND " + DatabaseHelper.T_TASK_OCCURRENCES + "." + "status" + " = '" + TaskStatus.COMPLETED.name() + "'" +
                " AND " + DatabaseHelper.T_TASK_OCCURRENCES + "." + "date" + " > ?" +
                " GROUP BY " + DatabaseHelper.T_TASK_OCCURRENCES + "." + "date";

        Cursor cursor = db.rawQuery(query, new String[]{userId, String.valueOf(sevenDaysAgo)});

        if (cursor.moveToFirst()) {
            do {
                String dateString = convertTimestampToDateString(cursor.getLong(0));
                int totalXp = cursor.getInt(1);
                weeklyXp.put(dateString, totalXp);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return weeklyXp;
    }

    private String convertTimestampToDateString(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        return String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, day);

    }




    public void updateTask(Task task) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        // ID se ne menja, on je u WHERE klauzuli
        cv.put("user_id", task.getUserId());
        cv.put("name", task.getName());
        cv.put("description", task.getDescription());
        cv.put("task_category_id", task.getTaskCategoryId());
        cv.put("task_type", task.getTaskType().name());
        cv.put("recurrence_unit", task.getRecurrenceUnit() != null ? task.getRecurrenceUnit().name() : null);
        cv.put("recurring_interval", task.getRecurringInterval());
        cv.put("recurring_start_date", task.getRecurringStartDate());
        cv.put("recurring_end_date", task.getRecurringEndDate());
        cv.put("execution_time", task.getExecutionTime());
        cv.put("task_difficulty", task.getTaskDifficulty().name());
        cv.put("task_priority", task.getTaskPriority().name());
        cv.put("xp", task.getXp());
        db.update(DatabaseHelper.T_TASKS, cv, "id = ?", new String[]{task.getId()});
        db.close();
    }



    public void updateTaskEndDate(String taskId, Long newEndDate) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("recurring_end_date", newEndDate);
        db.update(DatabaseHelper.T_TASKS, cv, "id = ?", new String[]{taskId});
        db.close();
    }


    public void deleteTask(String taskId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(DatabaseHelper.T_TASKS, "id = ?", new String[]{taskId});
        db.close();
    }

}
