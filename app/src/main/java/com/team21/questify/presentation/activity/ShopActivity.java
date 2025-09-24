package com.team21.questify.presentation.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.team21.questify.R;
import com.team21.questify.application.model.enums.MissionActionType;
import com.team21.questify.application.service.EquipmentService;
import com.team21.questify.application.service.SpecialMissionService;
import com.team21.questify.application.service.UserService;
import com.team21.questify.presentation.adapter.ShopAdapter;
import com.team21.questify.utils.EquipmentHelper;
import com.team21.questify.utils.SharedPrefs;

public class ShopActivity extends AppCompatActivity {

    private EquipmentService equipmentService;
    private UserService userService;
    private RecyclerView rvShopItems;
    private SharedPrefs sharedPrefs;
    private SpecialMissionService missionService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        Toolbar toolbar = findViewById(R.id.toolbar_shop);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Shop");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        equipmentService = new EquipmentService(this);
        userService = new UserService(this);
        sharedPrefs = new SharedPrefs(this);
        rvShopItems = findViewById(R.id.rv_shop_items);
        rvShopItems.setLayoutManager(new LinearLayoutManager(this));
        missionService = new SpecialMissionService(this);
        loadShop();
    }

    private void loadShop() {
        String userId = sharedPrefs.getUserUid();
        userService.fetchUserProfile(userId).addOnSuccessListener(user -> {
            ShopAdapter adapter = new ShopAdapter(EquipmentHelper.ShopItem.getShopItems(), user.getLevel(), equipmentService, (item, buyButton) -> {
                buyItem(item, buyButton);
            });
            rvShopItems.setAdapter(adapter);
        });
    }

    private void buyItem(EquipmentHelper.ShopItem item, Button buyButton) {
        buyButton.setEnabled(false);

        equipmentService.buyItem(sharedPrefs.getUserUid(), item)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, item.name + " purchased!", Toast.LENGTH_SHORT).show();
                    buyButton.setEnabled(true);
                    recordStorePurchase();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Purchase failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    buyButton.setEnabled(true);
                });
    }

    private void recordStorePurchase() {
        missionService.recordUserAction(MissionActionType.STORE_PURCHASE, task -> {
            if (task.isSuccessful()) {
                Log.d("ShopActivity", "Store purchase successfully recorded for special mission.");
            } else if (task.getException() != null) {
                // Grešku samo logujemo da ne bismo prekidali korisnika.
                Log.e("ShopActivity", "Failed to record store purchase for special mission.", task.getException());
            }
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