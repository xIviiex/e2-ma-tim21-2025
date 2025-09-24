package com.team21.questify.presentation.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.Tasks;
import com.team21.questify.R;
import com.team21.questify.application.model.enums.TaskStatus;
import com.team21.questify.application.service.UserStatisticsService;
import com.team21.questify.utils.SharedPrefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserStatisticsActivity extends AppCompatActivity {
    private UserStatisticsService statisticsService;
    private SharedPrefs sharedPrefs;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_statistics);

        Toolbar toolbar = findViewById(R.id.toolbar_statistics);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Statistics");
        }

        statisticsService = new UserStatisticsService(this);
        sharedPrefs = new SharedPrefs(this);
        currentUserId = sharedPrefs.getUserUid();

        if (currentUserId != null) {
            loadAndDisplayStatistics();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadAndDisplayStatistics() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                final Map<TaskStatus, Integer> taskCounts = statisticsService.getTaskCountsByStatus(currentUserId);
                final Map<String, Integer> weeklyXp = statisticsService.getWeeklyXp(currentUserId);
                final int longestStreak = Tasks.await(statisticsService.getLongestStreakOfCompletedTasks(currentUserId));
                final Map<String, Integer> categoryCounts = statisticsService.getCompletedTaskCountsByCategory(currentUserId);
                final Map<String, Double> avgDifficulty = statisticsService.getAverageDailyDifficultyXp(currentUserId);
                final int startedMissions = Tasks.await(statisticsService.countStartedMissionsForUser(currentUserId));
                final int completedMissions = Tasks.await(statisticsService.countCompletedMissionsForUser(currentUserId));

                final int activeDays = com.google.android.gms.tasks.Tasks.await(statisticsService.getConsecutiveActiveDays(currentUserId));

                runOnUiThread(() -> {
                    displayTaskCountsByStatus(taskCounts);
                    displayActiveDaysStreak(activeDays);
                    displayWeeklyXp(weeklyXp);
                    displayLongestTaskStreak(longestStreak);
                    displayCompletedTasksByCategory(categoryCounts);
                    displayAverageDifficultyXp(avgDifficulty);
                    displaySpecialMissionsCount(startedMissions, completedMissions);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(UserStatisticsActivity.this, "Failed to load statistics.", Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        });
    }

    private void displayTaskCountsByStatus(Map<TaskStatus, Integer> counts) {
        PieChart pieChart = new PieChart(this);
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        for (Map.Entry<TaskStatus, Integer> entry : counts.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey().name()));
            colors.add(getTaskStatusColor(entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Task status");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        PieData pieData = new PieData(dataSet);

        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Task status");
        pieChart.animateY(1000);

        FrameLayout container = findViewById(R.id.donutChartContainer);
        container.removeAllViews();
        container.addView(pieChart);
    }

    private void displayActiveDaysStreak(int streak) {
        TextView streakTextView = findViewById(R.id.tvActiveDaysStreak);
        streakTextView.setText(String.format("%d days", streak));
    }

    private void displayWeeklyXp(Map<String, Integer> weeklyXp) {
        LineChart lineChart = new LineChart(this);
        List<Entry> entries = new ArrayList<>();
        List<String> dates = new ArrayList<>(weeklyXp.keySet());
        Collections.sort(dates);

        for (int i = 0; i < dates.size(); i++) {
            entries.add(new Entry(i, weeklyXp.get(dates.get(i))));
        }

        LineDataSet dataSet = new LineDataSet(entries, "XP gained");
        dataSet.setColor(ColorTemplate.getHoloBlue());
        dataSet.setLineWidth(2f);
        dataSet.setValueTextSize(10f);
        LineData lineData = new LineData(dataSet);

        lineChart.setData(lineData);
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(dates));
        lineChart.getXAxis().setGranularity(1f);

        FrameLayout container = findViewById(R.id.lineChartContainer);
        container.removeAllViews();
        container.addView(lineChart);
    }

    private void displayLongestTaskStreak(int streak) {
        TextView streakTextView = findViewById(R.id.tvLongestTaskStreak);
        streakTextView.setText(String.format("%d days", streak));
    }

    private void displayCompletedTasksByCategory(Map<String, Integer> counts) {
        BarChart barChart = new BarChart(this);
        List<BarEntry> entries = new ArrayList<>();
        List<String> categoryLabels = new ArrayList<>();
        int i = 0;

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            entries.add(new BarEntry(i, entry.getValue()));
            categoryLabels.add(entry.getKey());
            i++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Completed tasks by category");
        dataSet.setColors(ColorTemplate.JOYFUL_COLORS);
        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.setDrawValueAboveBar(true);

        barChart.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(categoryLabels));
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setCenterAxisLabels(true);

        FrameLayout container = findViewById(R.id.barChartContainer);
        container.removeAllViews();
        container.addView(barChart);
    }

    private void displayAverageDifficultyXp(Map<String, Double> averageDifficultyXp) {
        LineChart lineChart = new LineChart(this);
        List<Entry> entries = new ArrayList<>();
        List<String> dates = new ArrayList<>(averageDifficultyXp.keySet());
        Collections.sort(dates);

        for (int i = 0; i < dates.size(); i++) {
            entries.add(new Entry(i, Objects.requireNonNull(averageDifficultyXp.get(dates.get(i))).floatValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Average Task Difficulty (XP)");
        dataSet.setColor(ColorTemplate.VORDIPLOM_COLORS[0]);
        dataSet.setLineWidth(2f);
        dataSet.setValueTextSize(10f);
        LineData lineData = new LineData(dataSet);

        lineChart.setData(lineData);
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(dates));
        lineChart.getXAxis().setGranularity(1f);

        FrameLayout container = findViewById(R.id.averageDifficultyChartContainer);
        container.removeAllViews();
        container.addView(lineChart);
    }

    private void displaySpecialMissionsCount(int started, int completed) {
        TextView tvMissions = findViewById(R.id.tvSpecialMissionsCount);
        if (tvMissions != null) {
            tvMissions.setText(String.format("%d Started / %d Completed", started, completed));
        }
    }

    private int getTaskStatusColor(TaskStatus status) {
        switch (status) {
            case ACTIVE: return Color.parseColor("#4285F4");
            case PAUSED: return Color.parseColor("#FBBC05");
            case UNCOMPLETED: return Color.parseColor("#DB4437");
            case COMPLETED: return Color.parseColor("#0F9D58");
            case CANCELED: return Color.parseColor("#A8A9AD");
            default: return Color.GRAY;
        }
    }
}
