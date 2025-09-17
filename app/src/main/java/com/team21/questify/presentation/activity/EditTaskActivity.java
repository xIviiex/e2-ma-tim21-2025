package com.team21.questify.presentation.activity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.team21.questify.R;
import com.team21.questify.application.model.Task;
import com.team21.questify.application.model.enums.TaskDifficulty;
import com.team21.questify.application.model.enums.TaskPriority;
import com.team21.questify.application.service.TaskService;

import java.util.ArrayList;
import java.util.List;

public class EditTaskActivity extends AppCompatActivity {

    private EditText nameEditText;
    private EditText descriptionEditText;
    private TimePicker timePicker;
    private Spinner difficultySpinner;
    private Spinner prioritySpinner;
    private Button saveButton;
    private Task currentTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        String taskId = getIntent().getStringExtra("TASK_ID");
        if (taskId != null) {
            TaskService taskService = new TaskService(this);
            taskService.getTaskById(taskId, taskResult -> {
                if (taskResult.isSuccessful() && taskResult.getResult() != null) {
                    Task task = taskResult.getResult();
                    currentTask = task;
                    populateFields(task);
                }
            });
        }
    }

    private void populateFields(Task task) {
        nameEditText = findViewById(R.id.editTextTaskName);
        descriptionEditText = findViewById(R.id.editTextDescription);
        timePicker = findViewById(R.id.timePickerReminder);
        difficultySpinner = findViewById(R.id.spinnerDifficulty);
        prioritySpinner = findViewById(R.id.spinnerPriority);

        nameEditText.setText(task.getName());
        descriptionEditText.setText(task.getDescription());
        saveButton = findViewById(R.id.buttonSaveChanges);
        saveButton.setOnClickListener(v -> saveTask());
        long executionTimeMillis = task.getExecutionTime();
        int hour = (int) (executionTimeMillis / (1000 * 60 * 60));
        int minute = (int) ((executionTimeMillis / (1000 * 60)) % 60);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            timePicker.setHour(hour);
            timePicker.setMinute(minute);
        } else {
            timePicker.setCurrentHour(hour);
            timePicker.setCurrentMinute(minute);
        }

        // Popunjavanje Spinnera za težinu
        List<String> difficultyList = new ArrayList<>();
        for (TaskDifficulty difficulty : TaskDifficulty.values()) {
            difficultyList.add(difficulty.name());
        }
        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, difficultyList);
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        difficultySpinner.setAdapter(difficultyAdapter);
        difficultySpinner.setSelection(task.getTaskDifficulty().ordinal());

        List<String> priorityList = new ArrayList<>();
        for (TaskPriority priority : TaskPriority.values()) {
            priorityList.add(priority.name());
        }
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priorityList);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);
        prioritySpinner.setSelection(task.getTaskPriority().ordinal());
    }



    private void saveTask() {
        String name = nameEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();

        int hour, minute;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            hour = timePicker.getHour();
            minute = timePicker.getMinute();
        } else {
            hour = timePicker.getCurrentHour();
            minute = timePicker.getCurrentMinute();
        }

        // Pretvori sat i minut u millis (samo vreme u danu)
        long executionTime = (hour * 60L * 60L * 1000L) + (minute * 60L * 1000L);

        TaskDifficulty difficulty = TaskDifficulty.valueOf(
                difficultySpinner.getSelectedItem().toString()
        );

        TaskPriority priority = TaskPriority.valueOf(
                prioritySpinner.getSelectedItem().toString()
        );

        // Preuzmi originalni task (možeš ga čuvati u polju)
        Task updatedTask = currentTask;
        updatedTask.setName(name);
        updatedTask.setDescription(description);
        updatedTask.setExecutionTime(executionTime);
        updatedTask.setTaskDifficulty(difficulty);
        updatedTask.setTaskPriority(priority);

        TaskService taskService = new TaskService(this);
        taskService.updateTask(updatedTask, updateResult -> {
            if (updateResult.isSuccessful()) {
                Toast.makeText(this, "Task updated successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error updating task.", Toast.LENGTH_SHORT).show();
            }
        });
    }


}