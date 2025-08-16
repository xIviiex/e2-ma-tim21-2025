package com.team21.questify.data.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "questify.db";
    public static final int DB_VERSION = 1;
    public static final String T_USERS = "users";

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
                "level INTEGER DEFAULT 1" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + T_USERS);
        onCreate(db);
    }

}
