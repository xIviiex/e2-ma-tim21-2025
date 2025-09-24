package com.team21.questify.data.firebase;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.team21.questify.application.model.TaskOccurrence;
import com.team21.questify.application.model.enums.TaskDifficulty;
import com.team21.questify.application.model.enums.TaskPriority;
import com.team21.questify.application.model.enums.TaskStatus;

import java.util.Calendar;
import java.util.Map;


public class TaskOccurrenceRemoteDataSource {

    private static final String OCCURRENCES_COLLECTION = "task_occurrences";
    private final FirebaseFirestore db;

    public TaskOccurrenceRemoteDataSource() {
        this.db = FirebaseFirestore.getInstance();
    }


    public void insertOccurrence(TaskOccurrence occurrence, OnCompleteListener<Void> listener) {
        if (occurrence.getId() != null) {
            db.collection(OCCURRENCES_COLLECTION)
                    .document(occurrence.getId())
                    .set(occurrence)
                    .addOnCompleteListener(listener);
        }
    }


    public void getAllOccurrencesForUser(String userId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection(OCCURRENCES_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(listener);
    }


    public void getOccurrencesByTaskId(String taskId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection(OCCURRENCES_COLLECTION)
                .whereEqualTo("taskId", taskId)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(listener);
    }

    public void findFutureOccurrences(String taskId, long fromDate, OnCompleteListener<QuerySnapshot> listener) {
        db.collection(OCCURRENCES_COLLECTION)
                .whereEqualTo("taskId", taskId)
                .whereGreaterThanOrEqualTo("date", fromDate)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(listener);
    }



    public void updateOccurrence(String occurrenceId, Map<String, Object> updates, OnCompleteListener<Void> listener) {
        db.collection(OCCURRENCES_COLLECTION)
                .document(occurrenceId)
                .update(updates)
                .addOnCompleteListener(listener);
    }


    public void updateOccurrenceTaskId(String occurrenceId, String newTaskId, OnCompleteListener<Void> listener) {
        db.collection(OCCURRENCES_COLLECTION)
                .document(occurrenceId)
                .update("taskId", newTaskId)
                .addOnCompleteListener(listener);
    }


    public void deleteOccurrence(String occurrenceId, OnCompleteListener<Void> listener) {
        db.collection(OCCURRENCES_COLLECTION)
                .document(occurrenceId)
                .delete()
                .addOnCompleteListener(listener);
    }

    public Task<QuerySnapshot> getOccurrencesForUser(String userId) {
        return db.collection("task_occurrences")
                .whereEqualTo("userId", userId)
                .get();
    }


    public void getTodaysCompletedOccurrencesForUser(String userId, OnCompleteListener<QuerySnapshot> listener) {

        Calendar calendar = Calendar.getInstance();


        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDayMillis = calendar.getTimeInMillis();


        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endOfDayMillis = calendar.getTimeInMillis();


        db.collection(OCCURRENCES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "COMPLETED")
                .whereGreaterThanOrEqualTo("date", startOfDayMillis)
                .whereLessThanOrEqualTo("date", endOfDayMillis)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(listener);
    }


    public void getCompletedOccurrencesForDateRange(String userId, long fromDate, long toDate, OnCompleteListener<QuerySnapshot> listener) {
        db.collection(OCCURRENCES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", TaskStatus.COMPLETED.name())
                .whereGreaterThanOrEqualTo("date", fromDate)
                .whereLessThanOrEqualTo("date", toDate)
                .get()
                .addOnCompleteListener(listener);
    }


    public void getOldActiveOccurrences(String userId, OnCompleteListener<QuerySnapshot> listener) {

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -3);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long threeDaysAgoMillis = calendar.getTimeInMillis();


        db.collection(OCCURRENCES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", TaskStatus.ACTIVE.name())
                .whereLessThan("date", threeDaysAgoMillis)
                .get()
                .addOnCompleteListener(listener);
    }


    // =================================================================
    // BOSS FIGHT
    // =================================================================

    public Task<QuerySnapshot> getCompletedOccurrencesInDateRange(String userId, long fromDate, long toDate) {
        return db.collection(OCCURRENCES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", TaskStatus.COMPLETED.name())
                .whereGreaterThanOrEqualTo("date", fromDate)
                .whereLessThan("date", toDate) // Koristimo 'lessThan' za preciznost
                .get();
    }

    public Task<QuerySnapshot> getUncompletedOccurrencesInDateRange(String userId, long fromDate, long toDate) {
        return db.collection(OCCURRENCES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", TaskStatus.UNCOMPLETED.name()) // Ključna razlika
                .whereGreaterThanOrEqualTo("date", fromDate)
                .whereLessThan("date", toDate)
                .get();
    }


    // =================================================================
    // NEW METHODS FOR XP QUOTA CHECKING
    // =================================================================


    public void getTodaysCompletedTaskCountByDifficulty(String userId, TaskDifficulty difficulty, OnCompleteListener<QuerySnapshot> listener) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDay = calendar.getTimeInMillis();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endOfDay = calendar.getTimeInMillis();

        db.collection(OCCURRENCES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", TaskStatus.COMPLETED.name())
                .whereEqualTo("taskDifficulty", difficulty.name()) // Samo po težini
                .whereGreaterThanOrEqualTo("date", startOfDay)
                .whereLessThanOrEqualTo("date", endOfDay)
                .get().addOnCompleteListener(listener);
    }


    public void getTodaysCompletedTaskCountByPriority(String userId, TaskPriority priority, OnCompleteListener<QuerySnapshot> listener) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDay = calendar.getTimeInMillis();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endOfDay = calendar.getTimeInMillis();

        db.collection(OCCURRENCES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", TaskStatus.COMPLETED.name())
                .whereEqualTo("taskPriority", priority.name()) // Samo po prioritetu
                .whereGreaterThanOrEqualTo("date", startOfDay)
                .whereLessThanOrEqualTo("date", endOfDay)
                .get().addOnCompleteListener(listener);
    }


    public void getThisWeeksCompletedTaskCount(String userId, TaskDifficulty difficulty, OnCompleteListener<QuerySnapshot> listener) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfWeek = calendar.getTimeInMillis();

        calendar.add(Calendar.WEEK_OF_YEAR, 1);
        calendar.add(Calendar.DAY_OF_WEEK, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endOfWeek = calendar.getTimeInMillis();

        db.collection(OCCURRENCES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", TaskStatus.COMPLETED.name())
                .whereEqualTo("taskDifficulty", difficulty.name()) // Requires denormalized data
                .whereGreaterThanOrEqualTo("date", startOfWeek)
                .whereLessThanOrEqualTo("date", endOfWeek)
                .get()
                .addOnCompleteListener(listener);
    }

    public void getThisMonthsCompletedTaskCount(String userId, TaskPriority priority, OnCompleteListener<QuerySnapshot> listener) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfMonth = calendar.getTimeInMillis();

        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        long endOfMonth = calendar.getTimeInMillis();

        db.collection(OCCURRENCES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", TaskStatus.COMPLETED.name())
                .whereEqualTo("taskPriority", priority.name()) // Requires denormalized data
                .whereGreaterThanOrEqualTo("date", startOfMonth)
                .whereLessThanOrEqualTo("date", endOfMonth)
                .get()
                .addOnCompleteListener(listener);
    }


}
