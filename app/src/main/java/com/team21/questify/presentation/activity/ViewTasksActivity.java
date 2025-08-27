package com.team21.questify.presentation.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.team21.questify.R;
import com.team21.questify.presentation.fragment.ViewTasksCalendarFragment;
import com.team21.questify.presentation.fragment.ViewTasksListFragment;

public class ViewTasksActivity extends AppCompatActivity {

    private Spinner viewSwitchSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_tasks);

        viewSwitchSpinner = findViewById(R.id.viewSwitchSpinner);

        viewSwitchSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Fragment fragmentToLoad;
                switch (position) {
                    case 0: // Kalendarski prikaz
                        fragmentToLoad = new ViewTasksCalendarFragment();
                        break;
                    case 1: // Lista zadataka
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

        // Učitaj default fragment
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
