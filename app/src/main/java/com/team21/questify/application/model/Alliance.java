package com.team21.questify.application.model;

import com.team21.questify.application.model.enums.MissionStatus;

import java.util.ArrayList;
import java.util.List;

public class Alliance {
    private String allianceId;
    private String name;
    private String leaderId;
    private List<String> membersIds;
    private String currentMissionId;
    private MissionStatus missionStatus;

    public Alliance() {}

    public Alliance(String allianceId, String name, String leaderId) {
        this.allianceId = allianceId;
        this.name = name;
        this.leaderId = leaderId;
        this.membersIds = new ArrayList<>();
        this.membersIds.add(leaderId);
        this.currentMissionId = "";
        this.missionStatus = MissionStatus.NOT_STARTED;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAllianceId() {
        return allianceId;
    }

    public void setAllianceId(String allianceId) {
        this.allianceId = allianceId;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }

    public List<String> getMembersIds() {
        return membersIds;
    }

    public void setMembersIds(List<String> membersIds) {
        this.membersIds = membersIds;
    }

    public String getCurrentMissionId() {
        return currentMissionId;
    }

    public void setCurrentMissionId(String currentMissionId) {
        this.currentMissionId = currentMissionId;
    }

    public MissionStatus getMissionStatus() {
        return missionStatus;
    }

    public void setMissionStatus(MissionStatus missionStatus) {
        this.missionStatus = missionStatus;
    }

}
