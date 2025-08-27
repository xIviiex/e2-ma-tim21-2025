package com.team21.questify.data.database;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.team21.questify.application.model.TaskCategory;

import java.util.ArrayList;
import java.util.List;

public class TaskCategoryLocalDataSource {
    private final DatabaseHelper helper;

    public TaskCategoryLocalDataSource(Context ctx) {
        this.helper = new DatabaseHelper(ctx);
    }

    public void insertCategory(TaskCategory category) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("id", category.getId());
        cv.put("user_id", category.getUserId());
        cv.put("name", category.getName());
        cv.put("hex_color", category.getHexColor());

        db.insert(DatabaseHelper.T_TASK_CATEGORIES, null, cv);
        db.close();
    }

    private TaskCategory cursorToTaskCategory(Cursor c) {
        TaskCategory category = new TaskCategory();
        category.setId(c.getString(c.getColumnIndexOrThrow("id")));
        category.setUserId(c.getString(c.getColumnIndexOrThrow("user_id")));
        category.setName(c.getString(c.getColumnIndexOrThrow("name")));
        category.setHexColor(c.getString(c.getColumnIndexOrThrow("hex_color")));
        return category;
    }

    public List<TaskCategory> getAllCategoriesForUser(String userId) {
        List<TaskCategory> categoryList = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.T_TASK_CATEGORIES, null,
                "user_id" + "=?", new String[]{userId},
                null, null, null);
        if (c != null && c.moveToFirst()) {
            do {
                categoryList.add(cursorToTaskCategory(c));
            } while (c.moveToNext());
            c.close();
        }
        db.close();
        return categoryList;
    }


    public TaskCategory getCategoryById(String categoryId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        TaskCategory category = null;
        Cursor c = db.query(DatabaseHelper.T_TASK_CATEGORIES, null,
                "id" + "=?", new String[]{categoryId},
                null, null, null);

        if (c != null && c.moveToFirst()) {
            category = cursorToTaskCategory(c);
            c.close();
        }
        db.close();
        return category;
    }
}
