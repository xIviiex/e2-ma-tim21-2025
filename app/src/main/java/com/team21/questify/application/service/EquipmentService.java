package com.team21.questify.application.service;

import static com.team21.questify.application.model.enums.EquipmentType.ARMOR;
import static com.team21.questify.application.model.enums.EquipmentType.POTION;
import static com.team21.questify.application.model.enums.EquipmentType.WEAPON;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.team21.questify.application.model.BattleStats;
import com.team21.questify.application.model.Equipment;
import com.team21.questify.application.model.User;
import com.team21.questify.application.model.enums.EquipmentId;
import com.team21.questify.application.model.enums.EquipmentType;
import com.team21.questify.data.repository.EquipmentRepository;
import com.team21.questify.data.repository.UserRepository;
import com.team21.questify.utils.EquipmentHelper;
import com.team21.questify.utils.LevelCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
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

            Equipment newItem = createNewEquipmentFromId(userId, shopItem.equipmentId);

            return userRepository.updateUser(user).continueWithTask(updateUserTask -> {
                if (!updateUserTask.isSuccessful()) {
                    throw Objects.requireNonNull(updateUserTask.getException());
                }
                return equipmentRepository.addItem(newItem);
            });
        });
    }

    public Task<Void> upgradeWeapon(String userId, Equipment weapon) {
        if (weapon.getType() != WEAPON) {
            return Tasks.forException(new IllegalArgumentException("Only weapons can be upgraded."));
        }

        return userRepository.getUserById(userId).continueWithTask(userTask -> {
            if (!userTask.isSuccessful() || userTask.getResult() == null) {
                throw new Exception("User not found.");
            }
            User user = userTask.getResult();

            int upgradeCost = calculateUpdatePrice(user.getLevel());

            if (user.getCoins() < upgradeCost) {
                throw new Exception("Not enough coins to upgrade! Cost: " + upgradeCost);
            }

            user.setCoins(user.getCoins() - upgradeCost);

            return userRepository.updateUser(user).continueWithTask(updateUserTask -> {
                if (!updateUserTask.isSuccessful()) {
                    throw updateUserTask.getException();
                }

                weapon.setCurrentBonus(weapon.getCurrentBonus() + 0.0001); // 0.01%

                return equipmentRepository.updateItem(weapon);
            });
        });
    }

    public int calculateUpdatePrice(int level) {
        return (int)(LevelCalculator.getCoinsForLevel(level - 1) * 0.6);
    }

    private Equipment createNewEquipmentFromId(String userId, EquipmentId id) {
        EquipmentType type;
        int uses;
        double baseBonus = getBaseBonusForItem(id);

        switch (id) {
            case ARMOR_GLOVES:
            case ARMOR_SHIELD:
            case ARMOR_BOOTS:
                type = EquipmentType.ARMOR;
                uses = 2;
                break;
            case WEAPON_SWORD:
            case WEAPON_BOW:
                type = EquipmentType.WEAPON;
                uses = -1;
                break;
            case POTION_PP_20:
            case POTION_PP_40:
                type = POTION;
                uses = 1;
                break;
            case POTION_PP_PERMANENT_5:
            case POTION_PP_PERMANENT_10:
                type = POTION;
                uses = -1;
                break;
            default:
                return null;
        }
        return new Equipment(userId, id, type, uses, baseBonus);
    }

    private double getBaseBonusForItem(EquipmentId id) {
        switch (id) {
            case ARMOR_GLOVES:
            case ARMOR_SHIELD:
            case POTION_PP_PERMANENT_10:
                return 0.10; // 10%
            case ARMOR_BOOTS:
            case POTION_PP_40:
                return 0.40; // 40%
            case WEAPON_BOW:
            case WEAPON_SWORD:
            case POTION_PP_PERMANENT_5:
                return 0.05; // 5%
            case POTION_PP_20: return 0.20; // 20%
            default: return 0.0;
        }
    }

    // logic for boss battle:
    // metoda kada korisnik iskoristi akriviranu opremu u borbi sa bosom - NAKON BORBE
    public Task<Void> processEquipmentAfterBossBattle(String userId) {
        return equipmentRepository.getInventory(userId).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();

            List<Equipment> inventory = task.getResult();
            List<Task<Void>> tasksToPerform = new ArrayList<>();

            for (Equipment item : inventory) {
                if (item.isActive()) {
                    boolean isConsumable = item.getType() == EquipmentType.ARMOR ||
                            item.getEquipmentId() == EquipmentId.POTION_PP_20 ||
                            item.getEquipmentId() == EquipmentId.POTION_PP_40;
                    if (isConsumable) {
                        item.setUsesLeft(item.getUsesLeft() - 1);
                        if (item.getUsesLeft() == 0) {
                            tasksToPerform.add(equipmentRepository.deleteItem(userId, item.getInventoryId()));
                        } else {
                            tasksToPerform.add(equipmentRepository.updateItem(item));
                        }
                    }
                }
            }

            return Tasks.whenAll(tasksToPerform);
        });
    }
    // metoda kada korisnik nakon borbe dobija nagradu nakon spec misije ili borbe sa bossom - napravi u zavisnosti
    // od toga za sta se poziva metoda listu sa mogucim nagradama (List<EquipmentId>) i metodi prosledi random vrednost
    // iz te liste (na osnovu verovatnoca koje imas ili kako vec) one 4 vrednosti za napitke ako je spec misija ili za
    // borbu sa bosom ona tri za odecu i oba oruzja
    public Task<Void> rewardEquipment(String userId, EquipmentId newItemId) {
        return equipmentRepository.getInventory(userId).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();

            List<Equipment> inventory = task.getResult();

            Equipment existingItem = inventory.stream()
                    .filter(it -> it.getEquipmentId() == newItemId)
                    .findFirst()
                    .orElse(null);

            if (existingItem != null) {
                if (existingItem.getType() == EquipmentType.ARMOR) {
                    double baseBonus = getBaseBonusForItem(newItemId);
                    existingItem.setCurrentBonus(existingItem.getCurrentBonus() + baseBonus);
                    existingItem.setUsesLeft(2);
                    return equipmentRepository.updateItem(existingItem);
                }
                else if (existingItem.getType() == WEAPON) {
                    existingItem.setCurrentBonus(existingItem.getCurrentBonus() + 0.0002);
                    return equipmentRepository.updateItem(existingItem);
                }
            }

            Equipment newItem = createNewEquipmentFromId(userId, newItemId);
            if (newItem != null) {
                return equipmentRepository.addItem(newItem);
            }

            return Tasks.forResult(null);
        });
    }

    // racuna bonuse koje nudi oprema i pozovi je PRE borbe, a onda menjaj u zavisnosti od toka borbe polja od
    // BattleStatsa (smanjuj, povecavaj)
    public Task<BattleStats> calculateBattleStats(String userId) {
        return getActiveInventory(userId).continueWith(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            List<Equipment> activeEquipment = task.getResult();
            BattleStats stats = new BattleStats();

            for (Equipment item : activeEquipment) {
                switch (item.getEquipmentId()) {
                    case POTION_PP_PERMANENT_5:
                    case POTION_PP_PERMANENT_10:
                    case ARMOR_GLOVES:
                    case WEAPON_SWORD:
                    case POTION_PP_20:
                    case POTION_PP_40:
                        stats.addPowerPointsBonus(item.getCurrentBonus());
                        break;
                    case ARMOR_SHIELD:
                        stats.setHitChanceBonus(stats.getHitChanceBonus() + item.getCurrentBonus());
                        break;
                    case ARMOR_BOOTS:
                        stats.setExtraAttackChance(stats.getExtraAttackChance() + item.getCurrentBonus());
                        break;
                    case WEAPON_BOW:
                        stats.addCoinsBonus(item.getCurrentBonus());
                        break;
                }
            }

            return stats;
        });
    }
}
