package com.team21.questify.utils;

import com.team21.questify.R;
import com.team21.questify.application.model.Equipment;
import com.team21.questify.application.model.enums.EquipmentId;
import com.team21.questify.application.model.enums.EquipmentType;

import java.util.ArrayList;
import java.util.List;

public class EquipmentHelper {
    public static String getName(EquipmentId id) {
        switch (id) {
            case POTION_PP_20: return "Potion of Power (+20%)";
            case POTION_PP_40: return "Greater Potion of Power (+40%)";
            case POTION_PP_PERMANENT_5: return "Elixir of Strength (+5% perm.)";
            case POTION_PP_PERMANENT_10: return "Greater Elixir of Strength (+10% perm.)";
            case ARMOR_GLOVES: return "Gloves of Power (+10% PP)";
            case ARMOR_SHIELD: return "Shield of Accuracy (+10% hit chance)";
            case ARMOR_BOOTS: return "Boots of Swiftness (+40% extra attack)";
            case WEAPON_SWORD: return "Sword of Power";
            case WEAPON_BOW: return "Bow of Greed";
            default: return "Unknown Item";
        }
    }

    public static String getBonusText(Equipment item) {
        switch (item.getEquipmentId()) {
            case WEAPON_SWORD:
                return String.format("+%.2f%% Permanent Power", item.getCurrentBonus() * 100);
            case WEAPON_BOW:
                return String.format("+%.2f%% Permanent Coins", item.getCurrentBonus() * 100);
            default:
                return "";
        }
    }

    public static int getIcon(EquipmentId id) {
        switch (id) {
            case POTION_PP_20:
            case POTION_PP_40:
            case POTION_PP_PERMANENT_5:
            case POTION_PP_PERMANENT_10:
                return R.drawable.ic_potion;
            case ARMOR_GLOVES:
                return R.drawable.ic_gloves;
            case ARMOR_SHIELD:
                return R.drawable.ic_shield;
            case ARMOR_BOOTS:
                return R.drawable.ic_boots;
            case WEAPON_SWORD:
                return R.drawable.ic_sword;
            case WEAPON_BOW:
                return R.drawable.ic_bow;
            default:
                return R.drawable.ic_help;
        }
    }

    public static class ShopItem {
        public EquipmentId equipmentId;
        public String name;
        public String description;
        public double priceMultiplier;
        public EquipmentType type;

        public ShopItem() {
        }

        public ShopItem(EquipmentId id, EquipmentType type, String name, String desc, double priceMultiplier) {
            this.equipmentId = id;
            this.type = type;
            this.name = name;
            this.description = desc;
            this.priceMultiplier = priceMultiplier;
        }

        public static List<ShopItem> getShopItems() {
            List<ShopItem> items = new ArrayList<>();

            items.add(new ShopItem(EquipmentId.POTION_PP_20, EquipmentType.POTION, "Potion of Power", "+20% PP for one battle.", 0.5));
            items.add(new ShopItem(EquipmentId.POTION_PP_40, EquipmentType.POTION, "Greater Potion of Power", "+40% PP for one battle.", 0.7));
            items.add(new ShopItem(EquipmentId.POTION_PP_PERMANENT_5, EquipmentType.POTION, "Elixir of Strength", "+5% permanent PP.", 2.0));
            items.add(new ShopItem(EquipmentId.POTION_PP_PERMANENT_10, EquipmentType.POTION, "Greater Elixir of Strength", "+10% permanent PP.", 10.0));

            items.add(new ShopItem(EquipmentId.ARMOR_GLOVES, EquipmentType.ARMOR, "Gloves of Power", "+10% PP. Lasts 2 battles.", 0.6));
            items.add(new ShopItem(EquipmentId.ARMOR_SHIELD, EquipmentType.ARMOR, "Shield of Accuracy", "+10% hit chance. Lasts 2 battles.", 0.6));
            items.add(new ShopItem(EquipmentId.ARMOR_BOOTS, EquipmentType.ARMOR, "Boots of Swiftness", "+40% extra attack chance. Lasts 2 battles.", 0.8));

            return items;
        }
    }
}
