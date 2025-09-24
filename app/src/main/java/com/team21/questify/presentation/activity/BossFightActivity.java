package com.team21.questify.presentation.activity;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.appbar.MaterialToolbar;
import com.team21.questify.R;
import com.team21.questify.application.model.BattleStats;
import com.team21.questify.application.model.Boss;
import com.team21.questify.application.model.User;
import com.team21.questify.application.model.enums.EquipmentId;
import com.team21.questify.application.model.enums.MissionActionType;
import com.team21.questify.application.service.BossService;
import com.team21.questify.application.service.EquipmentService;
import com.team21.questify.application.service.SpecialMissionService;
import com.team21.questify.application.service.TaskOccurrenceService;
import com.team21.questify.application.service.UserService;
import com.team21.questify.utils.LevelCalculator;
import com.team21.questify.utils.ShakeDetector;
import com.team21.questify.utils.SharedPrefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class BossFightActivity extends AppCompatActivity {

    // Servisi
    private BossService bossService;
    private UserService userService;
    private EquipmentService equipmentService;
    private SharedPrefs sharedPrefs;
    private TaskOccurrenceService taskOccurrenceService;
    private SpecialMissionService missionService;
    // Modeli
    private Boss currentBoss;
    private User currentUser;
    private BattleStats battleStats;

    // UI Elementi
    private MaterialToolbar toolbar;
    private LottieAnimationView bossAnimationView;
    private ProgressBar bossHpProgressBar;
    private TextView bossHpText, userPpText, hitChanceText, attackCounterText;
    private Button attackButton, seeInventoryButton;
    private ConstraintLayout rewardsOverlay;
    private LottieAnimationView chestAnimationView;

    // Senzor za "Shake"
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;
    private boolean isChestShakeListenerActive = false;


    // Logika borbe
    private static final long HIT_ANIMATION_DURATION = 1000;
    private static final int MAX_ATTACKS = 5;
    private int attacksLeft = MAX_ATTACKS;
    private int userPowerPoints = 0;
    private int hitChance = 0;
    private double initialBossHp = 0;
    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boss_fight);

        initServices();
        initViews();
        initShakeDetector();
        loadDataAndSetupFight();
    }

    private void initServices() {
        bossService = new BossService(this);
        userService = new UserService(this);
        equipmentService = new EquipmentService(this);
        sharedPrefs = new SharedPrefs(this);
        taskOccurrenceService = new TaskOccurrenceService(this);
        missionService = new SpecialMissionService(this);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        bossAnimationView = findViewById(R.id.bossAnimationView);
        bossHpProgressBar = findViewById(R.id.bossHpProgressBar);
        bossHpText = findViewById(R.id.bossHpText);
        userPpText = findViewById(R.id.userPpText);
        hitChanceText = findViewById(R.id.hitChanceText);
        attackCounterText = findViewById(R.id.attackCounterText);
        attackButton = findViewById(R.id.attackButton);
        seeInventoryButton = findViewById(R.id.seeInventory);
        rewardsOverlay = findViewById(R.id.rewardsOverlay);
        chestAnimationView = findViewById(R.id.chestAnimationView);

        attackButton.setOnClickListener(v -> performAttack());
        seeInventoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(BossFightActivity.this, InventoryActivity.class);
            startActivity(intent);
        });
    }

    private void initShakeDetector() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        shakeDetector = new ShakeDetector();
        shakeDetector.setOnShakeListener(count -> {
            if (isChestShakeListenerActive) {
                openChest();
            } else {
                performAttack();
            }
        });
    }

    private Task<Boss> getBossTaskWrapper() {
        TaskCompletionSource<Boss> tcs = new TaskCompletionSource<>();
        bossService.getBossForNextFight(task -> {
            if (task.isSuccessful()) {
                tcs.setResult(task.getResult());
            } else {
                tcs.setException(task.getException());
            }
        });
        return tcs.getTask();
    }

    private void loadDataAndSetupFight() {
        String userId = sharedPrefs.getUserUid();
        if (userId == null) {
            finishWithError("Authentication error!");
            return;
        }


        userService.fetchUserProfile(userId).addOnSuccessListener(user -> {
            if (user == null) {
                finishWithError("Failed to fetch user profile (user is null).");
                return;
            }

            this.currentUser = user;


            Task<Boss> bossTask = getBossTaskWrapper();
            Task<BattleStats> statsTask = equipmentService.calculateBattleStats(userId);


            Task<Integer> successRateTask = taskOccurrenceService.getSuccessRateForStage(
                    this.currentUser.getPreviousLevelUpTimestamp(),
                    this.currentUser.getCurrentLevelUpTimestamp()
            );




            Tasks.whenAllSuccess(bossTask, statsTask, successRateTask).addOnSuccessListener(results -> {

                this.currentBoss = (Boss) results.get(0);
                this.battleStats = (BattleStats) results.get(1);
                int taskSuccessRate = (Integer) results.get(2);


                if (currentBoss == null || battleStats == null) {
                    finishWithError("Failed to load secondary fight data.");
                    return;
                }

                // Statistike borbe
                this.initialBossHp = currentBoss.getCurrentHp();
                this.userPowerPoints = (int) (this.currentUser.getPowerPoints() * battleStats.getPowerPointsMultiplier());
                this.hitChance = (int) (taskSuccessRate + battleStats.getHitChanceBonus() * 100);


                updateUI();

            }).addOnFailureListener(e -> finishWithError("Error loading secondary fight data: " + e.getMessage()));

        }).addOnFailureListener(e -> {

            finishWithError("Error fetching user profile: " + e.getMessage());
        });
    }

    private void updateUI() {
        getSupportActionBar().setTitle(String.format(Locale.getDefault(), "Boss: Level %d", currentBoss.getLevel()));
        bossHpProgressBar.setMax((int) currentBoss.getMaxHp());
        updateBossHpUI();
        userPpText.setText(String.format(Locale.getDefault(), "Snaga (PP): %d", userPowerPoints));
        hitChanceText.setText(String.format(Locale.getDefault(), "Šansa: %d%%", hitChance));
        updateAttackCounterUI();
    }


    private void performAttack() {

        attackButton.setEnabled(false);

        if (attacksLeft <= 0 || (currentBoss != null && currentBoss.getCurrentHp() <= 0)) {
            attackButton.setEnabled(true);
            return;
        }

        attacksLeft--;

        if (random.nextDouble() < battleStats.getExtraAttackChance()) {
            Toast.makeText(this, "EXTRA ATTACK!", Toast.LENGTH_SHORT).show();
            attacksLeft++;
        }

        updateAttackCounterUI();

        if (random.nextInt(100) < hitChance) {

            missionService.recordUserAction(MissionActionType.REGULAR_BOSS_HIT, task -> {
                if (task.isSuccessful()) {
                    Log.d("BossFightActivity", "Regular boss hit successfully recorded for special mission.");
                } else if (task.getException() != null) {
                    // Samo logujemo grešku, ne prikazujemo je korisniku da ne bi smetala.
                    Log.e("BossFightActivity", "Failed to record regular boss hit.", task.getException());
                }
            });

            currentBoss.setCurrentHp(currentBoss.getCurrentHp() - userPowerPoints);
            if (currentBoss.getCurrentHp() < 0) currentBoss.setCurrentHp(0);
            Toast.makeText(this, "HIT! Dealt " + userPowerPoints + " damage!", Toast.LENGTH_SHORT).show();


            bossAnimationView.loop(false);
            bossAnimationView.setAnimation(R.raw.boss_hit_animation);
            bossAnimationView.playAnimation();


            new Handler(Looper.getMainLooper()).postDelayed(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                bossAnimationView.setAnimation(R.raw.boss_idle_animation);
                bossAnimationView.loop(true);
                bossAnimationView.playAnimation();


                if (attacksLeft > 0) {
                    attackButton.setEnabled(true);
                }
            }, HIT_ANIMATION_DURATION);

        } else {

            Toast.makeText(this, "MISS!", Toast.LENGTH_SHORT).show();

            if (attacksLeft > 0) {
                attackButton.setEnabled(true);
            }
        }

        updateBossHpUI();


        if (attacksLeft <= 0 || currentBoss.getCurrentHp() == 0) {

            new Handler(Looper.getMainLooper()).postDelayed(this::endFight, HIT_ANIMATION_DURATION);
        }
    }

    private void endFight() {
        sensorManager.unregisterListener(shakeDetector);
        attackButton.setEnabled(false);
        seeInventoryButton.setEnabled(false);

        double damageDealt = initialBossHp - currentBoss.getCurrentHp();
        boolean isFullWin = currentBoss.getCurrentHp() <= 0;
        boolean isPartialWin = !isFullWin && (damageDealt >= initialBossHp / 2);


        if (isFullWin) {
            currentBoss.setIsDefeated(true);
        }
        bossService.updateBoss(currentBoss, task -> Log.d("BossFight", "Boss state saved."));


        if (isPartialWin) {
            battleStats.setCoinsMultiplier(battleStats.getCoinsMultiplier() / 2);
        }


        if (isFullWin || isPartialWin) {
            userService.addPPAndCoinsAfterBossBattle(currentUser.getUserId(), battleStats, isFullWin)
                    .addOnSuccessListener(aVoid -> Log.d("BossFight", "User stats (PP, Coins) updated successfully."))
                    .addOnFailureListener(e -> Log.e("BossFight", "Failed to update user stats.", e));
        }


        if (isFullWin) {
            handleRewards(20);
        } else if (isPartialWin) {
            handleRewards(10);
        } else {
            showEndDialog("You Lost!", "Get stronger and try again!");
        }

        equipmentService.processEquipmentAfterBossBattle(currentUser.getUserId())
                .addOnSuccessListener(v -> Log.d("BossFight", "Equipment processed."))
                .addOnFailureListener(e -> Log.e("BossFight", "Failed to process equipment.", e));
    }


    private void handleRewards(int equipmentChance) {


        EquipmentId rewardedItem = null;
        if (random.nextInt(100) < equipmentChance) {
            rewardedItem = (random.nextInt(100) < 95) ? getRandomArmor() : getRandomWeapon();
            equipmentService.rewardEquipment(currentUser.getUserId(), rewardedItem);
        }

        rewardsOverlay.setVisibility(View.VISIBLE);
        isChestShakeListenerActive = true;
        sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);


        String constructedMessage = "The battle is over!";
        constructedMessage +="\n\n \uD83E\uDE99 " + LevelCalculator.getCoinsForLevel(currentUser.getLevel())*battleStats.getCoinsMultiplier();
        if (rewardedItem != null) {
            constructedMessage += "\nYou found a new item in the chest!\n\t \uD83C\uDF92" + rewardedItem.name();

        } else {
            constructedMessage += "\n\nYou search the chest but find no new equipment this time.";
        }

        final String finalMessage = constructedMessage;

        chestAnimationView.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animation) {}

            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                showEndDialog("Victory!", finalMessage);
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animation) {}

            @Override
            public void onAnimationRepeat(@NonNull Animator animation) {}
        });
    }

    private void openChest() {
        if (isChestShakeListenerActive) {
            isChestShakeListenerActive = false;
            sensorManager.unregisterListener(shakeDetector);
            chestAnimationView.playAnimation();
        }
    }


    private void updateBossHpUI() {
        bossHpProgressBar.setProgress((int) currentBoss.getCurrentHp());
        bossHpText.setText(String.format(Locale.getDefault(), "%.0f / %.0f HP", currentBoss.getCurrentHp(), currentBoss.getMaxHp()));
    }

    private void updateAttackCounterUI() {
        attackCounterText.setText(String.format(Locale.getDefault(), "Attacks: %d/%d", attacksLeft, MAX_ATTACKS));
    }

    private EquipmentId getRandomArmor() {
        EquipmentId[] armors = {EquipmentId.ARMOR_BOOTS, EquipmentId.ARMOR_GLOVES, EquipmentId.ARMOR_SHIELD};
        return armors[random.nextInt(armors.length)];
    }

    private EquipmentId getRandomWeapon() {
        EquipmentId[] weapons = {EquipmentId.WEAPON_SWORD, EquipmentId.WEAPON_BOW};
        return weapons[random.nextInt(weapons.length)];
    }




    private void showEndDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void finishWithError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e("BossFightActivity", message);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!isChestShakeListenerActive) {
            sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }



    @Override
    protected void onPause() {
        sensorManager.unregisterListener(shakeDetector);
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}