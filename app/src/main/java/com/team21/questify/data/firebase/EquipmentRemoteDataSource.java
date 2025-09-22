package com.team21.questify.data.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.team21.questify.application.model.Equipment;

public class EquipmentRemoteDataSource {
    private final FirebaseFirestore db;
    private static final String INVENTORY_COLLECTION = "inventory";

    public EquipmentRemoteDataSource() {
        this.db = FirebaseFirestore.getInstance();
    }

    private CollectionReference getInventoryCollection(String userId) {
        return db.collection("users").document(userId).collection(INVENTORY_COLLECTION);
    }

    public Task<DocumentReference> addItem(Equipment item) {
        return getInventoryCollection(item.getUserId()).add(item);
    }

    public Task<Void> updateItem(Equipment item) {
        return getInventoryCollection(item.getUserId()).document(item.getInventoryId()).set(item);
    }

    public Task<Void> deleteItem(String userId, String inventoryId) {
        return getInventoryCollection(userId).document(inventoryId).delete();
    }

    public Task<QuerySnapshot> getInventory(String userId) {
        return getInventoryCollection(userId).get();
    }
}
