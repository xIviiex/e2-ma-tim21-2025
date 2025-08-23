package com.team21.questify.application.service;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.team21.questify.application.model.TaskOccurrence;
import com.team21.questify.data.repository.TaskOccurrenceRepository;

import java.util.UUID;

public class TaskOccurrenceService {
    private static final String TAG = "TaskOccurrenceService";
    private final TaskOccurrenceRepository repository;
    private final FirebaseAuth auth;

    public TaskOccurrenceService(Context context) {
        this.repository = new TaskOccurrenceRepository(context);
        this.auth = FirebaseAuth.getInstance();
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
}
