package com.team21.questify.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.team21.questify.application.model.Equipment;
import com.team21.questify.application.model.enums.EquipmentId;
import com.team21.questify.application.model.enums.EquipmentType;

import java.util.ArrayList;
import java.util.List;

public class EquipmentLocalDataSource {
    private final DatabaseHelper helper;

    public EquipmentLocalDataSource(Context context) {
        this.helper = new DatabaseHelper(context);
    }

    public void saveOrUpdateItem(Equipment item) {
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put("inventory_id", item.getInventoryId());
            cv.put("user_id", item.getUserId());
            cv.put("equipment_id", item.getEquipmentId().name());
            cv.put("type", item.getType().name());
            cv.put("is_active", item.isActive() ? 1 : 0);
            cv.put("uses_left", item.getUsesLeft());
            cv.put("current_bonus", item.getCurrentBonus());
            db.insertWithOnConflict(DatabaseHelper.T_INVENTORY, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } finally { /* Nema close() */ }
    }

    public List<Equipment> getInventoryForUser(String userId) {
        List<Equipment> inventory = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(DatabaseHelper.T_INVENTORY, null, "user_id=?", new String[]{userId}, null, null, null);
            if (c.moveToFirst()) {
                do {
                    inventory.add(cursorToEquipment(c));
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
        }
        return inventory;
    }

    public void deleteItem(String inventoryId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            db.delete(DatabaseHelper.T_INVENTORY, "inventory_id=?", new String[]{inventoryId});
        } finally {}
    }

    private Equipment cursorToEquipment(Cursor c) {
        Equipment item = new Equipment();
        item.setInventoryId(c.getString(c.getColumnIndexOrThrow("inventory_id")));
        item.setUserId(c.getString(c.getColumnIndexOrThrow("user_id")));
        item.setEquipmentId(EquipmentId.valueOf(c.getString(c.getColumnIndexOrThrow("equipment_id"))));
        item.setType(EquipmentType.valueOf(c.getString(c.getColumnIndexOrThrow("type"))));
        item.setActive(c.getInt(c.getColumnIndexOrThrow("is_active")) == 1);
        item.setUsesLeft(c.getInt(c.getColumnIndexOrThrow("uses_left")));
        item.setCurrentBonus(c.getDouble(c.getColumnIndexOrThrow("current_bonus")));
        return item;
    }

    public void deleteAllItemsForUser(String userId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            db.delete(DatabaseHelper.T_INVENTORY, "user_id=?", new String[]{userId});
        } finally { }
    }
}
