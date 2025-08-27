package com.team21.questify.presentation.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.team21.questify.R;
import com.team21.questify.application.model.Task;
import com.team21.questify.application.model.TaskCategory;
import com.team21.questify.application.model.TaskOccurrence;
import com.team21.questify.application.model.enums.TaskType;
import com.team21.questify.application.service.TaskCategoryService;
import com.team21.questify.application.service.TaskOccurrenceService;
import com.team21.questify.application.service.TaskService;
import com.team21.questify.presentation.adapter.TaskForDayAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ViewTasksListFragment extends Fragment {

    private static final String TAG = "ViewTasksListFragment";
    private RecyclerView recyclerView;
    private TaskForDayAdapter adapter;
    private Spinner filterSpinner;
    private TaskService taskService;
    private TaskOccurrenceService taskOccurrenceService;
    private TaskCategoryService taskCategoryService;
    private FirebaseAuth auth;

    private List<TaskOccurrence> allOccurrences = new ArrayList<>();
    private Map<String, Task> tasksMap = new HashMap<>();
    private Map<String, Integer> categoryColorMap = new HashMap<>();

    public ViewTasksListFragment() {}
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        taskService = new TaskService(getContext());
        taskOccurrenceService = new TaskOccurrenceService(getContext());
        taskCategoryService = new TaskCategoryService(getContext());
        auth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_tasks_list, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TaskForDayAdapter(allOccurrences, tasksMap, categoryColorMap, occurrence -> {
            TaskDetailsDialogFragment dialogFragment = TaskDetailsDialogFragment.newInstance(occurrence);
            dialogFragment.show(getParentFragmentManager(), "TaskDetailsDialog");
        });
        recyclerView.setAdapter(adapter);

        filterSpinner = view.findViewById(R.id.filterSpinner);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"All", "One-time", "Recurring"}
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(spinnerAdapter);



        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
                filterOccurrences(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        loadTasks(); // učitaj sve taskove i occurrence-e
        return view;
    }


    private void loadTasks() {
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User not logged in.");
            return;
        }

        taskService.getAllTasksForCurrentUser(taskResult -> {
            if (taskResult.isSuccessful() && taskResult.getResult() != null) {
                Set<String> categoryIds = new HashSet<>();
                List<Task> tasks = taskResult.getResult();

                for (Task task : tasks) {
                    if (task != null) {
                        tasksMap.put(task.getId(), task);
                        if (task.getTaskCategoryId() != null) {
                            categoryIds.add(task.getTaskCategoryId());
                        }
                    }
                }

                loadCategoryColors(categoryIds);
                loadAllOccurrences();  // posle učitavanja taskova -> occurrence-i
            } else {
                Log.e(TAG, "Error getting user tasks: ", taskResult.getException());
            }
        });
    }



    private void loadCategoryColors(Set<String> categoryIds) {
        categoryColorMap.clear();

        for (String categoryId : categoryIds) {
            taskCategoryService.getCategoryById(categoryId, task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    TaskCategory category = task.getResult();
                    int color;
                    try {
                        color = Color.parseColor(category.getHexColor());
                    } catch (Exception e) {
                        color = Color.GRAY;
                    }
                    categoryColorMap.put(categoryId, color);
                } else {
                    categoryColorMap.put(categoryId, Color.GRAY);
                }

            });
        }
    }

    private void loadAllOccurrences() {
        taskOccurrenceService.getAllOccurrencesForCurrentUser(occurrenceResult -> {
            if (occurrenceResult.isSuccessful() && occurrenceResult.getResult() != null) {
                allOccurrences.clear();

                List<TaskOccurrence> occurrences = occurrenceResult.getResult();
                for (TaskOccurrence occurrence : occurrences) {
                    if (occurrence != null) {
                        // filtriraj samo današnje i buduće
                        if (occurrence.getDate() >= getStartOfDay(System.currentTimeMillis())) {
                            allOccurrences.add(occurrence);
                        }
                    }
                }

                adapter.setOccurrences(allOccurrences);

            } else {
                Log.e(TAG, "Error getting task occurrences: ", occurrenceResult.getException());
            }
        });
    }



    private long getStartOfDay(long timeInMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeInMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }




    private void filterOccurrences(int position) {
        if (allOccurrences == null) return;

        List<TaskOccurrence> filtered = new ArrayList<>();
        switch (position) {
            case 1: // One-time
                for (TaskOccurrence occ : allOccurrences) {
                    Task t = tasksMap.get(occ.getTaskId());
                    if (t != null && t.getTaskType() == TaskType.ONE_TIME) {
                        filtered.add(occ);
                    }
                }
                break;
            case 2: // Recurring
                for (TaskOccurrence occ : allOccurrences) {
                    Task t = tasksMap.get(occ.getTaskId());
                    if (t != null && t.getTaskType() == TaskType.RECURRING) {
                        filtered.add(occ);
                    }
                }
                break;
            default: // All
                filtered = new ArrayList<>(allOccurrences);
        }
        adapter.setOccurrences(filtered);
    }
}
