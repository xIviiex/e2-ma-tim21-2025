package com.team21.questify.presentation.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.team21.questify.R;
import com.team21.questify.presentation.fragment.ViewTasksCalendarFragment;
import com.team21.questify.presentation.fragment.ViewTasksListFragment;

public class ViewTasksActivity extends AppCompatActivity {

    private Spinner viewSwitchSpinner;
    private Button btnViewCategories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_tasks);

        btnViewCategories = findViewById(R.id.btnViewCategories);

        btnViewCategories.setOnClickListener(v -> {

            Intent intent = new Intent(ViewTasksActivity.this, TaskCategoryViewActivity.class);
            startActivity(intent);
        });


        viewSwitchSpinner = findViewById(R.id.viewSwitchSpinner);

        viewSwitchSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Fragment fragmentToLoad;
                switch (position) {
                    case 0:
                        fragmentToLoad = new ViewTasksCalendarFragment();
                        break;
                    case 1:
                        fragmentToLoad = new ViewTasksListFragment();
                        break;
                    default:
                        fragmentToLoad = new ViewTasksCalendarFragment();
                }
                loadFragment(fragmentToLoad);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });


        if (savedInstanceState == null) {
            loadFragment(new ViewTasksCalendarFragment());
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_view, fragment)
                .commit();
    }
}
