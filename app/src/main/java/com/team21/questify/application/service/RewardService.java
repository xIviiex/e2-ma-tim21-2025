package com.team21.questify.application.service;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.team21.questify.application.model.User;
import com.team21.questify.application.model.enums.EquipmentId;
import com.team21.questify.data.repository.UserRepository;
import com.team21.questify.utils.LevelCalculator;

public class RewardService {

    private final EquipmentService equipmentService;
    private final UserService userService;
    private final UserRepository userRepository;

    public RewardService(Context context) {
        this.equipmentService = new EquipmentService(context);
        this.userService = new UserService(context);
        this.userRepository = new UserRepository(context);
    }


    public Task<Void> grantMissionRewards(@NonNull String userId) {

        return userRepository.getUserById(userId).continueWithTask(userTask -> {
            if (!userTask.isSuccessful() || userTask.getResult() == null) {

                throw new Exception("Failed to fetch user before granting rewards.");
            }
            User user = userTask.getResult();


            int coinsReward = LevelCalculator.getCoinsForLevel(user.getLevel()) / 2;


            EquipmentId potionId = EquipmentId.POTION_PP_20;
            EquipmentId clothingId = EquipmentId.ARMOR_SHIELD;


            Task<Void> potionTask = equipmentService.rewardEquipment(userId, potionId);
            Task<Void> clothingTask = equipmentService.rewardEquipment(userId, clothingId);


            user.setCoins(user.getCoins() + coinsReward);
            Task<Void> userUpdateTask = userRepository.updateUser(user);


            return Tasks.whenAll(potionTask, clothingTask, userUpdateTask);
        });
    }
}


