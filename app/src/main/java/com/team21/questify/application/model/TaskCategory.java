package com.team21.questify.application.model;

public class TaskCategory {
    private String id;
    private String userId;
    private String name;
    private String hexColor;

    // Prazan konstruktor je neophodan za neke ORM-ove i Firebase
    public TaskCategory() {
    }

    public TaskCategory(String id, String userId, String name, String hexColor) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.hexColor = hexColor;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHexColor() {
        return hexColor;
    }

    public void setHexColor(String hexColor) {
        this.hexColor = hexColor;
    }
}
