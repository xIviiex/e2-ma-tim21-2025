package com.team21.questify.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.team21.questify.application.model.Task;
import com.team21.questify.application.model.enums.TaskDifficulty;
import com.team21.questify.application.model.enums.TaskPriority;
import com.team21.questify.application.model.enums.TaskType;
import com.team21.questify.application.model.enums.RecurrenceUnit;

import java.util.ArrayList;
import java.util.List;

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
/*
    public Task getTaskById(String taskId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.T_TASKS, null,
                "id = ?", new String[]{taskId},
                null, null, null);
        Task task = null;
        if (c != null && c.moveToFirst()) {
            task = cursorToTask(c);
            c.close();
        }
        db.close();
        return task;
    }

    public List<Task> getAllTasks() {
        List<Task> taskList = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.T_TASKS, null, null, null, null, null, null);
        if (c != null && c.moveToFirst()) {
            do {
                taskList.add(cursorToTask(c));
            } while (c.moveToNext());
            c.close();
        }
        db.close();
        return taskList;
    }

   public void updateTask(Task task) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
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

    public void deleteTask(String taskId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(DatabaseHelper.T_TASKS, "id = ?", new String[]{taskId});
        db.close();
    }
*/
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
}
