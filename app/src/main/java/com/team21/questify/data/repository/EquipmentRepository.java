package com.team21.questify.data.repository;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.team21.questify.application.model.Equipment;
import com.team21.questify.data.database.EquipmentLocalDataSource;
import com.team21.questify.data.firebase.EquipmentRemoteDataSource;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class EquipmentRepository {
    private final EquipmentRemoteDataSource remoteDataSource;
    private final EquipmentLocalDataSource localDataSource;
    private final Executor executor;

    public EquipmentRepository(Context context) {
        this.remoteDataSource = new EquipmentRemoteDataSource();
        this.localDataSource = new EquipmentLocalDataSource(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public Task<List<Equipment>> getInventory(String userId) {
        return remoteDataSource.getInventory(userId).onSuccessTask(querySnapshot ->
                Tasks.call(executor, () -> {
                    List<Equipment> remoteItems = querySnapshot.toObjects(Equipment.class);

                    localDataSource.deleteAllItemsForUser(userId);
                    for (Equipment item : remoteItems) {
                        localDataSource.saveOrUpdateItem(item);
                    }

                    return localDataSource.getInventoryForUser(userId);
                })
        );
    }

    public Task<Void> addItem(Equipment item) {
        return remoteDataSource.addItem(item)
                .onSuccessTask(documentReference -> {
                    String newId = documentReference.getId();
                    item.setInventoryId(newId);
                    return Tasks.call(executor, () -> {
                        localDataSource.saveOrUpdateItem(item);
                        return null;
                    });
                });
    }

    public Task<Void> updateItem(Equipment item) {
        return remoteDataSource.updateItem(item).onSuccessTask(aVoid ->
                Tasks.call(executor, () -> {
                    localDataSource.saveOrUpdateItem(item);
                    return null;
                })
        );
    }

    public Task<Void> deleteItem(String userId, String inventoryId) {
        return remoteDataSource.deleteItem(userId, inventoryId).onSuccessTask(aVoid ->
                Tasks.call(executor, () -> {
                    localDataSource.deleteItem(inventoryId);
                    return null;
                })
        );
    }
}
