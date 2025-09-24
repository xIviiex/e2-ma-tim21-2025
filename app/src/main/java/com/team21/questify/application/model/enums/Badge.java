package com.team21.questify.application.model.enums;


public enum Badge {
    BRONZE_PARTICIPANT("Bronzani učesnik"),
    SILVER_CONTRIBUTOR("Srebrni doprinosilac"),
    GOLD_CONTRIBUTOR("Zlatni doprinosilac"),
    MISSION_MASTER("Gospodar misije");

    private final String badgeName;

    Badge(String name) {
        this.badgeName = name;
    }

    public String getBadgeName() {
        return badgeName;
    }
}