package com.team21.questify.presentation.activity;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.team21.questify.R;
import com.team21.questify.application.model.SpecialMission;
import com.team21.questify.application.service.SpecialMissionService;

public class SpecialMissionBossActivity extends AppCompatActivity {


    private ProgressBar bossHpProgressBar;
    private TextView bossHpText;
    private Toolbar toolbar;


    private SpecialMissionService missionService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_special_mission_boss);


        initViews();


        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }


        missionService = new SpecialMissionService(this);


        loadMissionData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        bossHpProgressBar = findViewById(R.id.bossHpProgressBar);
        bossHpText = findViewById(R.id.bossHpText);
    }


    private void loadMissionData() {

        missionService.getActiveMissionForCurrentAlliance(task -> {
            if (task.isSuccessful()) {
                SpecialMission mission = task.getResult();
                if (mission != null) {

                    updateUI(mission);
                } else {

                    Toast.makeText(this, "No active mission found.", Toast.LENGTH_LONG).show();
                    finish();
                }
            } else {

                Exception e = task.getException();
                String errorMessage = e != null ? e.getMessage() : "Failed to load mission data.";
                Toast.makeText(this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }


    private void updateUI(SpecialMission mission) {
        int currentHp = mission.getCurrentBossHp();
        int maxHp = mission.getInitialBossHp();


        int progressPercentage = (maxHp > 0) ? (int) (((double) currentHp / maxHp) * 100) : 0;

        bossHpProgressBar.setMax(100);
        bossHpProgressBar.setProgress(progressPercentage);
        bossHpText.setText(String.format("%d / %d HP", currentHp, maxHp));
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}