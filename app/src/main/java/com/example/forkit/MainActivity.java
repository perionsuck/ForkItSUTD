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
import com.example.forkit.models.FoodEntry;
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

    private static final String[] USER_SCOPED_KEYS = new String[]{
            "user_name", "user_handle", "weight_kg", "age", "height_cm", "goal_weight_kg",
            "activity_level", "is_male", "steps_today", "exercise_mins",
            "water_goal_ml", "dietary_prefs", "profile_pic_uri",
            "reminders_enabled", "reminder_times",
            "calorie_goal", "protein_goal", "carbs_goal", "fat_goal",
            "calories_burned", "streak", "last_log_date"
    };

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

        SupabaseClient.initApplicationContext(this);
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
        findViewById(R.id.drawer_logout).setOnClickListener(v -> logout());

        // Fetch user goals from Supabase and populate in-memory + PrefsHelper
        String userId = prefs.getString("user_id", "");
        fetchUserGoals(userId);
        fetchFoodEntries(userId);
    }

    private void fetchUserGoals(String userId) {
        if (userId.isEmpty()) return;

        SupabaseApi api = SupabaseClient.getClient().create(SupabaseApi.class);
        api.getUserGoals("*", "eq." + userId).enqueue(new Callback<List<UserGoals>>() {
            @Override
            public void onResponse(Call<List<UserGoals>> call, Response<List<UserGoals>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    UserGoals goals = response.body().get(0);

                    // Update in-memory goals used by HomeFragment and SettingsFragment
                    HomeFragment.userGoals = goals;

                    // Sync into PrefsHelper so SettingsFragment dialogs show correct values
                    PrefsHelper p = new PrefsHelper(MainActivity.this);
                    if (goals.getUserName() != null) p.setUserName(goals.getUserName());
                    if (goals.getUserHandle() != null) p.setUserHandle(goals.getUserHandle());
                    p.setCalorieGoal(goals.getDailyCalorieGoal());
                    p.setProteinGoal(goals.getProteinGoal());
                    p.setCarbsGoal(goals.getCarbsGoal());
                    p.setFatGoal(goals.getFatGoal());
                    if (goals.getAge() > 0) p.setAge(goals.getAge());
                    if (goals.getHeightCm() > 0) p.setHeightCm(goals.getHeightCm());
                    if (goals.getWeightKg() > 0) p.setWeight(goals.getWeightKg());
                    if (goals.getGoalWeightKg() > 0) p.setGoalWeight(goals.getGoalWeightKg());
                    p.setActivityLevel(goals.getActivityLevel());
                    p.setIsMale(goals.isMale());

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

    private void fetchFoodEntries(String userId) {
        if (userId == null || userId.isEmpty()) return;

        SupabaseApi api = SupabaseClient.getClient().create(SupabaseApi.class);
        api.getFoodEntries("timestamp.desc", "*", "eq." + userId).enqueue(new Callback<List<FoodEntry>>() {
            @Override
            public void onResponse(Call<List<FoodEntry>> call, Response<List<FoodEntry>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    HomeFragment.foodEntries.clear();
                    HomeFragment.foodEntries.addAll(response.body());
                    Log.d("Supabase", "Food entries loaded: " + response.body().size());

                    runOnUiThread(() -> {
                        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                        if (current instanceof HomeFragment) {
                            ((HomeFragment) current).updateDashboard();
                        } else if (current instanceof FoodLogFragment) {
                            ((FoodLogFragment) current).refreshList();
                        }
                    });
                } else {
                    Log.w("Supabase", "Failed to load food entries: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<FoodEntry>> call, Throwable t) {
                Log.e("Supabase", "Failed to fetch food entries: " + t.getMessage());
            }
        });
    }

    /**
     * Reload food entries from Supabase after a local insert (or when UI may be stale).
     */
    public void refreshFoodEntriesFromCloud() {
        SharedPreferences prefs = getSharedPreferences("forkit_prefs", MODE_PRIVATE);
        fetchFoodEntries(prefs.getString("user_id", ""));
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

    private void logout() {
        // Clear in-memory session data to prevent carryover after relogin.
        HomeFragment.foodEntries.clear();
        HomeFragment.userGoals = new UserGoals();

        SharedPreferences prefs = getSharedPreferences("forkit_prefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        saveUserScopedCache(prefs, userId);

        SharedPreferences.Editor editor = prefs.edit();
        clearActiveProfileKeys(editor);
        editor
                .remove("access_token")
                .remove(PrefsHelper.KEY_REFRESH_TOKEN)
                .remove("user_id")
                .remove("is_new_user")
                .apply();

        SupabaseClient.setAccessToken(SupabaseClient.ANON_KEY);

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void saveUserScopedCache(SharedPreferences prefs, String userId) {
        if (userId == null || userId.isEmpty()) return;
        SharedPreferences.Editor editor = prefs.edit();
        String prefix = "usercache_" + userId + "_";
        for (String key : USER_SCOPED_KEYS) {
            copyKeyToScopedCache(prefs, editor, key, prefix + key);
        }
        for (int i = 0; i < 7; i++) {
            copyKeyToScopedCache(prefs, editor, "water_" + i, prefix + "water_" + i);
        }
        editor.putBoolean(prefix + "__exists", true);
        editor.apply();
    }

    private void clearActiveProfileKeys(SharedPreferences.Editor editor) {
        for (String key : USER_SCOPED_KEYS) editor.remove(key);
        for (int i = 0; i < 7; i++) editor.remove("water_" + i);
    }

    private void copyKeyToScopedCache(SharedPreferences prefs, SharedPreferences.Editor editor, String fromKey, String toKey) {
        Object value = prefs.getAll().get(fromKey);
        if (value == null) {
            editor.remove(toKey);
        } else if (value instanceof String) {
            editor.putString(toKey, (String) value);
        } else if (value instanceof Integer) {
            editor.putInt(toKey, (Integer) value);
        } else if (value instanceof Float) {
            editor.putFloat(toKey, (Float) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(toKey, (Boolean) value);
        } else if (value instanceof Long) {
            editor.putLong(toKey, (Long) value);
        } else {
            editor.putString(toKey, String.valueOf(value));
        }
    }
}