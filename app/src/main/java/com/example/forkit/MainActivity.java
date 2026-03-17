package com.example.forkit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.forkit.activities.CameraFragment;
import com.example.forkit.activities.FoodLogFragment;
import com.example.forkit.activities.GoalsFragment;
import com.example.forkit.activities.HomeFragment;
import com.example.forkit.activities.LoginActivity;
import com.example.forkit.activities.RecommendationsFragment;
import com.example.forkit.activities.SearchFragment;
import com.example.forkit.activities.SettingsFragment;
import com.example.forkit.models.UserGoals;
import com.example.forkit.utils.PrefsHelper;
import com.example.forkit.utils.SupabaseApi;
import com.example.forkit.utils.SupabaseClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is logged in
        SharedPreferences prefs = getSharedPreferences("forkit_prefs", MODE_PRIVATE);
        String accessToken = prefs.getString("access_token", null);
        if (accessToken == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        SupabaseClient.setAccessToken(accessToken);
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

        findViewById(R.id.drawer_home).setOnClickListener(v -> {
            loadFragment(new HomeFragment());
            selectNavItem(R.id.nav_home);
            drawerLayout.closeDrawers();
        });
        findViewById(R.id.drawer_search).setOnClickListener(v -> {
            loadFragment(new SearchFragment());
            selectNavItem(R.id.nav_search);
            drawerLayout.closeDrawers();
        });
        findViewById(R.id.drawer_logs).setOnClickListener(v -> {
            loadFragment(new FoodLogFragment());
            selectNavItem(R.id.nav_log);
            drawerLayout.closeDrawers();
        });
        findViewById(R.id.drawer_settings).setOnClickListener(v -> {
            loadFragment(new SettingsFragment());
            selectNavItem(R.id.nav_profile);
            drawerLayout.closeDrawers();
        });
        findViewById(R.id.drawer_goals).setOnClickListener(v -> {
            loadFragment(new GoalsFragment());
            selectNavItem(R.id.nav_profile);
            drawerLayout.closeDrawers();
        });
        findViewById(R.id.drawer_recommendations).setOnClickListener(v -> {
            loadFragment(new RecommendationsFragment());
            selectNavItem(R.id.nav_home);
            drawerLayout.closeDrawers();
        });

        // Fetch user goals from Supabase and populate in-memory + PrefsHelper
        fetchUserGoals(prefs.getString("user_id", ""));
    }

    private void fetchUserGoals(String userId) {
        if (userId.isEmpty()) return;

        SupabaseApi api = SupabaseClient.getClient().create(SupabaseApi.class);
        api.getUserGoals("*").enqueue(new Callback<List<UserGoals>>() {
            @Override
            public void onResponse(Call<List<UserGoals>> call, Response<List<UserGoals>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    UserGoals goals = response.body().get(0);

                    // Update in-memory goals used by HomeFragment and SettingsFragment
                    HomeFragment.userGoals = goals;

                    // Sync into PrefsHelper so SettingsFragment dialogs show correct values
                    PrefsHelper p = new PrefsHelper(MainActivity.this);
                    if (goals.getUserName() != null) p.setUserName(goals.getUserName());
                    p.setCalorieGoal(goals.getDailyCalorieGoal());
                    p.setProteinGoal(goals.getProteinGoal());
                    p.setCarbsGoal(goals.getCarbsGoal());
                    p.setFatGoal(goals.getFatGoal());

                    // Refresh the current fragment if it's SettingsFragment
                    runOnUiThread(() -> {
                        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                        if (current instanceof SettingsFragment) {
                            loadFragment(new SettingsFragment());
                        }
                    });

                    Log.d("Supabase", "Goals loaded for user: " + userId);
                } else {
                    Log.w("Supabase", "No goals found for user: " + userId);
                }
            }

            @Override
            public void onFailure(Call<List<UserGoals>> call, Throwable t) {
                Log.e("Supabase", "Failed to fetch goals: " + t.getMessage());
            }
        });
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