package com.example.forkit.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.forkit.MainActivity;
import com.example.forkit.R;
import com.example.forkit.models.UserGoals;
import com.example.forkit.utils.PrefsHelper;
import com.example.forkit.utils.SupabaseApi;
import com.example.forkit.utils.SupabaseClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileSetupActivity extends AppCompatActivity {

    private static final String TAG = "Supabase";

    private TextInputEditText etName, etAge, etHeight, etWeight, etGoalWeight, etCalGoal, etBurnGoal, etProtein, etCarbs, etFat;
    private MaterialButton btnSave;
    private SupabaseApi supabaseApi;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        SharedPreferences prefs = getSharedPreferences("forkit_prefs", MODE_PRIVATE);
        SupabaseClient.initApplicationContext(this);
        String accessToken = prefs.getString("access_token", null);
        if (accessToken != null) {
            SupabaseClient.setAccessToken(accessToken);
        }
        userId = prefs.getString("user_id", "");

        supabaseApi = SupabaseClient.getClient().create(SupabaseApi.class);

        etName = findViewById(R.id.et_name);
        etAge = findViewById(R.id.et_age);
        etHeight = findViewById(R.id.et_height);
        etWeight = findViewById(R.id.et_weight);
        etGoalWeight = findViewById(R.id.et_goal_weight);
        etCalGoal = findViewById(R.id.et_cal_goal);
        etBurnGoal = findViewById(R.id.et_burn_goal);
        etProtein = findViewById(R.id.et_protein);
        etCarbs = findViewById(R.id.et_carbs);
        etFat = findViewById(R.id.et_fat);
        btnSave = findViewById(R.id.btn_save);

        // Pre-fill with defaults
        etAge.setText("25");
        etHeight.setText("170");
        etWeight.setText("70");
        etGoalWeight.setText("70");
        etCalGoal.setText("2000");
        etBurnGoal.setText("500");
        etProtein.setText("150");
        etCarbs.setText("250");
        etFat.setText("65");
        findViewById(R.id.rb_male).performClick();
        findViewById(R.id.rb_light).performClick();

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String name;
        int age;
        float height, weight, goalWeight;
        int calGoal, burnGoal;
        float protein, carbs, fat;
        int activityLevel;
        boolean isMale;

        try {
            name = etName.getText() != null ? etName.getText().toString().trim() : "";
            age = Integer.parseInt(etAge.getText().toString().trim());
            height = Float.parseFloat(etHeight.getText().toString().trim());
            weight = Float.parseFloat(etWeight.getText().toString().trim());
            goalWeight = Float.parseFloat(etGoalWeight.getText().toString().trim());
            calGoal = Integer.parseInt(etCalGoal.getText().toString().trim());
            burnGoal = Integer.parseInt(etBurnGoal.getText().toString().trim());
            protein = Float.parseFloat(etProtein.getText().toString().trim());
            carbs = Float.parseFloat(etCarbs.getText().toString().trim());
            fat = Float.parseFloat(etFat.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please fill in all fields with valid numbers", Toast.LENGTH_SHORT).show();
            return;
        }

        int checkedGenderId = ((android.widget.RadioGroup) findViewById(R.id.rg_gender)).getCheckedRadioButtonId();
        isMale = checkedGenderId != R.id.rb_female;

        int checkedActivityId = ((android.widget.RadioGroup) findViewById(R.id.rg_activity)).getCheckedRadioButtonId();
        if (checkedActivityId == R.id.rb_sedentary) activityLevel = 0;
        else if (checkedActivityId == R.id.rb_light) activityLevel = 1;
        else if (checkedActivityId == R.id.rb_moderate) activityLevel = 2;
        else if (checkedActivityId == R.id.rb_active) activityLevel = 3;
        else activityLevel = 4;

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        UserGoals goals = new UserGoals();
        goals.setId(userId);
        goals.setUserName(name);
        goals.setDailyCalorieGoal(calGoal);
        goals.setDailyBurnGoal(burnGoal);
        goals.setProteinGoal(protein);
        goals.setCarbsGoal(carbs);
        goals.setFatGoal(fat);
        goals.setAge(age);
        goals.setHeightCm(height);
        goals.setWeightKg(weight);
        goals.setGoalWeightKg(goalWeight);
        goals.setActivityLevel(activityLevel);
        goals.setMale(isMale);

        // Persist personal details locally for immediate Settings/Profile usage.
        PrefsHelper prefsHelper = new PrefsHelper(this);
        prefsHelper.setUserName(name);
        prefsHelper.setAge(age);
        prefsHelper.setHeightCm(height);
        prefsHelper.setWeight(weight);
        prefsHelper.setGoalWeight(goalWeight);
        prefsHelper.setIsMale(isMale);
        prefsHelper.setActivityLevel(activityLevel);
        prefsHelper.setCalorieGoal(calGoal);
        prefsHelper.setProteinGoal(protein);
        prefsHelper.setCarbsGoal(carbs);
        prefsHelper.setFatGoal(fat);

        // also update the memory object
        HomeFragment.userGoals = goals;

        supabaseApi.insertUserGoals(goals).enqueue(new Callback<List<UserGoals>>() {
            @Override
            public void onResponse(@NonNull Call<List<UserGoals>> call, @NonNull Response<List<UserGoals>> response) {
                if (response.isSuccessful()) {
                    startActivity(new Intent(ProfileSetupActivity.this, MainActivity.class));
                    finish();
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        Log.e(TAG, "Profile save failed: " + response.code() + " - " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "Profile save failed: " + response.code());
                    }
                    runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save & Continue");
                        Toast.makeText(ProfileSetupActivity.this,
                                "Failed to save profile (code " + response.code() + ")",
                                Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<UserGoals>> call, @NonNull Throwable t) {
                Log.e(TAG, "Profile save network error: " + t.getMessage());
                runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save & Continue");
                    Toast.makeText(ProfileSetupActivity.this, "Network error, try again", Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}