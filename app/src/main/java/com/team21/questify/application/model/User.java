package com.team21.questify.application.model;


import java.util.ArrayList;
import java.util.List;

public class User {
    private String userId;
    private String username;
    private String email;
    private String avatarName;
    private int level = 1;
    private int xp = 0;
    private boolean isActivated;
    private Long createdAt;
    private String title = "Adventurer";
    private int powerPoints = 0;
    private int coins = 0;
    private List<String> badges = new ArrayList<>();
    private List<String> equipment = new ArrayList<>();

    public User() {}

    public User(String userId, String username, String email, String avatarName, int level, int xp, boolean isActivated, Long createdAt) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.avatarName = avatarName;
        this.level = level;
        this.xp = xp;
        this.isActivated = isActivated;
        this.createdAt = createdAt;
    }

    public User(String userId, String username, String email, String avatarName, int level, int xp, boolean isActivated,
                Long createdAt, String title, int powerPoints, int coins, List<String> badges, List<String> equipment) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.avatarName = avatarName;
        this.level = level;
        this.xp = xp;
        this.isActivated = isActivated;
        this.createdAt = createdAt;
        this.title = title;
        this.powerPoints = powerPoints;
        this.coins = coins;
        this.badges = badges;
        this.equipment = equipment;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAvatarName() { return avatarName; }
    public void setAvatarName(String avatarName) { this.avatarName = avatarName; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }
    public boolean isActivated() { return isActivated; }
    public void setIsActivated(boolean isActivated) { this.isActivated = isActivated; }
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getPowerPoints() { return powerPoints; }
    public void setPowerPoints(int powerPoints) { this.powerPoints = powerPoints; }
    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }
    public List<String> getBadges() { return badges; }
    public void setBadges(List<String> badges) { this.badges = badges; }
    public List<String> getEquipment() { return equipment; }
    public void setEquipment(List<String> equipment) { this.equipment = equipment; }
    public Integer getEquipmentCount() { return this.equipment.size(); }
    public Integer getBadgesCount() { return this.badges.size(); }
}
