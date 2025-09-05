package com.team21.questify.presentation.activity;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import java.util.Calendar;

import com.google.firebase.auth.FirebaseAuth;
import com.team21.questify.R;
import com.team21.questify.application.model.Task;
import com.team21.questify.application.model.User;
import com.team21.questify.application.model.enums.RecurrenceUnit;
import com.team21.questify.application.model.enums.TaskDifficulty;
import com.team21.questify.application.model.enums.TaskPriority;
import com.team21.questify.application.model.enums.TaskType;
import com.team21.questify.application.service.TaskService;
import com.team21.questify.application.service.UserService;
import com.team21.questify.presentation.fragment.TaskCategoryFragment;
import com.team21.questify.utils.SharedPrefs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CreateTaskActivity extends AppCompatActivity implements TaskCategoryFragment.CategorySelectedListener{

    private EditText etTaskName;
    private EditText etTaskDescription;
    private Spinner spinnerDifficulty;
    private Spinner spinnerPriority;
    private RadioGroup radioGroupTaskType;

    // Novi članovi za ponavljajuće zadatke
    private LinearLayout recurringFieldsLayout;
    private EditText etRecurringInterval;
    private Spinner spinnerRecurrenceUnit;
    private DatePicker dpStartDate;
    private DatePicker dpEndDate;
    private View btnCreateTask;
    private String selectedCategoryId;


    private FirebaseAuth firebaseAuth;
    private TaskService taskService;
    private SharedPrefs sharedPreferences;
    private UserService userService;

    @Override
    public void onCategorySelected(String categoryId) {
        selectedCategoryId = categoryId;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_task);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();
        taskService = new TaskService(this);
        sharedPreferences = new SharedPrefs(this);
        userService = new UserService(this);

        initViews();
        setupEnums();
        setupListeners();

        // Proveravamo da li fragment već postoji pre nego što ga dodamo
        Fragment existingFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container_category);
        if (existingFragment == null) {
            TaskCategoryFragment categoryFragment = TaskCategoryFragment.newInstance();
            categoryFragment.setCategorySelectedListener(this); // Postavljanje listenera
            FragmentTransaction categoryTransaction = getSupportFragmentManager().beginTransaction();
            categoryTransaction.add(R.id.fragment_container_category, categoryFragment);
            categoryTransaction.commit();
        } else {
            // Ako fragment postoji, osiguravamo da je listener postavljen.
            ((TaskCategoryFragment) existingFragment).setCategorySelectedListener(this);
        }
    }

    private void initViews() {

        etTaskName = findViewById(R.id.editTextName);
        etTaskDescription = findViewById(R.id.editTextDescription);
        spinnerDifficulty = findViewById(R.id.spinnerDifficulty);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        radioGroupTaskType = findViewById(R.id.radioGroupTaskType);
        recurringFieldsLayout = findViewById(R.id.recurringFieldsLayout);
        etRecurringInterval = findViewById(R.id.et_recurring_interval);
        spinnerRecurrenceUnit = findViewById(R.id.spinnerRecurrenceUnit);
        dpStartDate = findViewById(R.id.dp_start_date);
        dpEndDate = findViewById(R.id.dp_end_date);
        btnCreateTask = findViewById(R.id.btn_create_task);

    }

    private void setupEnums() {
        // Popunjavanje Spinnera za težinu
        List<String> difficultyList = new ArrayList<>();
        for (TaskDifficulty difficulty : TaskDifficulty.values()) {
            difficultyList.add(difficulty.name());
        }
        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, difficultyList);
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(difficultyAdapter);

        // Popunjavanje Spinnera za prioritet
        List<String> priorityList = new ArrayList<>();
        for (TaskPriority priority : TaskPriority.values()) {
            priorityList.add(priority.name());
        }
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priorityList);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(priorityAdapter);

        // Popunjavanje Spinnera za jedinicu ponavljanja
        List<String> recurrenceUnitList = new ArrayList<>();
        for (RecurrenceUnit unit : RecurrenceUnit.values()) {
            recurrenceUnitList.add(unit.name());
        }
        ArrayAdapter<String> recurrenceUnitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, recurrenceUnitList);
        recurrenceUnitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecurrenceUnit.setAdapter(recurrenceUnitAdapter);

        // Dinamičko kreiranje RadioButton-a za tip zadatka
        for (TaskType type : TaskType.values()) {
            RadioButton radioButton = new RadioButton(this);
            // Lepše formatiranje teksta
            String formattedText = type.name().replace("_", " ");
            radioButton.setText(formattedText.substring(0, 1).toUpperCase() + formattedText.substring(1).toLowerCase());
            radioButton.setId(type.ordinal()); // Koristimo ordinal kao ID
            radioGroupTaskType.addView(radioButton);
        }

        // Postavljamo podrazumevani izbor na prvi RadioButton
        if (radioGroupTaskType.getChildCount() > 0) {
            ((RadioButton) radioGroupTaskType.getChildAt(0)).setChecked(true);
        }
    }

    private void setupListeners() {
        radioGroupTaskType.setOnCheckedChangeListener((group, checkedId) -> {
            // Pronalaženje izabranog RadioButton-a
            RadioButton checkedRadioButton = findViewById(checkedId);

            if (checkedRadioButton != null) {
                // Dobijanje tipa zadatka na osnovu ID-a
                TaskType selectedType = TaskType.values()[checkedId];

                if (selectedType == TaskType.RECURRING) {
                    recurringFieldsLayout.setVisibility(View.VISIBLE);
                } else {
                    recurringFieldsLayout.setVisibility(View.GONE);
                }
            }
        });

        // Inicijalno sakrivanje polja za ponavljanje ako prvi izbor nije RECURRING
        RadioButton initialCheckedButton = findViewById(radioGroupTaskType.getCheckedRadioButtonId());
        if (initialCheckedButton != null) {
            TaskType initialType = TaskType.values()[initialCheckedButton.getId()];
            if (initialType != TaskType.RECURRING) {
                recurringFieldsLayout.setVisibility(View.GONE);
            }
        }

        btnCreateTask.setOnClickListener(v -> createTask());

    }


    private void createTask() {
        String name = etTaskName.getText().toString().trim();
        String description = etTaskDescription.getText().toString().trim();

        if (name.isEmpty()) {
            etTaskName.setError("Task name is required");
            return;
        }

        TaskDifficulty difficulty = TaskDifficulty.valueOf(spinnerDifficulty.getSelectedItem().toString());
        TaskPriority priority = TaskPriority.valueOf(spinnerPriority.getSelectedItem().toString());

        int selectedTypeId = radioGroupTaskType.getCheckedRadioButtonId();
        TaskType taskType = TaskType.values()[selectedTypeId];


        int hour, minute;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hour =  ((TimePicker) findViewById(R.id.timePickerReminder)).getHour();
            minute = ((TimePicker) findViewById(R.id.timePickerReminder)).getMinute();
        } else {
            hour =  ((TimePicker) findViewById(R.id.timePickerReminder)).getCurrentHour();
            minute = ((TimePicker) findViewById(R.id.timePickerReminder)).getCurrentMinute();
        }

        long executionTimeMillis = TimeUnit.HOURS.toMillis(hour) + TimeUnit.MINUTES.toMillis(minute);


        // Pretpostavimo da je categoryId dobijen iz listenera
        if (selectedCategoryId == null) {
            // Ne dozvoljavamo kreiranje bez kategorije
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        userService.fetchUserProfile(sharedPreferences.getUserUid())
                .addOnSuccessListener(user -> {
                    // Kreiramo Task objekat
                    Task newTask = new Task();
                    newTask.setName(name);
                    newTask.setDescription(description);
                    newTask.setTaskCategoryId(selectedCategoryId);
                    newTask.setTaskType(taskType);
                    newTask.setTaskDifficulty(difficulty);
                    newTask.setTaskPriority(priority);
                    newTask.setExecutionTime(executionTimeMillis);
                    newTask.calculateAndSetXp(user.getLevel());
                    if (taskType == TaskType.RECURRING) {
                        String intervalStr = etRecurringInterval.getText().toString().trim();
                        if (intervalStr.isEmpty()) {
                            etRecurringInterval.setError("Interval required");
                            return;
                        }



                        int interval = Integer.parseInt(intervalStr);
                        try {
                            interval = Integer.parseInt(intervalStr);
                            if (interval <= 0) {
                                etRecurringInterval.setError("Interval must be greater than 0");
                                return;
                            }
                        } catch (NumberFormatException e) {
                            etRecurringInterval.setError("Invalid number");
                            return;
                        }


                        RecurrenceUnit unit = RecurrenceUnit.valueOf(spinnerRecurrenceUnit.getSelectedItem().toString());

                        Calendar startCal = Calendar.getInstance();
                        startCal.clear();
                        startCal.set(dpStartDate.getYear(), dpStartDate.getMonth(), dpStartDate.getDayOfMonth());


                        Calendar endCal = Calendar.getInstance();
                        endCal.clear();
                        endCal.set(dpEndDate.getYear(), dpEndDate.getMonth(), dpEndDate.getDayOfMonth());

                        if (endCal.before(startCal)) {
                            Toast.makeText(this, "End date cannot be before start date", Toast.LENGTH_SHORT).show();
                            return;
                        }


                        newTask.setRecurringInterval(interval);
                        newTask.setRecurrenceUnit(unit);
                        newTask.setRecurringStartDate(startCal.getTimeInMillis());
                        newTask.setRecurringEndDate(endCal.getTimeInMillis());
                    }
                    else {
                        // Ako nije RECURRING, ostavi sve vezano za ponavljanje na null ili podrazumevane vrednosti
                        newTask.setRecurringInterval(0);
                        newTask.setRecurrenceUnit(null);
                        newTask.setRecurringStartDate(null);
                        newTask.setRecurringEndDate(null);
                    }

                    taskService.createTask(newTask, task -> {
                        if (task != null && task.isSuccessful()) {
                            Toast.makeText(this, "Task created!", Toast.LENGTH_SHORT).show();
                            taskService.createOccurrences(newTask);

                            finish();
                        } else {
                            Toast.makeText(this, "Failed to create task", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    // Greška pri dohvatanju korisnika
                    Log.e("CreateTaskActivity", "Failed to get user for task creation.", e);
                    Toast.makeText(this, "Error: Could not get user data.", Toast.LENGTH_LONG).show();
                });





    }

}
