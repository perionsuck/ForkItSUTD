package com.example.forkit;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.forkit.activities.CameraFragment;
import com.example.forkit.activities.FoodLogFragment;
import com.example.forkit.activities.GoalsFragment;
import com.example.forkit.activities.HomeFragment;
import com.example.forkit.activities.RecommendationsFragment;
import com.example.forkit.activities.SearchFragment;
import com.example.forkit.activities.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        bottomNav = findViewById(R.id.bottom_navigation);

        loadFragment(new HomeFragment());
        bottomNav.setSelectedItemId(R.id.nav_home);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment f;
            if (id == R.id.nav_home) f = new HomeFragment();
            else if (id == R.id.nav_search) f = new SearchFragment();
            else if (id == R.id.nav_camera) f = new CameraFragment();
            else if (id == R.id.nav_log) f = new FoodLogFragment();
            else if (id == R.id.nav_profile) f = new SettingsFragment();
            else f = new HomeFragment();
            loadFragment(f);
            drawerLayout.closeDrawers();
            return true;
        });

        findViewById(R.id.drawer_home).setOnClickListener(v -> { loadFragment(new HomeFragment()); selectNavItem(R.id.nav_home); drawerLayout.closeDrawers(); });
        findViewById(R.id.drawer_search).setOnClickListener(v -> { loadFragment(new SearchFragment()); selectNavItem(R.id.nav_search); drawerLayout.closeDrawers(); });
        findViewById(R.id.drawer_logs).setOnClickListener(v -> { loadFragment(new FoodLogFragment()); selectNavItem(R.id.nav_log); drawerLayout.closeDrawers(); });
        findViewById(R.id.drawer_settings).setOnClickListener(v -> { loadFragment(new SettingsFragment()); selectNavItem(R.id.nav_profile); drawerLayout.closeDrawers(); });
        findViewById(R.id.drawer_goals).setOnClickListener(v -> { loadFragment(new GoalsFragment()); selectNavItem(R.id.nav_profile); drawerLayout.closeDrawers(); });
        findViewById(R.id.drawer_recommendations).setOnClickListener(v -> { loadFragment(new RecommendationsFragment()); selectNavItem(R.id.nav_home); drawerLayout.closeDrawers(); });
    }

    public void openDrawer() {
        drawerLayout.openDrawer(findViewById(R.id.drawer_content));
    }

    public void loadFragment(Fragment f) {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, f).commit();
    }

    public void selectNavItem(int id) {
        bottomNav.setSelectedItemId(id);
    }
}
