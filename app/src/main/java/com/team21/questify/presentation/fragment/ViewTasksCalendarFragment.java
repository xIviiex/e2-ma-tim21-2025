package com.team21.questify.presentation.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.applandeo.materialcalendarview.EventDay;
import com.google.firebase.auth.FirebaseAuth;
import com.applandeo.materialcalendarview.CalendarView;

import com.team21.questify.R;
import com.team21.questify.application.model.Task;
import com.team21.questify.application.model.TaskCategory;
import com.team21.questify.application.model.TaskOccurrence;
import com.team21.questify.application.service.TaskCategoryService;
import com.team21.questify.application.service.TaskOccurrenceService;
import com.team21.questify.application.service.TaskService;
import com.team21.questify.presentation.adapter.TaskForDayAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ViewTasksCalendarFragment extends Fragment implements TaskForDayAdapter.OnTaskClickListener {

    private static final String TAG = "ViewTasksCalendarFrag";

    private CalendarView calendarView;

    private RecyclerView recyclerView;
    private TaskForDayAdapter adapter;
    private TextView selectedDateTextView;

    private TaskService taskService;
    private TaskOccurrenceService taskOccurrenceService;
    private TaskCategoryService taskCategoryService;
    private FirebaseAuth auth;

    // Keširanje podataka
    private Map<String, Task> tasksMap = new HashMap<>();
    private Map<Long, List<TaskOccurrence>> occurrencesByDay = new HashMap<>();
    private Map<String, Integer> categoryColorMap = new HashMap<>();

    public ViewTasksCalendarFragment() {
        // Required empty public constructor
    }

    public static ViewTasksCalendarFragment newInstance() {
        return new ViewTasksCalendarFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        taskService = new TaskService(getContext());
        taskOccurrenceService = new TaskOccurrenceService(getContext());
        taskCategoryService = new TaskCategoryService(getContext());
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_view_tasks_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        calendarView = view.findViewById(R.id.calendarView);
        recyclerView = view.findViewById(R.id.recyclerViewTasks);
        selectedDateTextView = view.findViewById(R.id.selectedDateTextView);

        adapter = new TaskForDayAdapter(new ArrayList<>(), tasksMap, categoryColorMap, this);


        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadTasks();

        calendarView.setOnDayClickListener(eventDay -> {
            Calendar clickedDay = eventDay.getCalendar();
            Date selectedDate = clickedDay.getTime();
            filterTasksByDate(selectedDate);
            updateSelectedDateText(eventDay.getCalendar().getTime());
        });


        // Prikaz zadataka za danasnji dan na pocetku i postavi tekst
        Date today = new Date();
        filterTasksByDate(today);
        updateSelectedDateText(today);
    }


    @Override
    public void onTaskClick(TaskOccurrence occurrence) {
        TaskDetailsDialogFragment dialogFragment = TaskDetailsDialogFragment.newInstance(occurrence);
        dialogFragment.show(getParentFragmentManager(), "TaskDetailsDialog");
    }

    private void updateSelectedDateText(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.ENGLISH);
        String formattedDate = sdf.format(date);
        selectedDateTextView.setText(formattedDate);
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
                loadAllOccurrences();
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
                        color = Color.parseColor(category.getHexColor()); // assuming hex like "#FF0000"
                    } catch (Exception e) {
                        color = Color.GRAY;
                    }
                    categoryColorMap.put(categoryId, color);
                } else {
                    categoryColorMap.put(categoryId, Color.GRAY);
                }
                // Osveži dekoratore kalendara svaki put kad dobijemo boju
                updateCalendarDecorators();
            });
        }
    }

    private void loadAllOccurrences() {
        taskOccurrenceService.getAllOccurrencesForCurrentUser(occurrenceResult -> {
            if (occurrenceResult.isSuccessful() && occurrenceResult.getResult() != null) {

                occurrencesByDay.clear();

                List<TaskOccurrence> occurrences = occurrenceResult.getResult();

                for (TaskOccurrence occurrence : occurrences) {
                    if (occurrence != null) {
                        long dayStart = getStartOfDay(occurrence.getDate());
                        if (!occurrencesByDay.containsKey(dayStart)) {
                            occurrencesByDay.put(dayStart, new ArrayList<>());
                        }
                        occurrencesByDay.get(dayStart).add(occurrence);
                    }
                }

                updateCalendarDecorators();
                filterTasksByDate(new Date()); // prikaz za danas
            } else {
                Log.e(TAG, "Error getting task occurrences: ", occurrenceResult.getException());
            }
        });
    }



    private void updateCalendarDecorators() {
        List<EventDay> events = new ArrayList<>();

        for (Map.Entry<Long, List<TaskOccurrence>> entry : occurrencesByDay.entrySet()) {
            Date date = new Date(entry.getKey());
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            TaskOccurrence firstOccurrence = entry.getValue().get(0);
            Task task = tasksMap.get(firstOccurrence.getTaskId());

            int drawableRes = R.drawable.circle_shape; // ili neki drugi tvoj resurs

            if (task != null && task.getTaskCategoryId() != null) {
                Integer color = categoryColorMap.get(task.getTaskCategoryId());
                // Napravi custom EventDay ako koristiš boje, ili koristi drawable ako samo hoćeš da se nešto pojavi
                events.add(new EventDay(cal, drawableRes));
            }
        }

        calendarView.setEvents(events);
    }



    private void filterTasksByDate(Date selectedDate) {
        long selectedDateInMillis = getStartOfDay(selectedDate.getTime());
        List<TaskOccurrence> occurrencesForDay = occurrencesByDay.getOrDefault(selectedDateInMillis, new ArrayList<>());
        adapter.setOccurrences(occurrencesForDay);
    }

    private long getStartOfDay(long timeInMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeInMillis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
