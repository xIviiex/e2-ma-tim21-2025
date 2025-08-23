package com.team21.questify.application.model;

import com.team21.questify.application.model.enums.TaskStatus;

public class TaskOccurrence {

    private String id;
    private String taskId;
    private String userId; // User reference
    private Long date;
    private TaskStatus status;

    public TaskOccurrence() {
    }

    public TaskOccurrence(String id, String taskId, String userId, Long date, TaskStatus status) {
        this.id = id;
        this.taskId = taskId;
        this.userId = userId;
        this.date = date;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }
}
