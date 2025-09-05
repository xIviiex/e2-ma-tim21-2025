package com.team21.questify.presentation.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.widget.SearchView;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.team21.questify.R;
import com.team21.questify.application.model.User;
import com.team21.questify.application.service.AllianceService;
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
    private Button createAllianceButton;
    private AllianceService allianceService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        userService = new UserService(this);
        allianceService = new AllianceService(this);
        sharedPrefs = new SharedPrefs(this);
        currentUserId = sharedPrefs.getUserUid();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        createAllianceButton = findViewById(R.id.create_alliance_button);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUserAllianceStatus();
        if (searchView.getQuery().toString().isEmpty()) {
            loadFriendsList();
        }
    }
    private void checkUserAllianceStatus() {
        String userId = sharedPrefs.getUserUid();
        if (userId != null) {
            userService.checkUserAllianceStatus(userId)
                    .addOnSuccessListener(status -> {
                        boolean isInAlliance = status.first;
                        String currentAllianceId = status.second;

                        if (isInAlliance) {
                            createAllianceButton.setText("My Alliance");
                            createAllianceButton.setOnClickListener(v -> {
                                Intent intent = new Intent(FriendsActivity.this, AllianceActivity.class);
                                intent.putExtra("allianceId", currentAllianceId);
                                startActivity(intent);
                            });
                        } else {
                            createAllianceButton.setText("Create Alliance");
                            createAllianceButton.setOnClickListener(v -> showCreateAllianceDialog());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load user status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        createAllianceButton.setVisibility(View.GONE);
                    });
        } else {
            createAllianceButton.setVisibility(View.GONE);
        }
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
        userAdapter = new UsersAdapter(new ArrayList<>(), new ArrayList<>(), this, currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(userAdapter);
    }

    private void showCreateAllianceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_create_alliance, null);
        builder.setView(dialogView);

        EditText allianceNameInput = dialogView.findViewById(R.id.alliance_name_input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String allianceName = allianceNameInput.getText().toString().trim();
            if (TextUtils.isEmpty(allianceName)) {
                Toast.makeText(this, "Alliance name cannot be empty.", Toast.LENGTH_SHORT).show();
            } else {
                createAlliance(allianceName);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void createAlliance(String allianceName) {
        String userId = sharedPrefs.getUserUid();
        if (userId != null) {
            allianceService.createAlliance(allianceName, userId)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(FriendsActivity.this, "Alliance created successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(FriendsActivity.this, AllianceActivity.class);
                        startActivity(intent);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(FriendsActivity.this, "Failed to create alliance: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
        }
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
        userService.fetchUserProfile(currentUserId)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    User currentUser = task.getResult();
                    List<String> friendsIds = currentUser.getFriendsIds();
                    if (friendsIds == null || friendsIds.isEmpty()) {
                        return Tasks.forResult(new ArrayList<User>());
                    }
                    return userService.getUsersByIds(friendsIds);
                })
                .addOnSuccessListener(friends -> {
                    List<String> friendsIds = new ArrayList<>();
                    for(User friend : friends) {
                        friendsIds.add(friend.getUserId());
                    }
                    userAdapter.updateLists(friends, friendsIds);
                    updateUIForFriendsList(friends);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load friends: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    updateUIForFriendsList(new ArrayList<>());
                });
    }

    private void updateUIForFriendsList(List<User> friends) {
        if (friends.isEmpty()) {
            tvNoFriends.setVisibility(View.VISIBLE);
            tvNoFriends.setText(R.string.no_friends_message);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoFriends.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        tvFriendsTitle.setVisibility(View.VISIBLE);
    }

    private void searchUsers(String query) {
        userService.fetchUserProfile(currentUserId)
                .continueWithTask(userTask -> {
                    if (!userTask.isSuccessful()) {
                        Log.e("FriendsActivity", "Could not fetch friends list for search.", userTask.getException());
                        return userService.searchUsers(query).continueWith(searchTask -> new Pair<>(new ArrayList<String>(), searchTask.getResult()));
                    }
                    List<String> friendsIds = userTask.getResult().getFriendsIds();

                    return userService.searchUsers(query).continueWith(searchTask -> {
                        if (!searchTask.isSuccessful()) throw searchTask.getException();
                        return new Pair<>(friendsIds, searchTask.getResult());
                    });
                })
                .addOnSuccessListener(pair -> {
                    List<String> friendsIds = pair.first;
                    List<User> searchResults = pair.second;

                    userAdapter.updateLists(searchResults, friendsIds);

                    if (searchResults.isEmpty()) {
                        tvNoFriends.setVisibility(View.VISIBLE);
                        tvNoFriends.setText(R.string.no_users_found_matching_your_search);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        tvNoFriends.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                    tvFriendsTitle.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    tvNoFriends.setVisibility(View.VISIBLE);
                    tvNoFriends.setText(R.string.search_failed);
                    recyclerView.setVisibility(View.GONE);
                    tvFriendsTitle.setVisibility(View.GONE);
                });
    }

    @Override
    public void onAddFriendClick(User user) {
        performAddFriend(user.getUserId());
    }

    @Override
    public void onRemoveFriendClick(User user) {
        performRemoveFriend(user.getUserId());
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
        userService.addFriendship(currentUserId, friendUserId)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "You are now friends!", Toast.LENGTH_SHORT).show();
                    updateUiAfterFriendAdded(friendUserId);
                })
                .addOnFailureListener(e -> {
                    String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error.";
                    if (errorMessage.contains("already a friend")) {
                        Toast.makeText(this, "User is already a friend.", Toast.LENGTH_SHORT).show();
                        updateUiAfterFriendAdded(friendUserId);
                    } else {
                        Toast.makeText(this, "Failed to add friend: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUiAfterFriendAdded(String friendId) {
        List<String> currentFriends = userAdapter.getFriendsIds();
        if (!currentFriends.contains(friendId)) {
            currentFriends.add(friendId);
            userAdapter.setFriendsIds(currentFriends);
            userAdapter.notifyDataSetChanged();
        }
    }

    private void performRemoveFriend(String friendUserId) {
        userService.removeFriendship(currentUserId, friendUserId)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Friend successfully removed.", Toast.LENGTH_SHORT).show();
                    String currentQuery = searchView.getQuery().toString();
                    if (currentQuery.isEmpty()) {
                        loadFriendsList();
                    } else {
                        searchUsers(currentQuery);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to remove friend.", Toast.LENGTH_SHORT).show();
                });
    }
}
