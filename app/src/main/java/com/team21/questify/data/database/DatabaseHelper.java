package com.team21.questify.data.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "questify.db";
    public static final int DB_VERSION = 5;
    public static final String T_USERS = "users";
    public static final String T_TASK_CATEGORIES = "task_categories";
    public static final String T_TASKS = "tasks";
    public static final String T_TASK_OCCURRENCES = "task_occurrences";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_USERS + " (" +
                "id TEXT PRIMARY KEY, " +
                "email TEXT NOT NULL UNIQUE, " +
                "username TEXT NOT NULL UNIQUE, " +
                "avatar_name TEXT NOT NULL, " +
                "is_activated INTEGER NOT NULL DEFAULT 0, " +
                "created_at INTEGER NOT NULL," +
                "xp INTEGER DEFAULT 0," +
                "level INTEGER DEFAULT 1," +
                "power_points INTEGER DEFAULT 0," +
                "title TEXT DEFAULT 'Adventurer'," +
                "coins INTEGER DEFAULT 0," +
                "last_active_date INTEGER," +
                "consecutive_active_days INTEGER DEFAULT 0," +
                "friends_ids TEXT" +
                ")");

        db.execSQL("CREATE TABLE " + T_TASK_CATEGORIES + " (" +
                "id TEXT PRIMARY KEY, " +
                "user_id TEXT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "hex_color TEXT NOT NULL, " +
                "FOREIGN KEY(user_id) REFERENCES " + T_USERS + "(id) ON DELETE CASCADE" +
                ")");


        db.execSQL("CREATE TABLE " + T_TASKS + " (" +
                "id TEXT PRIMARY KEY, " +
                "user_id TEXT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "description TEXT, " +
                "task_category_id TEXT NOT NULL, " +
                "task_type TEXT NOT NULL, " +
                "recurrence_unit TEXT, " +
                "recurring_interval INTEGER, " +
                "recurring_start_date INTEGER, " +
                "recurring_end_date INTEGER, " +
                "execution_time INTEGER NOT NULL, " +
                "task_difficulty TEXT NOT NULL, " +
                "task_priority TEXT NOT NULL, " +
                "xp INTEGER NOT NULL, " +
                "FOREIGN KEY(user_id) REFERENCES " + T_USERS + "(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(task_category_id) REFERENCES " + T_TASK_CATEGORIES + "(id) ON DELETE CASCADE" +
                ")");


        db.execSQL("CREATE TABLE " + T_TASK_OCCURRENCES + " (" +
                "id TEXT PRIMARY KEY, " +
                "task_id TEXT NOT NULL, " +
                "user_id TEXT NOT NULL, " +
                "date INTEGER NOT NULL, " +
                "status TEXT NOT NULL, " +
                "FOREIGN KEY(task_id) REFERENCES " + T_TASKS + "(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(user_id) REFERENCES " + T_USERS + "(id) ON DELETE CASCADE" +
                ")");



    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + T_USERS + " ADD COLUMN power_points INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + T_USERS + " ADD COLUMN title TEXT DEFAULT 'Adventurer'");
            db.execSQL("ALTER TABLE " + T_USERS + " ADD COLUMN coins INTEGER DEFAULT 0");
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE " + T_TASK_CATEGORIES + " (" +
                    "id TEXT PRIMARY KEY, " +
                    "user_id TEXT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "hex_color TEXT NOT NULL, " +
                    "FOREIGN KEY(user_id) REFERENCES " + T_USERS + "(id) ON DELETE CASCADE" +
                    ")");


            db.execSQL("CREATE TABLE " + T_TASKS + " (" +
                    "id TEXT PRIMARY KEY, " +
                    "user_id TEXT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "description TEXT, " +
                    "task_category_id TEXT NOT NULL, " +
                    "task_type TEXT NOT NULL, " +
                    "recurrence_unit TEXT, " +
                    "recurring_interval INTEGER, " +
                    "recurring_start_date INTEGER, " +
                    "recurring_end_date INTEGER, " +
                    "execution_time INTEGER NOT NULL, " +
                    "task_difficulty TEXT NOT NULL, " +
                    "task_priority TEXT NOT NULL, " +
                    "xp INTEGER NOT NULL, " +
                    "FOREIGN KEY(user_id) REFERENCES " + T_USERS + "(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY(task_category_id) REFERENCES " + T_TASK_CATEGORIES + "(id) ON DELETE CASCADE" +
                    ")");


            db.execSQL("CREATE TABLE " + T_TASK_OCCURRENCES + " (" +
                    "id TEXT PRIMARY KEY, " +
                    "task_id TEXT NOT NULL, " +
                    "user_id TEXT NOT NULL, " +
                    "date INTEGER NOT NULL, " +
                    "status TEXT NOT NULL, " +
                    "FOREIGN KEY(task_id) REFERENCES " + T_TASKS + "(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY(user_id) REFERENCES " + T_USERS + "(id) ON DELETE CASCADE" +
                    ")");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + T_USERS + " ADD COLUMN last_active_date INTEGER");
            db.execSQL("ALTER TABLE " + T_USERS + " ADD COLUMN consecutive_active_days INTEGER DEFAULT 0");
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE " + T_USERS + " ADD COLUMN friends_ids TEXT");
        }
    }

}
