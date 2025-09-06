package com.team21.questify.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.team21.questify.application.model.ChatMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatLocalDataSource {
    private final DatabaseHelper helper;

    public ChatLocalDataSource(Context context) {
        this.helper = new DatabaseHelper(context);
    }

    public void insertOrUpdateMessage(ChatMessage message) {
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put("id", message.getMessageId());
            cv.put("alliance_id", message.getAllianceId());
            cv.put("sender_id", message.getSenderId());
            cv.put("sender_username", message.getSenderUsername());
            cv.put("message_text", message.getMessageText());
            cv.put("timestamp", message.getTimestamp().getTime());
            db.insertWithOnConflict(DatabaseHelper.T_MESSAGES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void insertOrUpdateMessages(List<ChatMessage> messages) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (ChatMessage message : messages) {
                ContentValues cv = new ContentValues();
                cv.put("id", message.getMessageId());
                cv.put("alliance_id", message.getAllianceId());
                cv.put("sender_id", message.getSenderId());
                cv.put("sender_username", message.getSenderUsername());
                cv.put("message_text", message.getMessageText());
                cv.put("timestamp", message.getTimestamp().getTime());
                db.insertWithOnConflict(DatabaseHelper.T_MESSAGES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<ChatMessage> getMessagesForAlliance(String allianceId) {
        List<ChatMessage> messages = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(DatabaseHelper.T_MESSAGES, null, "alliance_id=?", new String[]{allianceId}, null, null, "timestamp ASC");
            if (c.moveToFirst()) {
                do {
                    messages.add(cursorToMessage(c));
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
        }
        return messages;
    }

    private ChatMessage cursorToMessage(Cursor c) {
        ChatMessage msg = new ChatMessage();
        msg.setMessageId(c.getString(c.getColumnIndexOrThrow("id")));
        msg.setAllianceId(c.getString(c.getColumnIndexOrThrow("alliance_id")));
        msg.setSenderId(c.getString(c.getColumnIndexOrThrow("sender_id")));
        msg.setSenderUsername(c.getString(c.getColumnIndexOrThrow("sender_username")));
        msg.setMessageText(c.getString(c.getColumnIndexOrThrow("message_text")));
        msg.setTimestamp(new Date(c.getLong(c.getColumnIndexOrThrow("timestamp"))));
        return msg;
    }
}
