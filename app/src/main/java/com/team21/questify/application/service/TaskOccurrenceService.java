package com.team21.questify.application.service;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.team21.questify.application.model.TaskOccurrence;
import com.team21.questify.application.model.enums.TaskDifficulty;
import com.team21.questify.application.model.enums.TaskPriority;
import com.team21.questify.application.model.enums.TaskStatus;
import com.team21.questify.data.repository.TaskOccurrenceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaskOccurrenceService {
    private static final String TAG = "TaskOccurrenceService";
    private final TaskOccurrenceRepository repository;
    private final FirebaseAuth auth;

    public TaskOccurrenceService(Context context) {
        this.repository = new TaskOccurrenceRepository(context);
        this.auth = FirebaseAuth.getInstance();
    }

    public void getTodaysCompletedOccurrencesForCurrentUser(OnCompleteListener<List<TaskOccurrence>> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            // Ako korisnik nije ulogovan, odmah vrati grešku.
            Log.w(TAG, "Pokušaj dohvatanja završenih zadataka bez ulogovanog korisnika.");
            listener.onComplete(Tasks.forException(new Exception("User not authenticated")));
            return;
        }

        String userId = user.getUid();
        // Pozovi metodu iz repozitorijuma.
        repository.getTodaysCompletedOccurrencesForUser(userId, listener);
    }



    public void createTaskOccurrence(TaskOccurrence newOccurrence, OnCompleteListener<Void> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            String occurrenceId = UUID.randomUUID().toString();
            newOccurrence.setUserId(userId);
            newOccurrence.setId(occurrenceId);
            repository.createOccurrence(newOccurrence, listener);
        } else {
            Log.e(TAG, "User not authenticated. Cannot create TaskOccurrence.");
            listener.onComplete(null);
        }
    }

    public void getAllOccurrencesForCurrentUser(OnCompleteListener<List<TaskOccurrence>> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onComplete(Tasks.forException(new Exception("User not authenticated")));
            return;
        }

        String userId = user.getUid();

        repository.getAllOccurrencesForUser(userId, occurrenceResult -> {
            if (occurrenceResult.isSuccessful()) {
                listener.onComplete(occurrenceResult);
            } else {
                Log.e(TAG, "Failed to fetch task occurrences: " + occurrenceResult.getException());
                listener.onComplete(occurrenceResult);
            }
        });
    }


    public void getOccurrencesByTaskId(String taskId, OnCompleteListener<List<TaskOccurrence>> listener) {

        repository.getOccurrencesByTaskId(taskId, listener);
    }

    public void findFutureOccurrences(String taskId, long fromDate, OnCompleteListener<List<TaskOccurrence>> listener) {
        repository.findFutureOccurrences(taskId, fromDate, listener);
    }

    public void updateOccurrenceTaskId(String occurrenceId, String newTaskId, OnCompleteListener<Void> listener) {
        repository.updateOccurrenceTaskId(occurrenceId, newTaskId, listener);
    }

    public void deleteOccurrenceAndFutureOnes(String taskId, OnCompleteListener<Void> listener) {
        repository.deleteOccurrenceAndFutureOnes(taskId, listener);
    }





    public void updateOccurrenceStatus(String occurrenceId, TaskStatus status, OnCompleteListener<Void> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onComplete(Tasks.forException(new Exception("User not authenticated")));
            return;
        }
        repository.updateOccurrenceStatus(occurrenceId, status, listener);
    }


    public void updateOldOccurrences() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            repository.updateOldOccurrencesToUncompleted(currentUser.getUid());
        } else {
            Log.w("TaskOccurrenceService", "Cannot update old occurrences, no user is logged in.");
        }
    }

    // =================================================================
    // NEW METHODS FOR XP QUOTA CHECKING
    // =================================================================


    public void getTodaysCompletedTaskCountByDifficulty(TaskDifficulty difficulty, OnCompleteListener<Integer> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onComplete(Tasks.forException(new Exception("User not authenticated")));
            return;
        }
        repository.getTodaysCompletedTaskCountByDifficulty(user.getUid(), difficulty, listener);
    }

    public void getTodaysCompletedTaskCountByPriority(TaskPriority priority, OnCompleteListener<Integer> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onComplete(Tasks.forException(new Exception("User not authenticated")));
            return;
        }
        repository.getTodaysCompletedTaskCountByPriority(user.getUid(), priority, listener);
    }


    public void getThisWeeksCompletedTaskCount(TaskDifficulty difficulty, OnCompleteListener<Integer> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onComplete(Tasks.forException(new Exception("User not authenticated")));
            return;
        }
        repository.getThisWeeksCompletedTaskCount(user.getUid(), difficulty, listener);
    }


    public void getThisMonthsCompletedTaskCount(TaskPriority priority, OnCompleteListener<Integer> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onComplete(Tasks.forException(new Exception("User not authenticated")));
            return;
        }
        repository.getThisMonthsCompletedTaskCount(user.getUid(), priority, listener);
    }


}
