package com.team21.questify.data.repository;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.team21.questify.application.model.TaskOccurrence;
import com.team21.questify.application.model.enums.TaskDifficulty;
import com.team21.questify.application.model.enums.TaskPriority;
import com.team21.questify.application.model.enums.TaskStatus;
import com.team21.questify.data.database.DatabaseHelper;
import com.team21.questify.data.database.TaskOccurrenceLocalDataSource;
import com.team21.questify.data.firebase.TaskOccurrenceRemoteDataSource;


import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TaskOccurrenceRepository {
    private final TaskOccurrenceLocalDataSource localDataSource;
    private final TaskOccurrenceRemoteDataSource remoteDataSource;
    private final DatabaseHelper dbHelper;
    private final Executor executor;


    public TaskOccurrenceRepository(Context context) {
        this.localDataSource = new TaskOccurrenceLocalDataSource(context);
        this.remoteDataSource = new TaskOccurrenceRemoteDataSource();
        this.dbHelper = new DatabaseHelper(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void createOccurrence(TaskOccurrence occurrence, OnCompleteListener<Void> listener) {


        // Unos u udaljenu bazu
        remoteDataSource.insertOccurrence(occurrence, taskRemote -> {
            if (!taskRemote.isSuccessful()) {
                Log.e("TaskOccurrenceRepo", "Failed to insert occurrence to remote db: " + taskRemote.getException());

            }
            // Unos u lokalnu bazu
            localDataSource.insertTaskOccurrence(occurrence);
            listener.onComplete(taskRemote);
        });
    }


    public void getAllOccurrencesForUser(String userId, OnCompleteListener<List<TaskOccurrence>> listener) {
        List<TaskOccurrence> localOccurrences = localDataSource.getAllOccurrencesForUser(userId);

        remoteDataSource.getAllOccurrencesForUser(userId, taskRemote -> {
            if (taskRemote.isSuccessful() && taskRemote.getResult() != null) {
                List<TaskOccurrence> remoteOccurrences = new ArrayList<>();
                for (QueryDocumentSnapshot document : taskRemote.getResult()) {
                    TaskOccurrence occurrence = document.toObject(TaskOccurrence.class);
                    remoteOccurrences.add(occurrence);
                    localDataSource.insertTaskOccurrence(occurrence);
                }
                listener.onComplete(Tasks.forResult(remoteOccurrences));
            } else {
                // Ako Firestore nije uspeo, koristi lokalne ako ih ima
                if (!localOccurrences.isEmpty()) {
                    listener.onComplete(Tasks.forResult(localOccurrences));
                } else {
                    listener.onComplete(Tasks.forException(taskRemote.getException()));
                }
            }
        });
    }



    public Map<TaskStatus, Integer> getTaskCountsByStatus(String userId) {
        Map<String, Integer> stringCounts = localDataSource.getTaskCountsByStatus(userId);
        Map<TaskStatus, Integer> enumCounts = new HashMap<>();

        for (Map.Entry<String, Integer> entry : stringCounts.entrySet()) {
            try {
                TaskStatus status = TaskStatus.valueOf(entry.getKey());
                enumCounts.put(status, entry.getValue());
            } catch (IllegalArgumentException e) {
                Log.e("TaskOccurrenceRepository", "Unknown status: " + entry.getKey());
            }
        }

        return enumCounts;
    }

    public Task<List<TaskOccurrence>> getTaskOccurrencesByUserIdSortedByDate(String userId) {
        return remoteDataSource.getOccurrencesForUser(userId)
                .onSuccessTask(querySnapshot ->
                        Tasks.call(executor, () -> {
                            List<TaskOccurrence> remoteOccurrences = querySnapshot.toObjects(TaskOccurrence.class);
                            localDataSource.replaceAllForUser(userId, remoteOccurrences);
                            return localDataSource.getTaskOccurrencesByUserIdSortedByDate(userId);
                        })
                );
    }



    public void getOccurrencesByTaskId(String taskId, OnCompleteListener<List<TaskOccurrence>> listener) {
        remoteDataSource.getOccurrencesByTaskId(taskId, taskRemote -> {
            if (taskRemote.isSuccessful() && taskRemote.getResult() != null) {
                List<TaskOccurrence> remoteOccurrences = taskRemote.getResult().toObjects(TaskOccurrence.class);
                listener.onComplete(Tasks.forResult(remoteOccurrences));
            } else {
                // Fallback na lokalnu bazu ako remote ne uspe
                executor.execute(() -> {
                    List<TaskOccurrence> localOccurrences = localDataSource.getOccurrencesByTaskId(taskId);
                    listener.onComplete(Tasks.forResult(localOccurrences));
                });
            }
        });
    }

    public void findFutureOccurrences(String taskId, long fromDate, OnCompleteListener<List<TaskOccurrence>> listener) {
        remoteDataSource.findFutureOccurrences(taskId, fromDate, taskRemote -> {
            if (taskRemote.isSuccessful() && taskRemote.getResult() != null) {
                List<TaskOccurrence> allFutureOccurrences = taskRemote.getResult().toObjects(TaskOccurrence.class);

                // Dodatno filtriranje na klijentu
                List<TaskOccurrence> uncompletedFutureOccurrences = allFutureOccurrences.stream()
                        .filter(occ -> occ.getStatus() != TaskStatus.COMPLETED)
                        .collect(Collectors.toList());

                listener.onComplete(Tasks.forResult(uncompletedFutureOccurrences));
            } else {
                // Ako remote ne uspe, prosledi grešku
                listener.onComplete(Tasks.forException(taskRemote.getException()));
            }
        });
    }

    public void updateOccurrenceTaskId(String occurrenceId, String newTaskId, OnCompleteListener<Void> listener) {
        // Prvo ažuriraj remote bazu
        remoteDataSource.updateOccurrenceTaskId(occurrenceId, newTaskId, taskRemote -> {
            if (taskRemote.isSuccessful()) {
                // Ako je remote uspeo, ažuriraj i lokalnu
                executor.execute(() -> localDataSource.updateTaskOccurrenceTaskId(occurrenceId, newTaskId));
            }
            // Prosledi rezultat listeneru bez obzira na ishod lokalne operacije
            listener.onComplete(taskRemote);
        });
    }


    public void deleteOccurrenceAndFutureOnes(String taskId, OnCompleteListener<Void> listener) {
        executor.execute(() -> {
            long today = System.currentTimeMillis();

            // 1. Učitaj sve buduće occurrence za dati task (lokalno)
            List<TaskOccurrence> localFuture = localDataSource.findFutureOccurrences(taskId, today);

            // 2. Remote fetch future occurrences
            remoteDataSource.findFutureOccurrences(taskId, today, taskRemote -> {
                if (taskRemote.isSuccessful() && taskRemote.getResult() != null) {
                    List<TaskOccurrence> futureOccurrences = taskRemote.getResult().toObjects(TaskOccurrence.class);

                    // 3. Briši svuda (osim completed)
                    for (TaskOccurrence occ : futureOccurrences) {
                        if (occ.getStatus() != TaskStatus.COMPLETED) {
                            remoteDataSource.deleteOccurrence(occ.getId(), t -> {});
                        }
                    }

                    executor.execute(() -> {
                        for (TaskOccurrence occ : localFuture) {
                            if (occ.getStatus() != TaskStatus.COMPLETED) {
                                localDataSource.deleteTaskOccurrence(occ.getId());
                            }
                        }
                    });

                    listener.onComplete(Tasks.forResult(null));
                } else {
                    listener.onComplete(Tasks.forException(taskRemote.getException()));
                }
            });
        });
    }



    public void getTodaysCompletedOccurrencesForUser(String userId, OnCompleteListener<List<TaskOccurrence>> listener) {
        // 1. Pokušaj dohvatanja sa udaljenog izvora (Firestore)
        remoteDataSource.getTodaysCompletedOccurrencesForUser(userId, taskRemote -> {
            if (taskRemote.isSuccessful() && taskRemote.getResult() != null) {
                // 2. Ako je udaljeni izvor uspeo, konvertuj podatke i prosledi ih dalje
                List<TaskOccurrence> remoteOccurrences = taskRemote.getResult().toObjects(TaskOccurrence.class);
                listener.onComplete(Tasks.forResult(remoteOccurrences));
            } else {
                // 3. Ako udaljeni izvor nije uspeo, koristi lokalnu bazu kao fallback
                Log.w("TaskOccurrenceRepo", "Remote fetch failed for today's completed. Falling back to local.", taskRemote.getException());
                executor.execute(() -> {
                    List<TaskOccurrence> localOccurrences = localDataSource.getTodaysCompletedOccurrencesForUser(userId);
                    // Prosledi rezultat iz lokalne baze (može biti i prazna lista)
                    listener.onComplete(Tasks.forResult(localOccurrences));
                });
            }
        });
    }


    public void updateOccurrenceStatus(String occurrenceId, TaskStatus status, OnCompleteListener<Void> listener) {
        // Pripremi mapu za Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status.name());

        // Prvo ažuriraj remote bazu
        remoteDataSource.updateOccurrence(occurrenceId, updates, taskRemote -> {
            if (taskRemote.isSuccessful()) {
                // Ako je remote uspeo, ažuriraj i lokalnu bazu
                executor.execute(() -> localDataSource.updateOccurrenceStatus(occurrenceId, status));
            }
            // Prosledi rezultat listeneru
            listener.onComplete(taskRemote);
        });
    }


    public void getCompletedOccurrencesForDateRange(String userId, long fromDate, long toDate, OnCompleteListener<List<TaskOccurrence>> listener) {
        remoteDataSource.getCompletedOccurrencesForDateRange(userId, fromDate, toDate, task -> {
            if (task.isSuccessful()) {
                listener.onComplete(Tasks.forResult(task.getResult().toObjects(TaskOccurrence.class)));
            } else {
                // Fallback na lokalnu bazu
                executor.execute(() -> {
                    List<TaskOccurrence> localOccurrences = localDataSource.getCompletedOccurrencesForDateRange(userId, fromDate, toDate);
                    listener.onComplete(Tasks.forResult(localOccurrences));
                });
            }
        });
    }


    public void updateOldOccurrencesToUncompleted(String userId) {

        executor.execute(() -> {
            int localUpdatedCount = localDataSource.updateOldActiveOccurrencesToUncompleted();
            if (localUpdatedCount > 0) {
                Log.d("TaskOccurrenceRepo", "LOCAL: Updated " + localUpdatedCount + " old occurrences.");
            }
        });


        remoteDataSource.getOldActiveOccurrences(userId, getTask -> {
            if (getTask.isSuccessful() && getTask.getResult() != null) {
                if (getTask.getResult().isEmpty()) {
                    return; // Nema starih zadataka na remote-u, posao je gotov.
                }

                // Kreiraj batch operaciju za efikasno ažuriranje
                WriteBatch batch = FirebaseFirestore.getInstance().batch();
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", TaskStatus.UNCOMPLETED.name());

                for (QueryDocumentSnapshot document : getTask.getResult()) {
                    batch.update(document.getReference(), updates);
                }


                batch.commit().addOnCompleteListener(commitTask -> {
                    if (commitTask.isSuccessful()) {
                        Log.d("TaskOccurrenceRepo", "REMOTE: Successfully updated " + getTask.getResult().size() + " old occurrences.");
                    } else {
                        Log.e("TaskOccurrenceRepo", "REMOTE: Failed to update old occurrences.", commitTask.getException());
                    }
                });
            } else {
                Log.e("TaskOccurrenceRepo", "REMOTE: Failed to fetch old occurrences.", getTask.getException());
            }
        });
    }



    // =================================================================
    // NEW METHODS FOR XP QUOTA CHECKING
    // =================================================================

    public void getTodaysCompletedTaskCountByDifficulty(String userId, TaskDifficulty difficulty, OnCompleteListener<Integer> listener) {
        remoteDataSource.getTodaysCompletedTaskCountByDifficulty(userId, difficulty, remoteTask -> {
            if (remoteTask.isSuccessful() && remoteTask.getResult() != null) {
                listener.onComplete(Tasks.forResult(remoteTask.getResult().size()));
            } else {
                Log.w("TaskOccurrenceRepo", "Remote fetch failed. Falling back to local.", remoteTask.getException());
                executor.execute(() -> {
                    int localCount = localDataSource.getTodaysCompletedTaskCountByDifficulty(userId, difficulty);
                    listener.onComplete(Tasks.forResult(localCount));
                });
            }
        });
    }

    public void getTodaysCompletedTaskCountByPriority(String userId, TaskPriority priority, OnCompleteListener<Integer> listener) {
        remoteDataSource.getTodaysCompletedTaskCountByPriority(userId, priority, remoteTask -> {
            if (remoteTask.isSuccessful() && remoteTask.getResult() != null) {
                int count = remoteTask.getResult().size();
                Log.d("QuotaCheck", "Firestore SUCCESS. Count for " + priority.name() + " is: " + count);
                listener.onComplete(Tasks.forResult(count));
            } else {
                Log.e("QuotaCheck", "Firestore FAILED. Reason: " + remoteTask.getException());
                executor.execute(() -> {
                    int localCount = localDataSource.getTodaysCompletedTaskCountByPriority(userId, priority);
                    Log.d("QuotaCheck", "Fallback to LOCAL DB. Count for " + priority.name() + " is: " + localCount);
                    listener.onComplete(Tasks.forResult(localCount));
                });
            }
        });
    }

    public void getThisWeeksCompletedTaskCount(String userId, TaskDifficulty difficulty, OnCompleteListener<Integer> listener) {
        remoteDataSource.getThisWeeksCompletedTaskCount(userId, difficulty, remoteTask -> {
            if (remoteTask.isSuccessful() && remoteTask.getResult() != null) {
                int count = remoteTask.getResult().size();
                listener.onComplete(Tasks.forResult(count));
            } else {
                Log.w("TaskOccurrenceRepo", "Remote fetch failed for this week's count. Falling back to local.", remoteTask.getException());
                executor.execute(() -> {
                    int localCount = localDataSource.getThisWeeksCompletedTaskCount(userId, difficulty);
                    listener.onComplete(Tasks.forResult(localCount));
                });
            }
        });
    }

    public void getThisMonthsCompletedTaskCount(String userId, TaskPriority priority, OnCompleteListener<Integer> listener) {
        remoteDataSource.getThisMonthsCompletedTaskCount(userId, priority, remoteTask -> {
            if (remoteTask.isSuccessful() && remoteTask.getResult() != null) {
                int count = remoteTask.getResult().size();
                listener.onComplete(Tasks.forResult(count));
            } else {
                Log.w("TaskOccurrenceRepo", "Remote fetch failed for this month's count. Falling back to local.", remoteTask.getException());
                executor.execute(() -> {
                    int localCount = localDataSource.getThisMonthsCompletedTaskCount(userId, priority);
                    listener.onComplete(Tasks.forResult(localCount));
                });
            }
        });
    }


}