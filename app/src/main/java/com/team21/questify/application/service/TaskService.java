package com.team21.questify.application.service;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.team21.questify.application.model.Task;
import com.team21.questify.application.model.TaskOccurrence;
import com.team21.questify.application.model.enums.TaskType;
import com.team21.questify.application.model.enums.TaskStatus;
import com.team21.questify.data.repository.TaskRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.UUID;

public class TaskService {
    private static final String TAG = "TaskService";
    private final TaskRepository repository;
    private final TaskOccurrenceService taskOccurrenceService;
    private final FirebaseAuth auth;


    public TaskService(Context context) {
        this.repository = new TaskRepository(context);
        this.auth = FirebaseAuth.getInstance();
        this.taskOccurrenceService = new TaskOccurrenceService(context);
    }


    public void createTask(Task newTask, OnCompleteListener<Void> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // Postavi userID za zadatak
            String userId = user.getUid();
            String taskId = UUID.randomUUID().toString();
            newTask.setUserId(userId);
            newTask.setId(taskId);
            repository.createTask(newTask, listener);
        } else {

            listener.onComplete(null);
        }
    }


    public void createOccurrences(Task task) {
        if (task.getTaskType() == TaskType.RECURRING) {
            createOccurrencesForRecurringTask(task);
        } else {
            createOccurrenceForOneTimeTask(task);
        }
    }



    public void createOccurrencesForRecurringTask(Task task) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(task.getRecurringStartDate());
        resetTimeToMidnight(calendar);

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTimeInMillis(task.getRecurringEndDate());
        resetTimeToMidnight(endCalendar);

        while (!calendar.after(endCalendar)) {
            long dateOnlyMillis = calendar.getTimeInMillis();

            TaskOccurrence occurrence = new TaskOccurrence();
            occurrence.setId(UUID.randomUUID().toString());
            occurrence.setTaskId(task.getId());
            occurrence.setUserId(userId);
            occurrence.setDate(dateOnlyMillis);  // SAMO datum, bez vremena
            occurrence.setStatus(TaskStatus.UNCOMPLETED);

            taskOccurrenceService.createTaskOccurrence(occurrence, result -> {
                if (result == null || !result.isSuccessful()) {
                    Log.e(TAG, "Failed to create task occurrence: " + (result != null ? result.getException() : "task is null"));
                }
            });

            switch (task.getRecurrenceUnit()) {
                case DAY:
                    calendar.add(Calendar.DAY_OF_MONTH, task.getRecurringInterval());
                    break;
                case WEEK:
                    calendar.add(Calendar.WEEK_OF_YEAR, task.getRecurringInterval());
                    break;
            }
            resetTimeToMidnight(calendar);
        }
    }


    private void resetTimeToMidnight(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private void createOccurrenceForOneTimeTask(Task task) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();

        TaskOccurrence occurrence = new TaskOccurrence();
        occurrence.setId(UUID.randomUUID().toString());
        occurrence.setTaskId(task.getId());
        occurrence.setUserId(userId);

        // Čuvamo samo datum, na primer kao danas na ponoć
        Calendar calendar = Calendar.getInstance();
        resetTimeToMidnight(calendar);

        long dateOnlyMillis = calendar.getTimeInMillis();

        occurrence.setDate(dateOnlyMillis);
        occurrence.setStatus(TaskStatus.UNCOMPLETED);

        taskOccurrenceService.createTaskOccurrence(occurrence, result -> {
            if (result == null || !result.isSuccessful()) {
                Log.e(TAG, "Failed to create one-time task occurrence: " + (result != null ? result.getException() : "result is null"));
            }
        });
    }


}
