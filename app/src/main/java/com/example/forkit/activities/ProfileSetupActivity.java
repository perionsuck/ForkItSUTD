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

    private TextInputEditText etName, etCalGoal, etBurnGoal, etProtein, etCarbs, etFat;
    private MaterialButton btnSave;
    private SupabaseApi supabaseApi;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        supabaseApi = SupabaseClient.getClient().create(SupabaseApi.class);

        SharedPreferences prefs = getSharedPreferences("forkit_prefs", MODE_PRIVATE);
        userId = prefs.getString("user_id", "");

        etName = findViewById(R.id.et_name);
        etCalGoal = findViewById(R.id.et_cal_goal);
        etBurnGoal = findViewById(R.id.et_burn_goal);
        etProtein = findViewById(R.id.et_protein);
        etCarbs = findViewById(R.id.et_carbs);
        etFat = findViewById(R.id.et_fat);
        btnSave = findViewById(R.id.btn_save);

        // Pre-fill with defaults
        etCalGoal.setText("2000");
        etBurnGoal.setText("500");
        etProtein.setText("150");
        etCarbs.setText("250");
        etFat.setText("65");

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String name;
        int calGoal, burnGoal;
        float protein, carbs, fat;

        try {
            name = etName.getText() != null ? etName.getText().toString().trim() : "";
            calGoal = Integer.parseInt(etCalGoal.getText().toString().trim());
            burnGoal = Integer.parseInt(etBurnGoal.getText().toString().trim());
            protein = Float.parseFloat(etProtein.getText().toString().trim());
            carbs = Float.parseFloat(etCarbs.getText().toString().trim());
            fat = Float.parseFloat(etFat.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please fill in all fields with valid numbers", Toast.LENGTH_SHORT).show();
            return;
        }

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

        // Also update the in-memory object
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