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
import com.team21.questify.data.repository.UserRepository;
import com.team21.questify.utils.SharedPrefs;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivAvatar, ivQrCode;
    private TextView tvUsername, tvLevel, tvTitle, tvPowerPoints, tvXp, tvCoins, tvBadgesStatus, tvEquipmentStatus;
    private Button btnChangePassword;

    private UserService userService;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.my_profile);
        }

        userService = new UserService(this);
        userRepository = new UserRepository(this);

        ivAvatar = findViewById(R.id.iv_profile_avatar);
        tvUsername = findViewById(R.id.tv_profile_username);
        tvLevel = findViewById(R.id.tv_profile_level);
        tvTitle = findViewById(R.id.tv_profile_title);
        tvPowerPoints = findViewById(R.id.tv_profile_pp);
        tvXp = findViewById(R.id.tv_profile_xp);
        tvCoins = findViewById(R.id.tv_profile_coins);
        tvBadgesStatus = findViewById(R.id.tv_badges_status);
        tvEquipmentStatus = findViewById(R.id.tv_equipment_status);
        btnChangePassword = findViewById(R.id.btn_change_password);
        ivQrCode = findViewById(R.id.iv_qr_code_icon);

        loadUserProfile();
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        ivQrCode.setOnClickListener(v -> showQrCodeDialog());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            userService.logoutUser();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadUserProfile() {
        String userId = new SharedPrefs(this).getUserUid();
        if (userId == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRepository.fetchUserFromFirebase(userId, task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                User user = task.getResult().toObject(User.class);
                if (user != null) {
                    tvUsername.setText(user.getUsername());
                    tvLevel.setText("Level: " + user.getLevel());
                    tvTitle.setText("Title: " + user.getTitle());
                    tvPowerPoints.setText("PP: " + user.getPowerPoints());
                    tvXp.setText("XP: " + user.getXp());
                    tvCoins.setText("Coins: " + user.getCoins());

                    if (user.getBadgesCount() > 0) {
                        tvBadgesStatus.setText("You have " + user.getBadgesCount() + " badges.");
                    } else {
                        tvBadgesStatus.setText(R.string.no_badges_message);
                    }

                    if (user.getEquipment() != null && !user.getEquipment().isEmpty()) {
                        tvEquipmentStatus.setText("You have " + user.getEquipmentCount() + " pieces of equipment.");
                    } else {
                        tvEquipmentStatus.setText(R.string.no_equipment_message);
                    }

                    int resId = getResources().getIdentifier(user.getAvatarName(), "drawable", getPackageName());
                    if (resId != 0) {
                        ivAvatar.setImageResource(resId);
                    } else {
                        ivAvatar.setImageResource(R.drawable.default_avatar);
                    }
                }
            } else {
                Toast.makeText(this, "Failed to load user profile.", Toast.LENGTH_SHORT).show();
            }
        });
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
            String oldPassword = tilOldPassword.getEditText().getText().toString().trim();
            String newPassword = tilNewPassword.getEditText().getText().toString().trim();
            String confirmNewPassword = tilConfirmNewPassword.getEditText().getText().toString().trim();

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
        String userId = new SharedPrefs(this).getUserUid();
        if (userId == null) {
            Toast.makeText(this, "User ID not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_qr_code, null);
            builder.setView(dialogView);

            ImageView ivQrCode = dialogView.findViewById(R.id.iv_qr_code_display);

            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(userId, BarcodeFormat.QR_CODE, 400, 400);

            ivQrCode.setImageBitmap(bitmap);

            AlertDialog dialog = builder.create();
            dialog.show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR code.", Toast.LENGTH_SHORT).show();
        }
    }

}