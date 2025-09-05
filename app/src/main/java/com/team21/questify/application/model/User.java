package com.team21.questify.application.model;

import java.util.ArrayList;
import java.util.List;

public class User {
    public static final String DEFAULT_TITLE = "Adventurer";
    public static final int DEFAULT_LEVEL = 1;
    public static final int DEFAULT_XP = 0;
    public static final int DEFAULT_POWER_POINTS = 0;
    public static final int DEFAULT_COINS = 0;

    private String userId;
    private String username;
    private String email;
    private String avatarName;
    private int level = DEFAULT_LEVEL;
    private int xp = DEFAULT_XP;
    private boolean isActivated;
    private Long createdAt;
    private String title = DEFAULT_TITLE;
    private int powerPoints = DEFAULT_POWER_POINTS;
    private int coins = DEFAULT_COINS;
    private Long lastActiveDate;
    private int consecutiveActiveDays = 0;
    private List<String> friendsIds = new ArrayList<>();
    private String currentAllianceId;
    private String fcmToken;

    public User() {}

    public User(String userId, String username, String email, String avatarName, Long createdAt) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.avatarName = avatarName;
        this.createdAt = createdAt;
        this.isActivated = false;
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
    public void setActivated(boolean activated) { isActivated = activated; }
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getPowerPoints() { return powerPoints; }
    public void setPowerPoints(int powerPoints) { this.powerPoints = powerPoints; }
    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }
    public Long getLastActiveDate() { return lastActiveDate; }
    public void setLastActiveDate(Long lastActiveDate) { this.lastActiveDate = lastActiveDate; }
    public int getConsecutiveActiveDays() { return consecutiveActiveDays; }
    public void setConsecutiveActiveDays(int consecutiveActiveDays) { this.consecutiveActiveDays = consecutiveActiveDays; }
    public List<String> getFriendsIds() { return friendsIds; }
    public void setFriendsIds(List<String> friendsIds) { this.friendsIds = friendsIds; }
    public String getCurrentAllianceId() { return currentAllianceId; }
    public void setCurrentAllianceId(String currentAllianceId) { this.currentAllianceId = currentAllianceId; }
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
}
