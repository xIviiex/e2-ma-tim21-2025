package com.team21.questify.data.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "questify.db";
    public static final int DB_VERSION = 10;
    public static final String T_USERS = "users";
    public static final String T_TASK_CATEGORIES = "task_categories";
    public static final String T_TASKS = "tasks";
    public static final String T_TASK_OCCURRENCES = "task_occurrences";
    public static final String T_ALLIANCES = "alliances";
    public static final String T_INVITATIONS = "invitations";
    public static final String T_MESSAGES = "messages";
    public static final String T_INVENTORY = "inventory";
    public static final String T_BOSSES = "bosses";

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
                "friends_ids TEXT," +
                "current_alliance_id TEXT," +
                "fcm_token TEXT," +
                "previous_level_up_timestamp INTEGER," +
                "current_level_up_timestamp INTEGER" +
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

        db.execSQL("CREATE TABLE " + T_ALLIANCES + " (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "leader_id TEXT NOT NULL, " +
                "members_ids TEXT," +
                "current_mission_id TEXT," +
                "mission_status TEXT NOT NULL," +
                "FOREIGN KEY(leader_id) REFERENCES " + T_USERS + "(id) ON DELETE CASCADE" +
                ")");

        db.execSQL("CREATE TABLE " + T_INVITATIONS + " (" +
                "id TEXT PRIMARY KEY," +
                "alliance_id TEXT NOT NULL," +
                "sender_id TEXT NOT NULL," +
                "receiver_id TEXT NOT NULL," +
                "timestamp INTEGER NOT NULL," +
                "status TEXT NOT NULL," +
                "FOREIGN KEY(alliance_id) REFERENCES " + T_ALLIANCES + "(id) ON DELETE CASCADE," +
                "FOREIGN KEY(sender_id) REFERENCES " + T_USERS + "(id) ON DELETE CASCADE," +
                "FOREIGN KEY(receiver_id) REFERENCES " + T_USERS + "(id) ON DELETE CASCADE" +
                ")");

        db.execSQL("CREATE TABLE " + T_MESSAGES + " (" +
                "id TEXT PRIMARY KEY," +
                "alliance_id TEXT NOT NULL," +
                "sender_id TEXT NOT NULL," +
                "sender_username TEXT NOT NULL," +
                "message_text TEXT NOT NULL," +
                "timestamp INTEGER NOT NULL," +
                "FOREIGN KEY(alliance_id) REFERENCES " + T_ALLIANCES + "(id) ON DELETE CASCADE," +
                "FOREIGN KEY(sender_id) REFERENCES " + T_USERS + "(id) ON DELETE CASCADE" +
                ")");

        db.execSQL("CREATE TABLE " + T_INVENTORY + " (" +
                "inventory_id TEXT PRIMARY KEY," +
                "user_id TEXT NOT NULL," +
                "equipment_id TEXT NOT NULL," +
                "type TEXT NOT NULL," +
                "is_active INTEGER NOT NULL DEFAULT 0," +
                "uses_left INTEGER NOT NULL," +
                "current_bonus REAL NOT NULL," +
                "FOREIGN KEY(user_id) REFERENCES " + T_USERS + "(id) ON DELETE CASCADE" +
                ")");

        db.execSQL("CREATE TABLE " + T_BOSSES + " (" +
                "id TEXT PRIMARY KEY, " +
                "user_id TEXT NOT NULL, " +
                "max_hp REAL NOT NULL, " +
                "current_hp REAL NOT NULL, " +
                "is_defeated INTEGER NOT NULL, " +
                "level INTEGER NOT NULL, " +
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
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE " + T_USERS + " ADD COLUMN current_alliance_id  TEXT");
            db.execSQL("ALTER TABLE " + T_USERS + " ADD COLUMN fcm_token  TEXT");
            db.execSQL("CREATE TABLE " + T_ALLIANCES + " (" +
                    "id TEXT PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "leader_id TEXT NOT NULL, " +
                    "members_ids TEXT," +
                    "current_mission_id TEXT," +
                    "mission_status TEXT NOT NULL," +
                    "FOREIGN KEY(leader_id) REFERENCES " + T_USERS + "(id) ON DELETE CASCADE" +
                    ")");

            db.execSQL("CREATE TABLE " + T_INVITATIONS + " (" +
                    "id TEXT PRIMARY KEY," +
                    "alliance_id TEXT NOT NULL," +
                    "sender_id TEXT NOT NULL," +
                    "receiver_id TEXT NOT NULL," +
                    "timestamp INTEGER NOT NULL," +
                    "status TEXT NOT NULL," +
                    "FOREIGN KEY(alliance_id) REFERENCES " + T_ALLIANCES + "(id) ON DELETE CASCADE," +
                    "FOREIGN KEY(sender_id) REFERENCES " + T_USERS + "(id) ON DELETE CASCADE," +
                    "FOREIGN KEY(receiver_id) REFERENCES " + T_USERS + "(id) ON DELETE CASCADE" +
                    ")");
        }
        if (oldVersion < 7) {
            db.execSQL("CREATE TABLE " + T_MESSAGES + " (" +
                    "id TEXT PRIMARY KEY," +
                    "alliance_id TEXT NOT NULL," +
                    "sender_id TEXT NOT NULL," +
                    "sender_username TEXT NOT NULL," +
                    "message_text TEXT NOT NULL," +
                    "timestamp INTEGER NOT NULL," +
                    "FOREIGN KEY(alliance_id) REFERENCES " + T_ALLIANCES + "(id) ON DELETE CASCADE," +
                    "FOREIGN KEY(sender_id) REFERENCES " + T_USERS + "(id) ON DELETE CASCADE" +
                    ")");
        }
        if (oldVersion < 8 ) {
            db.execSQL("CREATE TABLE " + T_INVENTORY + " (" +
                    "inventory_id TEXT PRIMARY KEY," +
                    "user_id TEXT NOT NULL," +
                    "equipment_id TEXT NOT NULL," +
                    "type TEXT NOT NULL," +
                    "is_active INTEGER NOT NULL DEFAULT 0," +
                    "uses_left INTEGER NOT NULL," +
                    "current_bonus REAL NOT NULL," +
                    "FOREIGN KEY(user_id) REFERENCES " + T_USERS + "(id) ON DELETE CASCADE" +
                    ")");
        }
        if (oldVersion < 9) {
            db.execSQL("CREATE TABLE " + T_BOSSES + " (" +
                    "id TEXT PRIMARY KEY, " +
                    "user_id TEXT NOT NULL, " +
                    "max_hp REAL NOT NULL, " +
                    "current_hp REAL NOT NULL, " +
                    "is_defeated INTEGER NOT NULL, " +
                    "level INTEGER NOT NULL, " +
                    "FOREIGN KEY(user_id) REFERENCES " + T_USERS + "(id) ON DELETE CASCADE" +
                    ")");
        }
        if (oldVersion < 10) {
            db.execSQL("ALTER TABLE " + T_USERS + " ADD COLUMN previous_level_up_timestamp INTEGER");
            db.execSQL("ALTER TABLE " + T_USERS + " ADD COLUMN current_level_up_timestamp INTEGER");
        }
    }

}
