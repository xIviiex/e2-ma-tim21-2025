package com.team21.questify.application.model;

import com.team21.questify.application.model.enums.RequestStatus;

public class Invitation {
    private String invitationId;
    private String allianceId;
    private String senderId;
    private String receiverId;
    private long timestamp;
    private RequestStatus status;

    public Invitation() {
    }

    public Invitation(String invitationId, String allianceId, String senderId, String receiverId) {
        this.invitationId = invitationId;
        this.allianceId = allianceId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.timestamp = System.currentTimeMillis();
        this.status = RequestStatus.PENDING;
    }

    public String getInvitationId() {
        return invitationId;
    }

    public void setInvitationId(String invitationId) {
        this.invitationId = invitationId;
    }

    public String getAllianceId() {
        return allianceId;
    }

    public void setAllianceId(String allianceId) {
        this.allianceId = allianceId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }
}
