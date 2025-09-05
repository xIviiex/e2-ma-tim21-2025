package com.team21.questify.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.team21.questify.application.model.Invitation;
import com.team21.questify.application.model.enums.RequestStatus;

import java.util.ArrayList;
import java.util.List;

public class InvitationLocalDataSource {

    private final DatabaseHelper helper;

    public InvitationLocalDataSource(Context ctx) {
        this.helper = new DatabaseHelper(ctx);
    }

    public void insertInvitation(Invitation invitation) {
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put("id", invitation.getInvitationId());
            cv.put("alliance_id", invitation.getAllianceId());
            cv.put("sender_id", invitation.getSenderId());
            cv.put("receiver_id", invitation.getReceiverId());
            cv.put("timestamp", invitation.getTimestamp());
            cv.put("status", invitation.getStatus().name());
            db.insertWithOnConflict(DatabaseHelper.T_INVITATIONS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } finally {
            if (db != null) db.close();
        }
    }

    public List<Invitation> getInvitationsByReceiverId(String receiverId) {
        List<Invitation> invitationList = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(DatabaseHelper.T_INVITATIONS, null, "receiver_id=?", new String[]{receiverId}, null, null, "timestamp DESC");
            if (c != null && c.moveToFirst()) {
                do {
                    invitationList.add(cursorToInvitation(c));
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return invitationList;
    }

    public void updateInvitationStatus(String invitationId, RequestStatus status) {
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put("status", status.name());
            db.update(DatabaseHelper.T_INVITATIONS, cv, "id=?", new String[]{invitationId});
        } finally {
            if (db != null) db.close();
        }
    }


    private Invitation cursorToInvitation(Cursor c) {
        Invitation invitation = new Invitation();
        invitation.setInvitationId(c.getString(c.getColumnIndexOrThrow("id")));
        invitation.setAllianceId(c.getString(c.getColumnIndexOrThrow("alliance_id")));
        invitation.setSenderId(c.getString(c.getColumnIndexOrThrow("sender_id")));
        invitation.setReceiverId(c.getString(c.getColumnIndexOrThrow("receiver_id")));
        invitation.setTimestamp(c.getLong(c.getColumnIndexOrThrow("timestamp")));
        invitation.setStatus(RequestStatus.valueOf(c.getString(c.getColumnIndexOrThrow("status"))));
        return invitation;
    }
}
