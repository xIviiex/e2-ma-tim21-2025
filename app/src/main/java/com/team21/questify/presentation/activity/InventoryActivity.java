package com.team21.questify.presentation.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.team21.questify.R;
import com.team21.questify.application.model.Equipment;
import com.team21.questify.application.model.enums.EquipmentType;
import com.team21.questify.application.service.EquipmentService;
import com.team21.questify.presentation.adapter.EquipmentAdapter;
import com.team21.questify.presentation.adapter.InventoryAdapter;
import com.team21.questify.utils.SharedPrefs;

import java.util.List;
import java.util.stream.Collectors;

public class InventoryActivity extends AppCompatActivity {

    private EquipmentService equipmentService;
    private RecyclerView rvWeapons, rvArmor, rvPotions;
    private InventoryAdapter weaponsAdapter, armorAdapter, potionsAdapter;
    private SharedPrefs sharedPrefs;
    private TextView tvWeaponsTitle, tvArmorTitle, tvPotionsTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        Toolbar toolbar = findViewById(R.id.toolbar_inventory);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initServicesAndViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInventory();
    }

    private void initServicesAndViews() {
        equipmentService = new EquipmentService(this);
        sharedPrefs = new SharedPrefs(this);

        rvWeapons = findViewById(R.id.rv_inventory_weapons);
        rvArmor = findViewById(R.id.rv_inventory_armor);
        rvPotions = findViewById(R.id.rv_inventory_potions);
        tvWeaponsTitle = findViewById(R.id.tv_inventory_weapons_title);
        rvWeapons = findViewById(R.id.rv_inventory_weapons);
        tvArmorTitle = findViewById(R.id.tv_inventory_armor_title);
        rvArmor = findViewById(R.id.rv_inventory_armor);
        tvPotionsTitle = findViewById(R.id.tv_inventory_potions_title);
        rvPotions = findViewById(R.id.rv_inventory_potions);

        InventoryAdapter.OnItemActionClickListener listener = this::handleItemAction;
        weaponsAdapter = new InventoryAdapter(listener);
        armorAdapter = new InventoryAdapter(listener);
        potionsAdapter = new InventoryAdapter(listener);

        rvWeapons.setLayoutManager(new LinearLayoutManager(this));
        rvWeapons.setAdapter(weaponsAdapter);
        rvArmor.setLayoutManager(new LinearLayoutManager(this));
        rvArmor.setAdapter(armorAdapter);
        rvPotions.setLayoutManager(new LinearLayoutManager(this));
        rvPotions.setAdapter(potionsAdapter);
    }

    private void loadInventory() {
        String userId = sharedPrefs.getUserUid();
        if (userId == null) return;

        equipmentService.getInventory(userId)
                .addOnSuccessListener(this::displayInventory)
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load inventory.", Toast.LENGTH_SHORT).show());
    }

    private void displayInventory(List<Equipment> inventory) {
        List<Equipment> weapons = inventory.stream()
                .filter(i -> i.getType() == EquipmentType.WEAPON)
                .collect(Collectors.toList());
        weaponsAdapter.setItems(weapons);

        int weaponsVisibility = weapons.isEmpty() ? View.GONE : View.VISIBLE;
        tvWeaponsTitle.setVisibility(weaponsVisibility);
        rvWeapons.setVisibility(weaponsVisibility);

        List<Equipment> armor = inventory.stream()
                .filter(i -> i.getType() == EquipmentType.ARMOR)
                .collect(Collectors.toList());
        armorAdapter.setItems(armor);

        int armorVisibility = armor.isEmpty() ? View.GONE : View.VISIBLE;
        tvArmorTitle.setVisibility(armorVisibility);
        rvArmor.setVisibility(armorVisibility);

        List<Equipment> potions = inventory.stream()
                .filter(i -> i.getType() == EquipmentType.POTION)
                .collect(Collectors.toList());
        potionsAdapter.setItems(potions);

        int potionsVisibility = potions.isEmpty() ? View.GONE : View.VISIBLE;
        tvPotionsTitle.setVisibility(potionsVisibility);
        rvPotions.setVisibility(potionsVisibility);
    }

    private void handleItemAction(Equipment item) {
        Task<Void> actionTask;
        if (item.isActive()) {
            actionTask = equipmentService.deactivateItem(item);
        } else {
            actionTask = equipmentService.activateItem(item);
        }

        actionTask.addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Status updated!", Toast.LENGTH_SHORT).show();
            loadInventory();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
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