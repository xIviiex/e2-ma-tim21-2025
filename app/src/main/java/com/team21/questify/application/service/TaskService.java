package com.team21.questify.application.service;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class TaskService {
    private static final String TAG = "TaskService";
    private final TaskRepository repository;
    private final TaskOccurrenceService taskOccurrenceService;
    private final UserService userService;
    private final FirebaseAuth auth;


    public TaskService(Context context) {
        this.repository = new TaskRepository(context);
        this.auth = FirebaseAuth.getInstance();
        this.taskOccurrenceService = new TaskOccurrenceService(context);
        this.userService = new UserService(context);
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

        occurrence.setDate(task.getRecurringStartDate());
        occurrence.setStatus(TaskStatus.UNCOMPLETED);

        taskOccurrenceService.createTaskOccurrence(occurrence, result -> {
            if (result == null || !result.isSuccessful()) {
                Log.e(TAG, "Failed to create one-time task occurrence: " + (result != null ? result.getException() : "result is null"));
            }
        });
    }



    public void getAllTasksForCurrentUser(OnCompleteListener<List<Task>> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onComplete(Tasks.forException(new Exception("User not authenticated")));
            return;
        }

        String userId = user.getUid();

        repository.getAllTasksForUser(userId, taskResult -> {
            if (taskResult.isSuccessful()) {
                listener.onComplete(taskResult);
            } else {
                Log.e(TAG, "Failed to fetch tasks: " + taskResult.getException());
                listener.onComplete(taskResult);
            }
        });
    }

    public void getTaskById(String taskId, OnCompleteListener<Task> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onComplete(Tasks.forException(new Exception("User not authenticated")));
            return;
        }

        repository.getTaskById(taskId, listener);
    }


    private void updateOneTimeTask(Task updatedTask, OnCompleteListener<Void> listener) {
        // Pozivamo metodu iz servisa
        taskOccurrenceService.getOccurrencesByTaskId(updatedTask.getId(), occurrencesTask -> {
            if (!occurrencesTask.isSuccessful() || occurrencesTask.getResult().isEmpty()) {
                listener.onComplete(Tasks.forException(new Exception("Could not find occurrence for this task.")));
                return;
            }

            TaskOccurrence occurrence = occurrencesTask.getResult().get(0);
            if (occurrence.getStatus() == TaskStatus.COMPLETED) {
                listener.onComplete(Tasks.forException(new Exception("Cannot edit a completed task.")));
                return;
            }

            // 🚀 Fetch user profile i izračunaj XP pre update-a
            FirebaseUser firebaseUser = auth.getCurrentUser();
            if (firebaseUser == null) {
                listener.onComplete(Tasks.forException(new Exception("User not authenticated.")));
                return;
            }

            userService.fetchUserProfile(firebaseUser.getUid())
                    .addOnSuccessListener(user -> {
                        if (user != null) {
                            updatedTask.calculateAndSetXp(user.getLevel());
                        }
                        repository.updateTask(updatedTask, listener);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to fetch user profile for XP", e);
                        // i dalje radi update bez recalculacije XP
                        repository.updateTask(updatedTask, listener);
                    });
        });
    }



    private void updateRecurringTask(Task updatedTask, OnCompleteListener<Void> listener) {
        String originalTaskId = updatedTask.getId();
        long splitDate = System.currentTimeMillis();

        // 1. Pronađi sva buduća, nezavršena ponavljanja
        taskOccurrenceService.findFutureOccurrences(originalTaskId, splitDate, futureOccurrencesTask -> {
            if (!futureOccurrencesTask.isSuccessful()) {
                listener.onComplete(Tasks.forException(futureOccurrencesTask.getException()));
                return;
            }
            List<TaskOccurrence> futureOccurrences = futureOccurrencesTask.getResult();

            if (futureOccurrences.isEmpty()) {
                listener.onComplete(Tasks.forException(new Exception("No future occurrences to update.")));
                return;
            }

            // 2. Pronađi poslednje prošlo ponavljanje
            taskOccurrenceService.getOccurrencesByTaskId(originalTaskId, allOccurrencesTask -> {
                if (!allOccurrencesTask.isSuccessful()) {
                    listener.onComplete(Tasks.forException(allOccurrencesTask.getException()));
                    return;
                }
                List<TaskOccurrence> allOccurrences = allOccurrencesTask.getResult();
                TaskOccurrence lastPastOccurrence = allOccurrences.stream()
                        .filter(occ -> occ.getDate() < splitDate)
                        .max(Comparator.comparing(TaskOccurrence::getDate))
                        .orElse(null);

                Long newEndDateForOldTask = (lastPastOccurrence != null) ? lastPastOccurrence.getDate() : null;

                // 3. Ažuriraj krajnji datum starog zadatka
                repository.updateTaskEndDate(originalTaskId, newEndDateForOldTask, updateEndDateTask -> {
                    if (!updateEndDateTask.isSuccessful()) {
                        listener.onComplete(updateEndDateTask);
                        return;
                    }

                    // 4. Kreiraj novi Task asinhrono sa XP
                    createNewTaskFromUpdate(updatedTask, futureOccurrences.get(0).getDate(), newTaskResult -> {
                        if (!newTaskResult.isSuccessful()) {
                            listener.onComplete(Tasks.forException(newTaskResult.getException()));
                            return;
                        }

                        Task newTask = newTaskResult.getResult();

                        // 5. Sačuvaj novi task u repozitorijum
                        repository.createTask(newTask, createTask -> {
                            if (!createTask.isSuccessful()) {
                                listener.onComplete(createTask);
                                return;
                            }

                            // 6. Preveži buduća ponavljanja na novi Task
                            for (TaskOccurrence occurrence : futureOccurrences) {
                                taskOccurrenceService.updateOccurrenceTaskId(occurrence.getId(), newTask.getId(), updateIdTask -> {
                                    if (!updateIdTask.isSuccessful()) {
                                        Log.e(TAG, "Failed to update occurrence " + occurrence.getId());
                                    }
                                });
                            }

                            listener.onComplete(Tasks.forResult(null));
                        });
                    });
                });
            });
        });
    }


    public void updateTask(Task updatedTask, OnCompleteListener<Void> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onComplete(Tasks.forException(new Exception("User not authenticated.")));
            return;
        }

        if (updatedTask.getTaskType() == TaskType.RECURRING) {
            updateRecurringTask(updatedTask, listener);
        } else {
            updateOneTimeTask(updatedTask, listener);
        }
    }


    private void createNewTaskFromUpdate(Task updatedTask, long newStartDate, OnCompleteListener<Task> listener) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            listener.onComplete(Tasks.forException(new Exception("User not authenticated")));
            return;
        }

        Task newTask = new Task();
        newTask.setId(UUID.randomUUID().toString());
        newTask.setUserId(firebaseUser.getUid());

        // Preuzmi sve što se menja
        newTask.setName(updatedTask.getName());
        newTask.setDescription(updatedTask.getDescription());
        newTask.setExecutionTime(updatedTask.getExecutionTime());
        newTask.setTaskDifficulty(updatedTask.getTaskDifficulty());
        newTask.setTaskPriority(updatedTask.getTaskPriority());

        // Podesi recurrence parametre
        newTask.setTaskType(updatedTask.getTaskType());
        newTask.setRecurrenceUnit(updatedTask.getRecurrenceUnit());
        newTask.setRecurringInterval(updatedTask.getRecurringInterval());
        newTask.setRecurringStartDate(newStartDate);
        newTask.setRecurringEndDate(updatedTask.getRecurringEndDate());
        newTask.setTaskCategoryId(updatedTask.getTaskCategoryId());

        // Dohvati user level i setuj XP asinhrono
        userService.fetchUserProfile(firebaseUser.getUid())
                .addOnSuccessListener(user -> {
                    if (user != null) {
                        newTask.calculateAndSetXp(user.getLevel());
                    } else {
                        Log.w(TAG, "User profile not found, XP not set");
                    }
                    listener.onComplete(Tasks.forResult(newTask));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch user profile for XP", e);
                    // I dalje vraćamo task, ali XP može biti 0
                    listener.onComplete(Tasks.forResult(newTask));
                });
    }





}
