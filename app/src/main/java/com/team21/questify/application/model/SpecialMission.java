package com.team21.questify.application.model;

import com.team21.questify.application.model.enums.MissionStatus;
import java.util.HashMap;
import java.util.Map;


public class SpecialMission {

    private String missionId;
    private String allianceId;
    private MissionStatus missionStatus;
    private int initialBossHp;
    private int currentBossHp;
    private Long startTime;
    private Long endTime;

    private Map<String, SpecialMissionUser> participantsProgress;


    public SpecialMission() {
        this.participantsProgress = new HashMap<>();
    }



    public String getMissionId() {
        return missionId;
    }

    public void setMissionId(String missionId) {
        this.missionId = missionId;
    }

    public String getAllianceId() {
        return allianceId;
    }

    public void setAllianceId(String allianceId) {
        this.allianceId = allianceId;
    }

    public MissionStatus getMissionStatus() {
        return missionStatus;
    }

    public void setMissionStatus(MissionStatus missionStatus) {
        this.missionStatus = missionStatus;
    }

    public int getInitialBossHp() {
        return initialBossHp;
    }

    public void setInitialBossHp(int initialBossHp) {
        this.initialBossHp = initialBossHp;
    }

    public int getCurrentBossHp() {
        return currentBossHp;
    }

    public void setCurrentBossHp(int currentBossHp) {
        this.currentBossHp = currentBossHp;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public Map<String, SpecialMissionUser> getParticipantsProgress() {
        return participantsProgress;
    }

    public void setParticipantsProgress(Map<String, SpecialMissionUser> participantsProgress) {
        this.participantsProgress = participantsProgress;
    }
}