package com.team21.questify.data.repository;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.team21.questify.application.model.Task;
import com.team21.questify.application.model.TaskOccurrence;
import com.team21.questify.application.model.enums.TaskDifficulty;
import com.team21.questify.data.database.DatabaseHelper;
import com.team21.questify.data.database.TaskLocalDataSource;
import com.team21.questify.data.firebase.TaskRemoteDataSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskRepository {
    private final TaskLocalDataSource localDataSource;
    private final TaskRemoteDataSource remoteDataSource;

    public TaskRepository(Context context) {
        this.localDataSource = new TaskLocalDataSource(context);
        this.remoteDataSource = new TaskRemoteDataSource();
    }

    public void createTask(Task task, OnCompleteListener<Void> listener) {

        remoteDataSource.insertTask(task, taskRemote -> {
            if (!taskRemote.isSuccessful()) {
                Log.e("TaskRepository", "Failed to insert task to remote db: " + taskRemote.getException());

            }
            localDataSource.insertTask(task);
            listener.onComplete(taskRemote);
        });
    }


    public void getAllTasksForUser(String userId, OnCompleteListener<List<Task>> listener) {
        List<Task> localTasks = localDataSource.getAllTasksForUser(userId);

        remoteDataSource.getAllTasksForUser(userId, taskRemote -> {
            if (taskRemote.isSuccessful() && taskRemote.getResult() != null) {
                List<Task> remoteTasks = new ArrayList<>();
                for (QueryDocumentSnapshot document : taskRemote.getResult()) {
                    Task task = document.toObject(Task.class);
                    remoteTasks.add(task);
                    localDataSource.insertTask(task);
                }
                listener.onComplete(Tasks.forResult(remoteTasks));
            } else {
                if (!localTasks.isEmpty()) {
                    listener.onComplete(Tasks.forResult(localTasks));
                } else {
                    listener.onComplete(Tasks.forException(taskRemote.getException()));
                }
            }
        });
    }

    public void getTaskById(String taskId, OnCompleteListener<Task> listener) {

        Task localTask = localDataSource.getTaskById(taskId);
        if (localTask != null) {

            listener.onComplete(Tasks.forResult(localTask));
            return;
        }


        remoteDataSource.getTaskById(taskId, remoteTaskResult -> {
            if (remoteTaskResult.isSuccessful() && remoteTaskResult.getResult() != null && remoteTaskResult.getResult().exists()) {

                Task remoteTask = remoteTaskResult.getResult().toObject(Task.class);

                if (remoteTask != null) {

                    localDataSource.insertTask(remoteTask);
                    listener.onComplete(Tasks.forResult(remoteTask));
                } else {
                    listener.onComplete(Tasks.forException(new Exception("Failed to convert remote data to Task object.")));
                }

            } else {

                Exception exception = remoteTaskResult.getException() != null ? remoteTaskResult.getException() : new Exception("Task not found.");
                listener.onComplete(Tasks.forException(exception));
            }
        });
    }



    public Map<String, Integer> getCompletedTaskCountsByCategory(String userId) {
        return localDataSource.getCompletedTaskCountsByCategory(userId);
    }

    public Map<Long, TaskDifficulty> getCompletedTaskDifficultiesWithDates(String userId) {
        Map<Long, String> difficultyStrings = localDataSource.getCompletedTaskDifficultiesWithDates(userId);
        Map<Long, TaskDifficulty> difficulties = new HashMap<>();

        for (Map.Entry<Long, String> entry : difficultyStrings.entrySet()) {
            try {
                difficulties.put(entry.getKey(), TaskDifficulty.valueOf(entry.getValue()));
            } catch (IllegalArgumentException e) {
                Log.e("TaskRepository", "Unknown task difficulty: " + entry.getValue());
            }
        }
        return difficulties;
    }

    public Map<String, Integer> getWeeklyXp(String userId) {
        return localDataSource.getWeeklyXp(userId);
    }


    private Map<String, Object> taskToMap(Task task) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", task.getUserId());
        map.put("name", task.getName());
        map.put("description", task.getDescription());
        map.put("taskCategoryId", task.getTaskCategoryId());
        map.put("taskType", task.getTaskType().name());
        map.put("recurrenceUnit", task.getRecurrenceUnit() != null ? task.getRecurrenceUnit().name() : null);
        map.put("recurringInterval", task.getRecurringInterval());
        map.put("recurringStartDate", task.getRecurringStartDate());
        map.put("recurringEndDate", task.getRecurringEndDate());
        map.put("executionTime", task.getExecutionTime());
        map.put("taskDifficulty", task.getTaskDifficulty().name());
        map.put("taskPriority", task.getTaskPriority().name());
        map.put("xp", task.getXp());
        return map;
    }


    public void updateTask(Task task, OnCompleteListener<Void> listener) {

        Map<String, Object> updates = taskToMap(task);

        remoteDataSource.updateTask(task.getId(), updates, taskRemote -> {
            if (taskRemote.isSuccessful()) {

                localDataSource.updateTask(task);
            } else {
                Log.e("TaskRepository", "Failed to update task on remote db: " + taskRemote.getException());
            }
            listener.onComplete(taskRemote);
        });
    }

    public void updateTaskEndDate(String taskId, Long newEndDate, OnCompleteListener<Void> listener) {
        remoteDataSource.updateTaskEndDate(taskId, newEndDate, taskRemote -> {
            if (taskRemote.isSuccessful()) {

                localDataSource.updateTaskEndDate(taskId, newEndDate);
            } else {
                Log.e("TaskRepository", "Failed to update task end date on remote db: " + taskRemote.getException());
            }
            listener.onComplete(taskRemote);
        });
    }

}
