package com.team21.questify.application.model;

public class BattleStats {
    private double powerPointsMultiplier = 1.0;
    private double hitChanceBonus = 0.0;
    private double extraAttackChance = 0.0;
    private double coinsMultiplier = 1.0;

    public BattleStats() {}

    public double getPowerPointsMultiplier() { return powerPointsMultiplier; }
    public void setPowerPointsMultiplier(double powerPointsMultiplier) { this.powerPointsMultiplier = powerPointsMultiplier; }
    public double getHitChanceBonus() { return hitChanceBonus; }
    public void setHitChanceBonus(double hitChanceBonus) { this.hitChanceBonus = hitChanceBonus; }
    public double getExtraAttackChance() { return extraAttackChance; }
    public void setExtraAttackChance(double extraAttackChance) { this.extraAttackChance = extraAttackChance; }

    public double getCoinsMultiplier() {
        return coinsMultiplier;
    }

    public void setCoinsMultiplier(double coinsMultiplier) {
        this.coinsMultiplier = coinsMultiplier;
    }
    public void addPowerPointsBonus(double bonus) {
        this.powerPointsMultiplier += bonus;
    }
    public void addCoinsBonus(double bonus) {
        this.coinsMultiplier += bonus;
    }


}
