package com.team21.questify.application.model;



import com.team21.questify.application.model.enums.RecurrenceUnit;

import com.team21.questify.application.model.enums.TaskDifficulty;

import com.team21.questify.application.model.enums.TaskPriority;
import com.team21.questify.application.model.enums.TaskType;
import com.team21.questify.utils.LevelCalculator;


public class Task {

    private String id;

    private String userId;

    private String name;

    private String description;

    private String taskCategoryId;

    private TaskType taskType;

    private RecurrenceUnit recurrenceUnit;

    private int recurringInterval;

    private Long recurringStartDate;
    private Long recurringEndDate;
    private Long executionTime;

    private TaskDifficulty taskDifficulty;

    private TaskPriority taskPriority;

    private int xp;


    public Task() {
    }

    public Task(String id, String userId, String name, String description, String taskCategoryId,
                TaskType taskType, RecurrenceUnit recurrenceUnit, int recurringInterval,
                Long recurringStartDate, Long recurringEndDate,
                TaskDifficulty taskDifficulty, TaskPriority taskPriority,
                Long executionTime, int xp) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.taskCategoryId = taskCategoryId;
        this.taskType = taskType;
        this.recurrenceUnit = recurrenceUnit;
        this.recurringInterval = recurringInterval;
        this.recurringStartDate = recurringStartDate;
        this.recurringEndDate = recurringEndDate;
        this.taskDifficulty = taskDifficulty;
        this.taskPriority = taskPriority;
        this.executionTime = executionTime;
        this.xp = xp;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTaskCategoryId() {
        return taskCategoryId;
    }

    public void setTaskCategoryId(String taskCategoryId) {
        this.taskCategoryId = taskCategoryId;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public RecurrenceUnit getRecurrenceUnit() {
        return recurrenceUnit;
    }

    public void setRecurrenceUnit(RecurrenceUnit recurrenceUnit) {
        this.recurrenceUnit = recurrenceUnit;
    }

    public int getRecurringInterval() {
        return recurringInterval;
    }

    public void setRecurringInterval(int recurringInterval) {
        this.recurringInterval = recurringInterval;
    }

    public Long getRecurringStartDate() {
        return recurringStartDate;
    }

    public void setRecurringStartDate(Long recurringStartDate) {
        this.recurringStartDate = recurringStartDate;
    }

    public Long getRecurringEndDate() {
        return recurringEndDate;
    }

    public void setRecurringEndDate(Long recurringEndDate) {
        this.recurringEndDate = recurringEndDate;
    }

    public Long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Long executionTime) {
        this.executionTime = executionTime;
    }

    public TaskDifficulty getTaskDifficulty() {
        return taskDifficulty;
    }

    public void setTaskDifficulty(TaskDifficulty taskDifficulty) {
        this.taskDifficulty = taskDifficulty;
    }

    public TaskPriority getTaskPriority() {
        return taskPriority;
    }

    public void setTaskPriority(TaskPriority taskPriority) {
        this.taskPriority = taskPriority;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    // Metoda za izračunavanje i postavljanje XP-a
    public void calculateAndSetXp(int currentLevel) {
        int difficultyXp = taskDifficulty != null ? LevelCalculator.getXpForDifficultyOrPriority(currentLevel, taskDifficulty.getXp()) : 0;
        int priorityXp = taskPriority != null ? LevelCalculator.getXpForDifficultyOrPriority(currentLevel, taskPriority.getXp()) : 0;
        this.xp = difficultyXp + priorityXp;
    }

}