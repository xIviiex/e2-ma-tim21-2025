package com.team21.questify.application.model;

import com.team21.questify.application.model.enums.Badge;
import java.util.ArrayList;
import java.util.List;

public class SpecialMissionUser {

    private String userId;
    private int storePurchases;
    private int successfulRegularBossHits;
    private int solvedVeryEasyEasyNormalOrImportantTasks;
    private int solvedOtherTasks;
    private boolean hasNoUnsolvedTasks;
    private List<Long> daysWithMessageSent;
    private List<Badge> earnedBadges;
    private int totalDamageContributed;

    public SpecialMissionUser() {
        this.daysWithMessageSent = new ArrayList<>();
        this.earnedBadges = new ArrayList<>();
        this.totalDamageContributed = 0;
    }



    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getStorePurchases() {
        return storePurchases;
    }

    public void setStorePurchases(int storePurchases) {
        this.storePurchases = storePurchases;
    }

    public int getSuccessfulRegularBossHits() {
        return successfulRegularBossHits;
    }

    public void setSuccessfulRegularBossHits(int successfulRegularBossHits) {
        this.successfulRegularBossHits = successfulRegularBossHits;
    }

    public int getSolvedVeryEasyEasyNormalOrImportantTasks() {
        return solvedVeryEasyEasyNormalOrImportantTasks;
    }

    public void setSolvedVeryEasyEasyNormalOrImportantTasks(int solvedVeryEasyEasyNormalOrImportantTasks) {
        this.solvedVeryEasyEasyNormalOrImportantTasks = solvedVeryEasyEasyNormalOrImportantTasks;
    }

    public int getSolvedOtherTasks() {
        return solvedOtherTasks;
    }

    public void setSolvedOtherTasks(int solvedOtherTasks) {
        this.solvedOtherTasks = solvedOtherTasks;
    }

    public boolean isHasNoUnsolvedTasks() {
        return hasNoUnsolvedTasks;
    }

    public void setHasNoUnsolvedTasks(boolean hasNoUnsolvedTasks) {
        this.hasNoUnsolvedTasks = hasNoUnsolvedTasks;
    }

    public List<Long> getDaysWithMessageSent() {
        return daysWithMessageSent;
    }

    public void setDaysWithMessageSent(List<Long> daysWithMessageSent) {
        this.daysWithMessageSent = daysWithMessageSent;
    }

    public List<Badge> getEarnedBadges() {
        return earnedBadges;
    }

    public void setEarnedBadges(List<Badge> earnedBadges) {
        this.earnedBadges = earnedBadges;
    }

    public int getTotalDamageContributed() {
        return totalDamageContributed;
    }

    public void setTotalDamageContributed(int totalDamageContributed) {
        this.totalDamageContributed = totalDamageContributed;
    }
}