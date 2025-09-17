package com.team21.questify.presentation.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.team21.questify.R;
import com.team21.questify.application.model.Task;
import com.team21.questify.application.model.TaskCategory;
import com.team21.questify.application.model.TaskOccurrence;
import com.team21.questify.application.model.User;
import com.team21.questify.application.model.enums.TaskDifficulty;
import com.team21.questify.application.model.enums.TaskPriority;
import com.team21.questify.application.model.enums.TaskStatus;
import com.team21.questify.application.model.enums.TaskType;
import com.team21.questify.application.service.TaskCategoryService;
import com.team21.questify.application.service.TaskOccurrenceService;
import com.team21.questify.application.service.TaskService;
import com.team21.questify.application.service.UserService;
import com.team21.questify.presentation.activity.EditTaskActivity;
import com.team21.questify.utils.LevelCalculator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskDetailsDialogFragment extends DialogFragment {

    private static final String ARG_TASK_OCCURRENCE = "task_occurrence";

    private TaskOccurrence taskOccurrence;
    private Task task;
    private TaskService taskService;
    private TaskCategoryService taskCategoryService;
    private TaskOccurrenceService taskOccurrenceService;
    private UserService userService;
    private FirebaseAuth auth;
    private User currentUser;

    private OnTaskUpdatedListener listener;


    public interface OnTaskUpdatedListener {
        void onTaskUpdated();
    }

    public void setOnTaskUpdatedListener(OnTaskUpdatedListener listener) {
        this.listener = listener;
    }

    public static TaskDetailsDialogFragment newInstance(TaskOccurrence occurrence) {
        TaskDetailsDialogFragment fragment = new TaskDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TASK_OCCURRENCE, occurrence);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            getDialog().getWindow().setLayout(width, height);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            taskOccurrence = (TaskOccurrence) getArguments().getSerializable(ARG_TASK_OCCURRENCE);
        }

        taskService = new TaskService(getContext());
        taskCategoryService = new TaskCategoryService(getContext());
        taskOccurrenceService = new TaskOccurrenceService(getContext());
        userService = new UserService(getContext());
        auth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_details_dialog, container, false);

        TextView statusTextView = view.findViewById(R.id.textViewStatus);
        Button closeButton = view.findViewById(R.id.buttonClose);
        ImageView editButton = view.findViewById(R.id.edit_button);
        ImageView deleteButton = view.findViewById(R.id.delete_button);

        closeButton.setOnClickListener(v -> dismiss());
        editButton.setOnClickListener(v -> openEditActivity());
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());
        statusTextView.setOnClickListener(v -> showStatusChangeDialog());


        loadDataSequentially(view);

        return view;
    }


    private void loadDataSequentially(View view) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser != null) {

            userService.fetchUserProfile(firebaseUser.getUid()).addOnCompleteListener(userTask -> {
                if (userTask.isSuccessful()) {

                    this.currentUser = userTask.getResult();


                    loadAndDisplayTaskData(view);

                } else {
                    Toast.makeText(getContext(), "Failed to load user data.", Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            });
        } else {
            Toast.makeText(getContext(), "User not authenticated.", Toast.LENGTH_SHORT).show();
            dismiss();
        }
    }

    private void loadAndDisplayTaskData(View view) {
        String taskId = taskOccurrence.getTaskId();
        taskService.getTaskById(taskId, taskResult -> {
            if (taskResult.isSuccessful() && taskResult.getResult() != null) {
                this.task = taskResult.getResult();


                ((TextView) view.findViewById(R.id.textViewTaskName)).setText(task.getName());
                ((TextView) view.findViewById(R.id.textViewType)).setText(task.getTaskType().name());
                ((TextView) view.findViewById(R.id.textViewDescription)).setText(task.getDescription());
                ((TextView) view.findViewById(R.id.textViewDifficulty)).setText(task.getTaskDifficulty().name());
                ((TextView) view.findViewById(R.id.textViewPriority)).setText(task.getTaskPriority().name());
                ((TextView) view.findViewById(R.id.textViewXP)).setText(String.format(Locale.getDefault(), "%d", task.getXp()));


                long timestamp = taskOccurrence.getDate();
                Date date = new Date(timestamp);
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                ((TextView) view.findViewById(R.id.textViewDate)).setText(sdf.format(date));


                if (task.getTaskCategoryId() != null) {
                    taskCategoryService.getCategoryById(task.getTaskCategoryId(), categoryResult -> {
                        if (categoryResult.isSuccessful() && categoryResult.getResult() != null) {
                            ((TextView) view.findViewById(R.id.textViewCategory)).setText(categoryResult.getResult().getName());
                        } else {
                            ((TextView) view.findViewById(R.id.textViewCategory)).setText("N/A");
                        }
                    });
                } else {
                    ((TextView) view.findViewById(R.id.textViewCategory)).setText("N/A");
                }


                updateStatusUI();

            } else {
                ((TextView) view.findViewById(R.id.textViewTaskName)).setText("Task not found.");
            }
        });
    }


    private void updateStatusUI() {
        if (getView() == null) return;

        TextView statusTextView = getView().findViewById(R.id.textViewStatus);
        ImageView editButton = getView().findViewById(R.id.edit_button);
        ImageView deleteButton = getView().findViewById(R.id.delete_button);

        if (taskOccurrence == null) return;

        TaskStatus status = taskOccurrence.getStatus();
        statusTextView.setText(status.name());


        switch (status) {
            case COMPLETED:
                statusTextView.setTextColor(Color.parseColor("#4CAF50"));
                break;
            case CANCELED:
            case UNCOMPLETED:
                statusTextView.setTextColor(Color.GRAY);
                break;
            case PAUSED:
                statusTextView.setTextColor(Color.parseColor("#03A9F4"));
                break;
            case ACTIVE:
            default:
                statusTextView.setTextColor(Color.parseColor("#F44336"));
                break;
        }


        if (status == TaskStatus.UNCOMPLETED || status == TaskStatus.CANCELED) {
            editButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
        } else {
            editButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
        }
    }


    private void showStatusChangeDialog() {
        if (task == null || taskOccurrence == null) return;

        final TaskStatus currentStatus = taskOccurrence.getStatus();


        Calendar limitCal = Calendar.getInstance();
        limitCal.add(Calendar.DAY_OF_YEAR, -3);
        limitCal.set(Calendar.HOUR_OF_DAY, 0);
        if (currentStatus == TaskStatus.ACTIVE && taskOccurrence.getDate() < limitCal.getTimeInMillis()) {
            Toast.makeText(getContext(), "This task is older than 3 days and cannot be changed.", Toast.LENGTH_LONG).show();
            TaskStatus newStatus = TaskStatus.UNCOMPLETED;
            updateTaskStatus(newStatus);
            return;
        }


        List<String> options = new ArrayList<>();
        if (currentStatus == TaskStatus.ACTIVE) {
            options.add(TaskStatus.COMPLETED.name());
            options.add(TaskStatus.CANCELED.name());

            if (task.getTaskType() == TaskType.RECURRING) {
                options.add(TaskStatus.PAUSED.name());
            }
        } else if (currentStatus == TaskStatus.PAUSED) {

            options.add(TaskStatus.ACTIVE.name());
        } else {

            Toast.makeText(getContext(), "This task's status cannot be changed.", Toast.LENGTH_SHORT).show();
            return;
        }


        new AlertDialog.Builder(getContext())
                .setTitle("Change Task Status")
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    String selectedStatusStr = options.get(which);
                    TaskStatus newStatus = TaskStatus.valueOf(selectedStatusStr);
                    updateTaskStatus(newStatus);
                })
                .show();
    }



    private void updateTaskStatus(TaskStatus newStatus) {
        final TaskStatus oldStatus = taskOccurrence.getStatus();

        taskOccurrenceService.updateOccurrenceStatus(taskOccurrence.getId(), newStatus, updateTask -> {
            if (updateTask.isSuccessful()) {
                Toast.makeText(getContext(), "Status updated to " + newStatus.name(), Toast.LENGTH_SHORT).show();
                taskOccurrence.setStatus(newStatus);
                updateStatusUI();

                boolean isBulkOperationPending = false;

                if (newStatus == TaskStatus.COMPLETED) {
                    handleXpAwarding();
                }

                if (newStatus == TaskStatus.PAUSED) {
                    isBulkOperationPending = true;
                    pauseAllFutureOccurrences();
                }

                if (oldStatus == TaskStatus.PAUSED && newStatus == TaskStatus.ACTIVE) {
                    isBulkOperationPending = true;
                    reactivateAllFutureOccurrences();
                }


                if (!isBulkOperationPending && listener != null) {
                    listener.onTaskUpdated();
                }

            } else {
                Toast.makeText(getContext(), "Error updating status: " + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void pauseAllFutureOccurrences() {
        if (taskOccurrence == null) return;
        long fromDate = taskOccurrence.getDate() + 1;

        taskOccurrenceService.findFutureOccurrences(taskOccurrence.getTaskId(), fromDate, futureTask -> {
            if (futureTask.isSuccessful() && futureTask.getResult() != null) {
                List<TaskOccurrence> futureOccurrences = futureTask.getResult();


                if (futureOccurrences.isEmpty()) {
                    if (listener != null) listener.onTaskUpdated();
                    return;
                }

                // Inicijalizuj brojač
                final int totalTasksToPause = futureOccurrences.size();
                final AtomicInteger pausedCounter = new AtomicInteger(0);

                for (TaskOccurrence futureOcc : futureOccurrences) {
                    taskOccurrenceService.updateOccurrenceStatus(futureOcc.getId(), TaskStatus.PAUSED, update -> {

                        int completedCount = pausedCounter.incrementAndGet();


                        if (completedCount == totalTasksToPause) {
                            Toast.makeText(getContext(), "All future occurrences have been paused.", Toast.LENGTH_SHORT).show();

                            if (listener != null) {
                                listener.onTaskUpdated();
                            }
                        }
                    });
                }
            } else {
                Log.e("TaskDetailsDialog", "Could not fetch future occurrences.", futureTask.getException());
            }
        });
    }


    private void reactivateAllFutureOccurrences() {
        if (taskOccurrence == null) return;
        long fromDate = taskOccurrence.getDate() + 1;

        taskOccurrenceService.findFutureOccurrences(taskOccurrence.getTaskId(), fromDate, futureTask -> {
            if (futureTask.isSuccessful() && futureTask.getResult() != null) {


                List<TaskOccurrence> occurrencesToReactivate = new ArrayList<>();
                for(TaskOccurrence occ : futureTask.getResult()){
                    if(occ.getStatus() == TaskStatus.PAUSED){
                        occurrencesToReactivate.add(occ);
                    }
                }

                if (occurrencesToReactivate.isEmpty()) {
                    if (listener != null) listener.onTaskUpdated();
                    return;
                }

                final int totalTasksToReactivate = occurrencesToReactivate.size();
                final AtomicInteger reactivatedCounter = new AtomicInteger(0);

                for (TaskOccurrence futureOcc : occurrencesToReactivate) {
                    taskOccurrenceService.updateOccurrenceStatus(futureOcc.getId(), TaskStatus.ACTIVE, update -> {
                        int completedCount = reactivatedCounter.incrementAndGet();
                        if (completedCount == totalTasksToReactivate) {
                            Toast.makeText(getContext(), "All future paused occurrences have been reactivated.", Toast.LENGTH_SHORT).show();
                            if (listener != null) {
                                listener.onTaskUpdated();
                            }
                        }
                    });
                }
            } else {
                Log.e("TaskDetailsDialog", "Could not fetch future occurrences.", futureTask.getException());
            }
        });
    }


    private void handleXpAwarding() {
        if (task == null || auth.getCurrentUser() == null || currentUser == null) {
            Log.e("handleXpAwarding", "Data ain't ready.");
            return;
        }


        checkPriorityQuota(task.getTaskPriority(), priorityXp -> {

            checkDifficultyQuota(task.getTaskDifficulty(), difficultyXp -> {
                int totalXpToAward = priorityXp + difficultyXp;

                if (totalXpToAward > 0) {
                    if (isAdded()) {
                        awardXp(auth.getCurrentUser().getUid(), totalXpToAward);
                    }
                } else {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "All xp for this task is filled.", Toast.LENGTH_LONG).show();
                    }
                }
            });
        });
    }

    private void checkPriorityQuota(TaskPriority priority, XpCallback callback) {
        if (priority == TaskPriority.SPECIAL) {
            taskOccurrenceService.getThisMonthsCompletedTaskCount(priority, monthlyCountTask -> {

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (monthlyCountTask.isSuccessful() && monthlyCountTask.getResult() != null && monthlyCountTask.getResult() < 1) {
                        callback.onXpCalculated(LevelCalculator.getXpForDifficultyOrPriority(currentUser.getLevel(), task.getTaskPriority().getXp()));
                    } else {
                        if (!monthlyCountTask.isSuccessful()) Log.e("checkPriorityQuota", "Error", monthlyCountTask.getException());
                        Toast.makeText(getContext(), "Month SPECIAL task filled", Toast.LENGTH_SHORT).show();
                        callback.onXpCalculated(0);
                    }
                });
            });
            return;
        }

        taskOccurrenceService.getTodaysCompletedTaskCountByPriority(priority, dailyCountTask -> {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (dailyCountTask.isSuccessful() && dailyCountTask.getResult() != null) {
                    int todaysCount = dailyCountTask.getResult();
                    boolean quotaHit = false;
                    switch (priority) {
                        case NORMAL:
                        case IMPORTANT:
                            if (todaysCount >= 5) quotaHit = true;
                            break;
                        case EXTREMELY_IMPORTANT:
                            if (todaysCount >= 2) quotaHit = true;
                            break;
                    }
                    if (quotaHit) {
                        Toast.makeText(getContext(), "Daily xp for " + priority.name() + " priority filled.", Toast.LENGTH_SHORT).show();
                        callback.onXpCalculated(0);
                    } else {
                        callback.onXpCalculated(LevelCalculator.getXpForDifficultyOrPriority(currentUser.getLevel(), task.getTaskPriority().getXp()));
                    }
                } else {
                    Log.e("checkPriorityQuota", "Error", dailyCountTask.getException());
                    Toast.makeText(getContext(), "Can't check data for priority", Toast.LENGTH_SHORT).show();
                    callback.onXpCalculated(0);
                }
            });
        });
    }

    private void checkDifficultyQuota(TaskDifficulty difficulty, XpCallback callback) {
        if (difficulty == TaskDifficulty.EXTREMELY_HARD) {
            taskOccurrenceService.getThisWeeksCompletedTaskCount(difficulty, weeklyCountTask -> {

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (weeklyCountTask.isSuccessful() && weeklyCountTask.getResult() != null && weeklyCountTask.getResult() < 1) {
                        callback.onXpCalculated(LevelCalculator.getXpForDifficultyOrPriority(currentUser.getLevel(), task.getTaskDifficulty().getXp()));
                    } else {
                        if (!weeklyCountTask.isSuccessful()) Log.e("checkDifficultyQuota", "Error", weeklyCountTask.getException());
                        Toast.makeText(getContext(), "Weakly xp for EXTREMELY HARD filled", Toast.LENGTH_SHORT).show();
                        callback.onXpCalculated(0);
                    }
                });
            });
            return;
        }

        taskOccurrenceService.getTodaysCompletedTaskCountByDifficulty(difficulty, dailyCountTask -> {

            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (dailyCountTask.isSuccessful() && dailyCountTask.getResult() != null) {
                    int todaysCount = dailyCountTask.getResult();
                    boolean quotaHit = false;
                    switch (difficulty) {
                        case VERY_EASY:
                        case EASY:
                            if (todaysCount >= 5) quotaHit = true;
                            break;
                        case HARD:
                            if (todaysCount >= 2) quotaHit = true;
                            break;
                    }
                    if (quotaHit) {
                        Toast.makeText(getContext(), "Daily xp for " + difficulty.name() + " difficulty filled.", Toast.LENGTH_SHORT).show();
                        callback.onXpCalculated(0);
                    } else {
                        callback.onXpCalculated(LevelCalculator.getXpForDifficultyOrPriority(currentUser.getLevel(), task.getTaskDifficulty().getXp()));
                    }
                } else {
                    Log.e("checkDifficultyQuota", "Error", dailyCountTask.getException());
                    Toast.makeText(getContext(), "Can't check data for difficulty", Toast.LENGTH_SHORT).show();
                    callback.onXpCalculated(0);
                }
            });
        });
    }


    private void awardXp(String userId, int xpToAdd) {
        userService.addXpAndCheckLevelUp(userId, xpToAdd).addOnCompleteListener(levelUpTask -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (levelUpTask.isSuccessful()) {
                        Toast.makeText(getContext(), "You got " + xpToAdd + " XP!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Error adding XP: " + levelUpTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }


    interface XpCallback {
        void onXpCalculated(int xp);
    }



    private void openEditActivity() {
        if (taskOccurrence != null) {
            Intent intent = new Intent(getContext(), EditTaskActivity.class);
            intent.putExtra("TASK_ID", taskOccurrence.getTaskId());
            startActivity(intent);
            dismiss();
        }
    }

    private void showDeleteConfirmationDialog() {
        if (taskOccurrence != null) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Delete Task Occurrences")
                    .setMessage("Are you sure you want to delete all future occurrences of this task?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        taskOccurrenceService.deleteOccurrenceAndFutureOnes(
                                taskOccurrence.getTaskId(),
                                task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(getContext(), "Future occurrences deleted", Toast.LENGTH_SHORT).show();
                                        if (listener != null) listener.onTaskUpdated();
                                        dismiss();
                                    } else {
                                        Toast.makeText(getContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }
}