package com.team21.questify.application.model;

import com.google.firebase.firestore.DocumentId;
import com.team21.questify.application.model.enums.EquipmentId;
import com.team21.questify.application.model.enums.EquipmentType;

public class Equipment {
    @DocumentId
    private String inventoryId;
    private String userId;
    private EquipmentId equipmentId;
    private EquipmentType type;
    private boolean isActive;
    private int usesLeft;
    private double currentBonus;

    public Equipment() {}

    public Equipment(String userId, EquipmentId equipmentId, EquipmentType type, int uses, double baseBonus) {
        this.userId = userId;
        this.equipmentId = equipmentId;
        this.type = type;
        this.isActive = false;
        this.usesLeft = uses;
        this.currentBonus = baseBonus;
    }

    public String getInventoryId() { return inventoryId; }
    public void setInventoryId(String inventoryId) { this.inventoryId = inventoryId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public EquipmentId getEquipmentId() { return equipmentId; }
    public void setEquipmentId(EquipmentId equipmentId) { this.equipmentId = equipmentId; }
    public EquipmentType getType() { return type; }
    public void setType(EquipmentType type) { this.type = type; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public int getUsesLeft() { return usesLeft; }
    public void setUsesLeft(int usesLeft) { this.usesLeft = usesLeft; }
    public double getCurrentBonus() { return currentBonus; }
    public void setCurrentBonus(double currentBonus) { this.currentBonus = currentBonus; }
}
