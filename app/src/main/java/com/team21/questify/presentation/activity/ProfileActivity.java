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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.team21.questify.R;
import com.team21.questify.application.model.Equipment;
import com.team21.questify.application.model.enums.EquipmentType;
import com.team21.questify.application.service.EquipmentService;
import com.team21.questify.application.service.SpecialMissionService;
import com.team21.questify.application.service.UserService;
import com.team21.questify.presentation.adapter.BadgeAdapter;
import com.team21.questify.presentation.adapter.EquipmentAdapter;
import com.team21.questify.utils.SharedPrefs;
import com.team21.questify.utils.LevelCalculator;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivAvatar, ivQrCode, ivAvatarBorder;
    private TextView tvUsername, tvLevel, tvTitle, tvPowerPoints, tvXpDetails, tvCoins, tvBadgesTitle;
    private Button btnChangePassword, btnAddFriend;
    private ProgressBar pbXpProgress;
    private boolean isMyProfile;
    private String profileUserId;
    private String currentUserId;
    private UserService userService;
    private SharedPrefs sharedPreferences;
    private EquipmentService equipmentService;
    private RecyclerView rvWeapons, rvArmor, rvPotions;
    private EquipmentAdapter weaponsAdapter, armorAdapter, potionsAdapter;
    private TextView tvWeaponsTitle, tvArmorTitle, tvPotionsTitle, tvNoEquipment;
    private ImageView ivShopIcon;
    private Button btnManageEquipment;
    private SpecialMissionService specialMissionService;
    private RecyclerView rvBadges;
    private BadgeAdapter badgeAdapter;
    private TextView tvNoBadges;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        setupToolbar();

        userService = new UserService(this);
        sharedPreferences = new SharedPrefs(this);
        equipmentService = new EquipmentService(this);
        specialMissionService = new SpecialMissionService(this);

        currentUserId = sharedPreferences.getUserUid();
        profileUserId = getIntent().getStringExtra("user_id");
        if (profileUserId == null || profileUserId.isEmpty()) {
            profileUserId = currentUserId;
        }

        isMyProfile = Objects.equals(currentUserId, profileUserId);

        setupProfileVisibility();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile(profileUserId);
        if (equipmentService != null) {
            loadUserInventory(profileUserId);
        }
        if (specialMissionService != null) {
            loadUserBadges(profileUserId);
        }
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
        tvNoEquipment = findViewById(R.id.tv_no_equipment);
        tvWeaponsTitle = findViewById(R.id.tv_weapons_title);
        rvWeapons = findViewById(R.id.rv_weapons);
        tvArmorTitle = findViewById(R.id.tv_armor_title);
        rvArmor = findViewById(R.id.rv_armor);
        tvPotionsTitle = findViewById(R.id.tv_potions_title);
        rvPotions = findViewById(R.id.rv_potions);
        ivShopIcon = findViewById(R.id.iv_shop_icon);btnManageEquipment = findViewById(R.id.btn_manage_equipment);
        rvBadges = findViewById(R.id.rv_badges);
        tvNoBadges = findViewById(R.id.tv_no_badges);
        tvBadgesTitle = findViewById(R.id.tv_badges_title);

        weaponsAdapter = new EquipmentAdapter();
        rvWeapons.setLayoutManager(new LinearLayoutManager(this));
        rvWeapons.setAdapter(weaponsAdapter);

        armorAdapter = new EquipmentAdapter();
        rvArmor.setLayoutManager(new LinearLayoutManager(this));
        rvArmor.setAdapter(armorAdapter);

        potionsAdapter = new EquipmentAdapter();
        rvPotions.setLayoutManager(new LinearLayoutManager(this));
        rvPotions.setAdapter(potionsAdapter);

        badgeAdapter = new BadgeAdapter();
        rvBadges.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvBadges.setAdapter(badgeAdapter);

        ivShopIcon.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ShopActivity.class);
            startActivity(intent);
        });
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        ivQrCode.setOnClickListener(v -> showQrCodeDialog());
        btnAddFriend.setOnClickListener(v -> addRemoveFriend());
        btnManageEquipment.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, InventoryActivity.class);
            startActivity(intent);
        });
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
        ivShopIcon.setVisibility(myProfileVisibility);
        btnManageEquipment.setVisibility(myProfileVisibility);

        if (getSupportActionBar() != null) {
            String title = getString(isMyProfile ? R.string.my_profile : R.string.user_profile);
            getSupportActionBar().setTitle(title);
        }

        if (isMyProfile) {
            btnAddFriend.setVisibility(View.GONE);
        } else {
            btnAddFriend.setVisibility(View.VISIBLE);
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
    private void loadUserInventory(String userId) {
        if (!isMyProfile) {
            equipmentService.getActiveInventory(userId).addOnSuccessListener(this::displayInventory);
            return;
        }

        equipmentService.getInventory(userId).addOnSuccessListener(this::displayInventory)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load inventory.", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserBadges(String userId) {
        specialMissionService.getAllEarnedBadges(userId)
                .addOnSuccessListener(badges -> {
                    if (badges == null || badges.isEmpty()) {
                        rvBadges.setVisibility(View.GONE);
                        tvNoBadges.setVisibility(View.VISIBLE);
                        tvBadgesTitle.setText(getString(R.string.badges_section_title));
                    } else {
                        rvBadges.setVisibility(View.VISIBLE);
                        tvNoBadges.setVisibility(View.GONE);
                        String titleWithCount = getString(R.string.badges_section_title) + " (" + badges.size() + ")";
                        tvBadgesTitle.setText(titleWithCount);
                        badgeAdapter.setBadges(badges);
                    }
                })
                .addOnFailureListener(e -> {
                    rvBadges.setVisibility(View.GONE);
                    tvNoBadges.setVisibility(View.VISIBLE);
                    tvBadgesTitle.setText(getString(R.string.badges_section_title));
                    Toast.makeText(this, "Failed to load badges.", Toast.LENGTH_SHORT).show();
                });
    }

    private void displayInventory(List<Equipment> inventory) {
        if (inventory.isEmpty()) {
            tvNoEquipment.setVisibility(View.VISIBLE);
            tvWeaponsTitle.setVisibility(View.GONE);
            rvWeapons.setVisibility(View.GONE);
            tvArmorTitle.setVisibility(View.GONE);
            rvArmor.setVisibility(View.GONE);
            tvPotionsTitle.setVisibility(View.GONE);
            rvPotions.setVisibility(View.GONE);

            if (btnManageEquipment != null) {
                btnManageEquipment.setVisibility(View.GONE);
            }
            return;
        }

        tvNoEquipment.setVisibility(View.GONE);

        List<Equipment> weapons = inventory.stream()
                .filter(i -> i.getType() == EquipmentType.WEAPON)
                .collect(Collectors.toList());
        weaponsAdapter.setItems(weapons);
        tvWeaponsTitle.setVisibility(weapons.isEmpty() ? View.GONE : View.VISIBLE);
        rvWeapons.setVisibility(weapons.isEmpty() ? View.GONE : View.VISIBLE);

        List<Equipment> armor = inventory.stream()
                .filter(i -> i.getType() == EquipmentType.ARMOR)
                .collect(Collectors.toList());
        armorAdapter.setItems(armor);
        tvArmorTitle.setVisibility(armor.isEmpty() ? View.GONE : View.VISIBLE);
        rvArmor.setVisibility(armor.isEmpty() ? View.GONE : View.VISIBLE);

        List<Equipment> potions = inventory.stream()
                .filter(i -> i.getType() == EquipmentType.POTION)
                .collect(Collectors.toList());
        potionsAdapter.setItems(potions);
        tvPotionsTitle.setVisibility(potions.isEmpty() ? View.GONE : View.VISIBLE);
        rvPotions.setVisibility(potions.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void addRemoveFriend() {
        if (isMyProfile) {
            Toast.makeText(this, "You can't add yourself as a friend.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAddFriend.setEnabled(false);

        if (btnAddFriend.getText().toString().equals(getString(R.string.add_friend_button))) {
            userService.addFriendship(currentUserId, profileUserId)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Friend added successfully!", Toast.LENGTH_SHORT).show();
                        btnAddFriend.setText(R.string.remove_friend_button);
                        btnAddFriend.setEnabled(true);
                    })
                    .addOnFailureListener(e -> {
                        btnAddFriend.setEnabled(true);
                        String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error.";
                        if (errorMessage.contains("already a friend")) {
                            Toast.makeText(this, "User is already a friend.", Toast.LENGTH_SHORT).show();
                            btnAddFriend.setText(R.string.remove_friend_button);
                        } else {
                            Toast.makeText(this, "Failed to add friend: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            userService.removeFriendship(currentUserId, profileUserId)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Friend successfully removed.", Toast.LENGTH_SHORT).show();
                        btnAddFriend.setText(R.string.add_friend_button);
                        btnAddFriend.setEnabled(true);
                    })
                    .addOnFailureListener(e -> {
                        btnAddFriend.setEnabled(true);
                        Toast.makeText(this, "Failed to remove friend.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void loadUserProfile(String userId) {
        userService.fetchUserProfile(userId)
                .addOnSuccessListener(user -> {
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
                        checkIfFriend(user.getUserId());
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user profile.", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkIfFriend(String profileId) {
        userService.fetchUserProfile(currentUserId)
                .addOnSuccessListener(currentUser -> {
                    List<String> friendsIds = currentUser.getFriendsIds();
                    if (friendsIds != null && friendsIds.contains(profileId)) {
                        btnAddFriend.setText(R.string.remove_friend_button);
                    } else {
                        btnAddFriend.setText(R.string.add_friend_button);
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

            tilOldPassword.setError(null);
            tilNewPassword.setError(null);
            tilConfirmNewPassword.setError(null);

            btnChange.setEnabled(false);

            userService.changePassword(oldPassword, newPassword, confirmNewPassword)
                    .addOnSuccessListener(aVoid -> {
                        btnChange.setEnabled(true);
                        Toast.makeText(ProfileActivity.this, "Password changed successfully.", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        btnChange.setEnabled(true);
                        String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error.";

                        if (errorMessage.contains("Incorrect old password")) {
                            tilOldPassword.setError("Incorrect old password.");
                        } else if (errorMessage.contains("at least 8 characters")) {
                            tilNewPassword.setError("New password must be at least 8 characters long.");
                        } else if (errorMessage.contains("do not match")) {
                            tilConfirmNewPassword.setError("New passwords do not match.");
                        } else {
                            Toast.makeText(ProfileActivity.this, "Failed: " + errorMessage, Toast.LENGTH_LONG).show();
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
