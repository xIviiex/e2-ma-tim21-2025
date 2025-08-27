package com.team21.questify.presentation.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.team21.questify.R;
import com.team21.questify.application.model.Task;
import com.team21.questify.application.model.TaskCategory;
import com.team21.questify.application.model.TaskOccurrence;
import com.team21.questify.application.model.enums.TaskStatus;
import com.team21.questify.application.service.TaskCategoryService;
import com.team21.questify.application.service.TaskService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TaskDetailsDialogFragment extends DialogFragment {

    private static final String ARG_TASK_OCCURRENCE = "task_occurrence";

    private TaskOccurrence taskOccurrence;
    private TaskService taskService;
    private TaskCategoryService taskCategoryService;
    private FirebaseAuth auth;

    public TaskDetailsDialogFragment() {
        // Required empty public constructor
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
        if (getDialog() != null) {
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
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_details_dialog, container, false);

        TextView taskNameTextView = view.findViewById(R.id.textViewTaskName);
        TextView descriptionTextView = view.findViewById(R.id.textViewDescription);
        TextView textViewDate = view.findViewById(R.id.textViewDate);
        TextView textViewType = view.findViewById(R.id.textViewType);
        TextView categoryTextView = view.findViewById(R.id.textViewCategory);
        TextView difficultyTextView = view.findViewById(R.id.textViewDifficulty);
        TextView priorityTextView = view.findViewById(R.id.textViewPriority);
        TextView xpTextView = view.findViewById(R.id.textViewXP);
        TextView statusTextView = view.findViewById(R.id.textViewStatus); // Added the new TextView
        Button closeButton = view.findViewById(R.id.buttonClose);

        closeButton.setOnClickListener(v -> dismiss());

        if (taskOccurrence != null) {
            // Set the status text and color based on the TaskOccurrence object
            statusTextView.setText(taskOccurrence.getStatus().name());
            if (taskOccurrence.getStatus() == TaskStatus.COMPLETED) {
                statusTextView.setTextColor(Color.GREEN);
            } else {
                statusTextView.setTextColor(Color.RED);
            }

            String taskId = taskOccurrence.getTaskId();
            taskService.getTaskById(taskId, taskResult -> {
                if (taskResult.isSuccessful() && taskResult.getResult() != null) {
                    Task task = taskResult.getResult();

                    taskNameTextView.setText(task.getName());
                    textViewType.setText(task.getTaskType().name());

                    //DATE
                    long timestamp = taskOccurrence.getDate(); // u millis
                    Date date = new Date(timestamp);

                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                    String formattedDate = sdf.format(date);

                    textViewDate.setText(formattedDate);

                    descriptionTextView.setText(task.getDescription());
                    difficultyTextView.setText(task.getTaskDifficulty().name());
                    priorityTextView.setText(task.getTaskPriority().name());
                    xpTextView.setText(String.format(Locale.getDefault(), "%d", task.getXp()));

                    String categoryId = task.getTaskCategoryId();
                    if (categoryId != null) {
                        taskCategoryService.getCategoryById(categoryId, categoryResult -> {
                            if (categoryResult.isSuccessful() && categoryResult.getResult() != null) {
                                TaskCategory category = categoryResult.getResult();
                                categoryTextView.setText(category.getName());
                            } else {
                                categoryTextView.setText("N/A");
                            }
                        });
                    } else {
                        categoryTextView.setText("N/A");
                    }
                } else {
                    taskNameTextView.setText("Task not found.");
                }
            });
        }
        return view;
    }
}