package com.team21.questify.presentation.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.team21.questify.R;
import com.team21.questify.application.model.Task;
import com.team21.questify.application.model.TaskOccurrence;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TaskViewHolder extends RecyclerView.ViewHolder {
    private final TextView taskTitleTextView;
    private final TextView taskStatusTextView;
    private final TextView taskTimeTextView;
    private final View colorIndicator;

    private final Map<String, Task> tasksMap;
    private final Map<String, Integer> categoryColorMap;
    private final TaskForDayAdapter.OnTaskClickListener listener;

    public TaskViewHolder(@NonNull View itemView,
                          Map<String, Task> tasksMap,
                          Map<String, Integer> categoryColorMap,
                          TaskForDayAdapter.OnTaskClickListener listener) {
        super(itemView);
        this.tasksMap = tasksMap;
        this.categoryColorMap = categoryColorMap;
        this.listener = listener;

        taskTitleTextView = itemView.findViewById(R.id.taskTitle);
        taskStatusTextView = itemView.findViewById(R.id.taskStatus);
        taskTimeTextView = itemView.findViewById(R.id.taskTime);
        colorIndicator = itemView.findViewById(R.id.colorIndicator);
    }

    public void bind(TaskOccurrence occurrence) {
        Task task = tasksMap.get(occurrence.getTaskId());

        if (task != null) {
            taskTitleTextView.setText(task.getName());
            taskStatusTextView.setText(occurrence.getStatus().name());

            // Set boja kategorije
            int color = categoryColorMap.getOrDefault(task.getTaskCategoryId(), Color.GRAY);
            colorIndicator.setBackgroundTintList(ColorStateList.valueOf(color));

            long executionTimeMillis = task.getExecutionTime();
            long hours = TimeUnit.MILLISECONDS.toHours(executionTimeMillis);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(executionTimeMillis) % 60;
            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hours, minutes);

            taskTimeTextView.setText(formattedTime);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(occurrence); // Prosleđujemo occurrence
                }
            });
        } else {
            taskTitleTextView.setText("Task not found");
            taskStatusTextView.setText("");
            taskTimeTextView.setText("");
            colorIndicator.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
        }
    }
}
