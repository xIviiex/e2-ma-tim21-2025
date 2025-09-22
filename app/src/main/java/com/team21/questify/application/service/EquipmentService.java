package com.team21.questify.application.service;

import static com.team21.questify.application.model.enums.EquipmentType.ARMOR;
import static com.team21.questify.application.model.enums.EquipmentType.POTION;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.team21.questify.application.model.Equipment;
import com.team21.questify.application.model.User;
import com.team21.questify.application.model.enums.EquipmentId;
import com.team21.questify.application.model.enums.EquipmentType;
import com.team21.questify.data.repository.EquipmentRepository;
import com.team21.questify.data.repository.UserRepository;
import com.team21.questify.utils.EquipmentHelper;
import com.team21.questify.utils.LevelCalculator;

import java.util.List;
import java.util.stream.Collectors;

public class EquipmentService {
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;

    public EquipmentService(Context context) {
        this.equipmentRepository = new EquipmentRepository(context);
        this.userRepository = new UserRepository(context);
    }

    public Task<List<Equipment>> getInventory(String userId) {
        return equipmentRepository.getInventory(userId);
    }

    public Task<List<Equipment>> getActiveInventory(String userId) {
        return equipmentRepository.getInventory(userId).continueWith(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return task.getResult().stream()
                    .filter(Equipment::isActive)
                    .collect(Collectors.toList());
        });
    }

    public Task<Void> activateItem(Equipment item) {
        item.setActive(true);
        return equipmentRepository.updateItem(item);
    }

    public Task<Void> deactivateItem(Equipment item) {
        item.setActive(false);
        return equipmentRepository.updateItem(item);
    }

    public Task<Void> buyItem(String userId, EquipmentHelper.ShopItem shopItem) {
        return userRepository.getUserById(userId).continueWithTask(userTask -> {
            if (!userTask.isSuccessful() || userTask.getResult() == null) {
                throw new Exception("Could not retrieve user data.");
            }
            User user = userTask.getResult();

            int previousLevelReward = LevelCalculator.getCoinsForLevel(user.getLevel() - 1);
            int price = (int) (previousLevelReward * shopItem.priceMultiplier);

            if (user.getCoins() < price) {
                throw new Exception("Not enough coins! You need " + price + " coins.");
            }

            user.setCoins(user.getCoins() - price);

            Equipment newItem = createEquipmentFromShopItem(userId, shopItem);

            Task<Void> updateUserTask = userRepository.updateUser(user);
            Task<Void> addItemTask = equipmentRepository.addItem(newItem);

            return Tasks.whenAll(updateUserTask, addItemTask);
        });
    }

    private Equipment createEquipmentFromShopItem(String userId, EquipmentHelper.ShopItem shopItem) {
        int uses = 0;
        double baseBonus = 0;

        switch (shopItem.type) {
            case POTION:
                uses = 1;
                if (shopItem.equipmentId == EquipmentId.POTION_PP_20) baseBonus = 0.20;
                if (shopItem.equipmentId == EquipmentId.POTION_PP_40) baseBonus = 0.40;
                if (shopItem.equipmentId == EquipmentId.POTION_PP_PERMANENT_5) baseBonus = 0.05;
                if (shopItem.equipmentId == EquipmentId.POTION_PP_PERMANENT_10) baseBonus = 0.10;
                break;
            case ARMOR:
                uses = 2;
                if (shopItem.equipmentId == EquipmentId.ARMOR_GLOVES) baseBonus = 0.10;
                if (shopItem.equipmentId == EquipmentId.ARMOR_SHIELD) baseBonus = 0.10;
                if (shopItem.equipmentId == EquipmentId.ARMOR_BOOTS) baseBonus = 0.40;
                break;
        }
        return new Equipment(userId, shopItem.equipmentId, shopItem.type, uses, baseBonus);
    }

}
