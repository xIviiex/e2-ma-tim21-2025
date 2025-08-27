package com.team21.questify.presentation.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.widget.SearchView;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.team21.questify.R;
import com.team21.questify.application.model.User;
import com.team21.questify.application.service.UserService;
import com.team21.questify.presentation.adapter.UsersAdapter;
import com.team21.questify.utils.SharedPrefs;

import java.util.ArrayList;
import java.util.List;

public class FriendsActivity extends AppCompatActivity implements UsersAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private UsersAdapter userAdapter;
    private UserService userService;
    private SharedPrefs sharedPrefs;
    private SearchView searchView;
    private TextView tvNoFriends;
    private TextView tvFriendsTitle;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        userService = new UserService(this);
        sharedPrefs = new SharedPrefs(this);
        currentUserId = sharedPrefs.getUserUid();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rv_friends_list);
        searchView = findViewById(R.id.sv_user_search);
        tvNoFriends = findViewById(R.id.tv_no_friends);
        tvFriendsTitle = findViewById(R.id.tv_friends_title);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void setupRecyclerView() {
        userAdapter = new UsersAdapter(new ArrayList<>(), this, sharedPrefs, userService);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(userAdapter);
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                tvFriendsTitle.setVisibility(View.GONE);
                searchUsers(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    tvFriendsTitle.setVisibility(View.VISIBLE);
                    loadFriendsList();
                } else {
                    tvFriendsTitle.setVisibility(View.GONE);
                    searchUsers(newText);
                }
                return true;
            }
        });
        loadFriendsList();
    }

    private void loadFriendsList() {
        userService.fetchAllUsers(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<User> allUsers = task.getResult();
                List<User> friends = new ArrayList<>();
                List<String> friendIds = userService.getUserById(currentUserId).getFriendsIds();
                if (friendIds != null) {
                    for (User user : allUsers) {
                        if (friendIds.contains(user.getUserId())) {
                            friends.add(user);
                        }
                    }
                }
                userAdapter.updateList(friends);
                if (friends.isEmpty()) {
                    tvNoFriends.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    tvNoFriends.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
                tvFriendsTitle.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "Failed to load friends.", Toast.LENGTH_SHORT).show();
                tvNoFriends.setVisibility(View.VISIBLE);
                tvNoFriends.setText(R.string.failed_to_load_friends);
                recyclerView.setVisibility(View.GONE);
                tvFriendsTitle.setVisibility(View.GONE);
            }
        });
    }

    private void searchUsers(String query) {
        userService.searchUsers(query, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<User> searchResults = task.getResult();
                userAdapter.updateList(searchResults);
                if (searchResults.isEmpty()) {
                    tvNoFriends.setVisibility(View.VISIBLE);
                    tvNoFriends.setText(R.string.no_users_found_matching_your_search);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    tvNoFriends.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
                tvFriendsTitle.setVisibility(View.GONE);
            } else {
                Toast.makeText(this, "Search failed.", Toast.LENGTH_SHORT).show();
                tvNoFriends.setVisibility(View.VISIBLE);
                tvNoFriends.setText(R.string.search_failed);
                recyclerView.setVisibility(View.GONE);
                tvFriendsTitle.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onAddFriendClick(User user) {
        performAddFriend(user.getUserId());
    }

    @Override
    public void onUserClick(User user) {
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra("user_id", user.getUserId());
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_friends, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_qr_scan) {
            startQrCodeScan();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startQrCodeScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan a user's QR code");
        integrator.setCameraId(0);
        integrator.setOrientationLocked(true);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            String scannedUserId = result.getContents();
            if (scannedUserId != null && !scannedUserId.isEmpty()) {
                performAddFriend(scannedUserId);
            } else {
                Toast.makeText(this, "QR code scan failed or invalid.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "QR code scan canceled.", Toast.LENGTH_SHORT).show();
        }
    }

    private void performAddFriend(String friendUserId) {
        if (friendUserId.equals(currentUserId)) {
            Toast.makeText(this, "You can't add yourself as a friend.", Toast.LENGTH_SHORT).show();
            return;
        }

        userService.addFriend(currentUserId, friendUserId, task -> {
            if (task.isSuccessful()) {
                userService.addFriend(friendUserId, currentUserId, reverseTask -> {
                    if (reverseTask.isSuccessful()) {
                        Toast.makeText(this, "Friend added successfully via QR code!", Toast.LENGTH_SHORT).show();
                        loadFriendsList();
                    } else {
                        Toast.makeText(this, "Failed to complete friendship.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error.";

                if (errorMessage.contains("already a friend")) {
                    Toast.makeText(this, "User is already a friend.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to add friend: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}