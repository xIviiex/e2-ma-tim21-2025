package com.team21.questify.application.service;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.team21.questify.application.model.TaskOccurrence;
import com.team21.questify.application.model.User;
import com.team21.questify.application.model.enums.TaskDifficulty;
import com.team21.questify.application.model.enums.TaskStatus;
import com.team21.questify.data.repository.TaskOccurrenceRepository;
import com.team21.questify.data.repository.TaskRepository;
import com.team21.questify.data.repository.UserRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class UserStatisticsService {
    private final TaskOccurrenceRepository taskOccurrenceRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public UserStatisticsService(Context context) {
        this.taskOccurrenceRepository = new TaskOccurrenceRepository(context);
        this.taskRepository = new TaskRepository(context);
        this.userRepository = new UserRepository(context);
    }

    public Map<TaskStatus, Integer> getTaskCountsByStatus(String userId) {
        return taskOccurrenceRepository.getTaskCountsByStatus(userId);
    }

    public Map<String, Integer> getCompletedTaskCountsByCategory(String userId) {
        return taskRepository.getCompletedTaskCountsByCategory(userId);
    }

    public int getLongestStreakOfCompletedTasks(String userId) {
        List<TaskOccurrence> occurrences = taskOccurrenceRepository.getTaskOccurrencesByUserIdSortedByDate(userId);
        if (occurrences.isEmpty()) {
            return 0;
        }

        int longestStreak = 0;
        int currentStreak = 0;

        Map<String, Boolean> dailyStatus = getDailyStatus(occurrences);

        String firstDateString = convertTimestampToDateString(occurrences.get(0).getDate());
        String todayDateString = convertTimestampToDateString(System.currentTimeMillis());
        Calendar calendar = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            calendar.setTime(Objects.requireNonNull(sdf.parse(firstDateString)));
        } catch (ParseException e) {
            return 0;
        }

        while (!convertTimestampToDateString(calendar.getTimeInMillis()).equals(todayDateString)) {
            String currentDateKey = convertTimestampToDateString(calendar.getTimeInMillis());

            if (dailyStatus.containsKey(currentDateKey)) {
                if (Boolean.TRUE.equals(dailyStatus.get(currentDateKey))) {
                    currentStreak++;
                } else {
                    currentStreak = 0;
                }
            }

            longestStreak = Math.max(longestStreak, currentStreak);

            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        return longestStreak;
    }

    @NonNull
    private Map<String, Boolean> getDailyStatus(List<TaskOccurrence> occurrences) {
        Map<String, Boolean> dailyStatus = new HashMap<>();

        for (TaskOccurrence occurrence : occurrences) {
            String dateKey = convertTimestampToDateString(occurrence.getDate());

            if (occurrence.getStatus() == TaskStatus.UNCOMPLETED || occurrence.getStatus() == TaskStatus.CANCELED) {
                dailyStatus.put(dateKey, false);
            }
            else if (occurrence.getStatus() == TaskStatus.COMPLETED && !dailyStatus.containsKey(dateKey)) {
                dailyStatus.put(dateKey, true);
            }
        }
        return dailyStatus;
    }

    private String convertTimestampToDateString(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        return String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, day);

    }

    public Map<String, Double> getAverageDailyDifficultyXp(String userId) {
        Map<Long, TaskDifficulty> completedTasks = taskRepository.getCompletedTaskDifficultiesWithDates(userId);
        if (completedTasks.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, List<Integer>> dailyXp = new HashMap<>();

        for (Map.Entry<Long, TaskDifficulty> entry : completedTasks.entrySet()) {
            String date = convertTimestampToDateString(entry.getKey());
            int baseXp = entry.getValue().getXp();

            dailyXp.computeIfAbsent(date, k -> new ArrayList<>()).add(baseXp);
        }

        Map<String, Double> averageDailyXp = new HashMap<>();
        for (Map.Entry<String, List<Integer>> entry : dailyXp.entrySet()) {
            double average = entry.getValue().stream().mapToDouble(val -> val).average().orElse(0.0);
            averageDailyXp.put(entry.getKey(), average);
        }

        return new TreeMap<>(averageDailyXp);
    }

    public Map<String, Integer> getWeeklyXp(String userId) {
        Map<String, Integer> weeklyXpFromDb = taskRepository.getWeeklyXp(userId);
        Map<String, Integer> weeklyXp = new TreeMap<>();

        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            String date = convertTimestampToDateString(calendar.getTimeInMillis());
            weeklyXp.put(date, 0);
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        }

        for (Map.Entry<String, Integer> entry : weeklyXpFromDb.entrySet()) {
            if (weeklyXp.containsKey(entry.getKey())) {
                weeklyXp.put(entry.getKey(), entry.getValue());
            }
        }

        return weeklyXp;
    }

    public Task<Integer> getConsecutiveActiveDays(String userId) {
        return userRepository.getUserById(userId).continueWith(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                return task.getResult().getConsecutiveActiveDays();
            }
            return 0;
        });
    }

}
