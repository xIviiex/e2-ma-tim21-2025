package com.team21.questify.application.model;

//==========================================================

//  DODAJ POLJE AKO SE VEC BORIO A NIJE GA POBEDIO DA STOJI TRUE KAKO SE NE BI PRIKAZIVAO DO SLEDECEG LEVELA

//===========================================================
public class Boss {
    private String bossId;
    private double maxHp;
    private double currentHp;
    private String userId;
    private Boolean isDefeated;
    private int level;


    public Boss() {
    }


    public Boss(String bossId, double maxHp, double currentHp, String userId, Boolean isDefeated, int level) {
        this.bossId = bossId;
        this.maxHp = maxHp;
        this.currentHp = currentHp;
        this.userId = userId;
        this.isDefeated = isDefeated;
        this.level = level;
    }

    // Getters and Setters

    public String getBossId() {
        return bossId;
    }

    public void setBossId(String bossId) {
        this.bossId = bossId;
    }

    public double getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(double maxHp) {
        this.maxHp = maxHp;
    }

    public double getCurrentHp() {
        return currentHp;
    }

    public void setCurrentHp(double currentHp) {
        this.currentHp = currentHp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Boolean getIsDefeated() {
        return isDefeated;
    }

    public void setIsDefeated(Boolean isDefeated) {
        this.isDefeated = isDefeated;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
