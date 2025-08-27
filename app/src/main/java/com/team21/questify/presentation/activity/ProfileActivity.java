package com.team21.questify.presentation.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.team21.questify.R;
import com.team21.questify.application.model.User;
import com.team21.questify.application.service.UserService;
import com.team21.questify.utils.SharedPrefs;
import com.team21.questify.utils.LevelCalculator;

import java.util.List;
import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivAvatar, ivQrCode, ivAvatarBorder;
    private TextView tvUsername, tvLevel, tvTitle, tvPowerPoints, tvXpDetails, tvCoins;
    private Button btnChangePassword, btnAddFriend;
    private ProgressBar pbXpProgress;
    private boolean isMyProfile;
    private String profileUserId;
    private String currentUserId;
    private UserService userService;
    private SharedPrefs sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        setupToolbar();

        userService = new UserService(this);
        sharedPreferences = new SharedPrefs(this);

        currentUserId = sharedPreferences.getUserUid();
        profileUserId = getIntent().getStringExtra("user_id");
        if (profileUserId == null || profileUserId.isEmpty()) {
            profileUserId = currentUserId;
        }

        isMyProfile = Objects.equals(currentUserId, profileUserId);

        setupProfileVisibility();
        loadUserProfile(profileUserId);
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.iv_profile_avatar);
        ivQrCode = findViewById(R.id.iv_qr_code_icon);
        ivAvatarBorder = findViewById(R.id.iv_avatar_border);
        btnAddFriend = findViewById(R.id.btn_add_friend);
        tvUsername = findViewById(R.id.tv_profile_username);
        tvLevel = findViewById(R.id.tv_profile_level);
        tvTitle = findViewById(R.id.tv_profile_title);
        tvPowerPoints = findViewById(R.id.tv_profile_pp);
        tvCoins = findViewById(R.id.tv_profile_coins);
        btnChangePassword = findViewById(R.id.btn_change_password);
        pbXpProgress = findViewById(R.id.pb_xp_progress);
        tvXpDetails = findViewById(R.id.tv_xp_details);

        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        ivQrCode.setOnClickListener(v -> showQrCodeDialog());
        btnAddFriend.setOnClickListener(v -> addFriend());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void setupProfileVisibility() {
        int myProfileVisibility = isMyProfile ? View.VISIBLE : View.GONE;

        tvPowerPoints.setVisibility(myProfileVisibility);
        tvCoins.setVisibility(myProfileVisibility);
        btnChangePassword.setVisibility(myProfileVisibility);

        List<String> friendsIds = userService.getUserById(sharedPreferences.getUserUid()).getFriendsIds();
        boolean isFriend = friendsIds != null && friendsIds.contains(currentUserId);
        if (isMyProfile || isFriend) {
            btnAddFriend.setVisibility(View.GONE);
        } else {
            btnAddFriend.setVisibility(View.VISIBLE);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(isMyProfile ? R.string.my_profile : R.string.user_profile);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_profile, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem logoutItem = menu.findItem(R.id.action_logout);
        MenuItem statsItem = menu.findItem(R.id.action_statistics);
        MenuItem friendsItem = menu.findItem(R.id.action_friends);
        if (logoutItem != null) {
            logoutItem.setVisible(isMyProfile);
        }
        if (statsItem != null) {
            statsItem.setVisible(isMyProfile);
        }
        if (friendsItem != null) {
            friendsItem.setVisible(isMyProfile);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (item.getItemId() == R.id.action_logout) {
            userService.logoutUser();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_statistics) {
            Intent intent = new Intent(this, UserStatisticsActivity.class);
            startActivity(intent);
            return true;
        }
        if (item.getItemId() == R.id.action_friends) {
            Intent intent = new Intent(this, FriendsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addFriend() {
        userService.fetchUserProfile(profileUserId, userTask -> {
            if (!userTask.isSuccessful() || userTask.getResult() == null) {
                Toast.makeText(this, "Failed to fetch user data.", Toast.LENGTH_SHORT).show();
                return;
            }

            User user = userTask.getResult();
            String profileUsername = user.getUsername();

            userService.addFriend(currentUserId, profileUserId, addFriendTask -> {
                if (addFriendTask.isSuccessful()) {
                    userService.addFriend(profileUserId, currentUserId, reverseAddTask -> {
                        if (reverseAddTask.isSuccessful()) {
                            Toast.makeText(this, "You and " + profileUsername + " are now friends!", Toast.LENGTH_SHORT).show();
                            btnAddFriend.setVisibility(View.GONE);
                        } else {
                            Toast.makeText(this, "Failed to complete friendship.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    String errorMessage = addFriendTask.getException() != null ? addFriendTask.getException().getMessage() : "Unknown error.";

                    if (errorMessage.contains("already a friend")) {
                        Toast.makeText(this, "User is already a friend.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to add friend: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    private void loadUserProfile(String userId) {
        userService.fetchUserProfile(userId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User user = task.getResult();
                tvUsername.setText(user.getUsername());
                tvLevel.setText("Level: " + user.getLevel());
                tvTitle.setText("(" + user.getTitle() + ")");
                tvPowerPoints.setText("PP: " + user.getPowerPoints());
                tvCoins.setText("Coins: " + user.getCoins());

                int xpToNextLevel = LevelCalculator.getRequiredXpForNextLevel(user.getLevel());
                pbXpProgress.setMax(xpToNextLevel);
                pbXpProgress.setProgress(user.getXp());
                tvXpDetails.setText(user.getXp() + " / " + xpToNextLevel + " XP");

                setAvatarBorder(user.getTitle());

                int resId = getResources().getIdentifier(user.getAvatarName(), "drawable", getPackageName());
                if (resId != 0) {
                    ivAvatar.setImageResource(resId);
                } else {
                    ivAvatar.setImageResource(R.drawable.default_avatar);
                }

                if (!isMyProfile) {
                    List<String> friendsIds = userService.getUserById(sharedPreferences.getUserUid()).getFriendsIds();
                    if (friendsIds != null && friendsIds.contains(user.getUserId())) {
                        btnAddFriend.setVisibility(View.GONE);
                    } else {
                        btnAddFriend.setVisibility(View.VISIBLE);
                    }
                }
            } else {
                Toast.makeText(this, "Failed to load user profile.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setAvatarBorder(String title) {
        int drawableResId;
        switch (title) {
            case "Adventurer":
                drawableResId = R.drawable.avatar_border_adventurer;
                break;
            case "Journeyman":
                drawableResId = R.drawable.avatar_border_journeyman;
                break;
            case "Hero":
                drawableResId = R.drawable.avatar_border_hero;
                break;
            default:
                drawableResId = R.drawable.avatar_border_master;
                break;
        }
        ivAvatarBorder.setImageResource(drawableResId);
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        final TextInputLayout tilOldPassword = dialogView.findViewById(R.id.til_old_password);
        final TextInputLayout tilNewPassword = dialogView.findViewById(R.id.til_new_password);
        final TextInputLayout tilConfirmNewPassword = dialogView.findViewById(R.id.til_confirm_new_password);
        Button btnChange = dialogView.findViewById(R.id.btn_change_password_dialog);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnChange.setOnClickListener(v -> {
            String oldPassword = Objects.requireNonNull(tilOldPassword.getEditText()).getText().toString().trim();
            String newPassword = Objects.requireNonNull(tilNewPassword.getEditText()).getText().toString().trim();
            String confirmNewPassword = Objects.requireNonNull(tilConfirmNewPassword.getEditText()).getText().toString().trim();

            userService.changePassword(oldPassword, newPassword, confirmNewPassword, task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(ProfileActivity.this, "Password changed successfully.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error.";
                    Toast.makeText(ProfileActivity.this, "Failed to change password: " + errorMessage, Toast.LENGTH_SHORT).show();

                    if (errorMessage.contains("Incorrect old password")) {
                        tilOldPassword.setError("Incorrect old password.");
                    } else if (errorMessage.contains("New password must be at least")) {
                        tilNewPassword.setError("New password is too short.");
                    } else if (errorMessage.contains("New passwords do not match")) {
                        tilConfirmNewPassword.setError("Passwords do not match.");
                    } else {
                        Toast.makeText(ProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }
    private void showQrCodeDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_qr_code, null);
            builder.setView(dialogView);

            ImageView ivQrCodeDisplay = dialogView.findViewById(R.id.iv_qr_code_display);

            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(profileUserId, BarcodeFormat.QR_CODE, 400, 400);

            ivQrCodeDisplay.setImageBitmap(bitmap);

            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR code.", Toast.LENGTH_SHORT).show();
        }
    }
}