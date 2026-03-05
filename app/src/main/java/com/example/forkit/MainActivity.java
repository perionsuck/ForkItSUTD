package com.example.forkit;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.forkit.activities.HomeFragment;
import com.example.forkit.activities.FoodLogFragment;
import com.example.forkit.activities.CameraFragment;
import com.example.forkit.activities.GoalsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        loadFragment(new HomeFragment());

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (id == R.id.nav_log) {
                fragment = new FoodLogFragment();
            } else if (id == R.id.nav_camera) {
                fragment = new CameraFragment();
            } else if (id == R.id.nav_goals) {
                fragment = new GoalsFragment();
            } else {
                fragment = new HomeFragment();
            }

            loadFragment(fragment);
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}