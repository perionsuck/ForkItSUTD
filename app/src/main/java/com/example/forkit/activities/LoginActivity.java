package com.example.forkit.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.forkit.MainActivity;
import com.example.forkit.R;
import com.example.forkit.utils.PrefsHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {
    //    TODO: Don't hardcode your keys lmao
    private static final String TAG = "Supabase";
    private static final String SUPABASE_URL = "https://fbbfaymfxetiwgfkrkxw.supabase.co";
    private static final String[] USER_SCOPED_KEYS = new String[]{
            "user_name", "user_handle", "weight_kg", "age", "height_cm", "goal_weight_kg",
            "activity_level", "is_male", "steps_today", "exercise_mins",
            "water_goal_ml", "dietary_prefs", "profile_pic_uri",
            "reminders_enabled", "reminder_times",
            "calorie_goal", "protein_goal", "carbs_goal", "fat_goal",
            "calories_burned", "streak", "last_log_date"
    };

    public static final String ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZiYmZheW1meGV0aXdnZmtya3h3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzMwNTYxNTksImV4cCI6MjA4ODYzMjE1OX0.tIuYcHqTrmfxxiVPG8DFSETPf6TxK6Odwsvl9Ag1ukg";

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnSubmit;
    private TextView tvToggle, tvTitle, tvSubtitle;

    private boolean isLoginMode = true;
    private OkHttpClient httpClient = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnSubmit = findViewById(R.id.btn_submit);
        tvToggle = findViewById(R.id.tv_toggle);
        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);

        btnSubmit.setOnClickListener(v -> handleSubmit());
        tvToggle.setOnClickListener(v -> toggleMode());
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;
        if (isLoginMode) {
            tvTitle.setText("Let's Fork It!");
            tvSubtitle.setText("Log in to your ForkIt account");
            btnSubmit.setText("Log In");
            tvToggle.setText("Don't have an account? Sign Up");
        } else {
            tvTitle.setText("Create account");
            tvSubtitle.setText("Start tracking your nutrition");
            btnSubmit.setText("Sign Up");
            tvToggle.setText("Already have an account? Log In");
        }
    }

    private void handleSubmit() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Please wait...");

        if (isLoginMode) {
            login(email, password);
        } else {
            signUp(email, password);
        }
    }

    private void login(String email, String password) {
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);

            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/auth/v1/token?grant_type=password")
                    .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                    .addHeader("apikey", ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        handleAuthSuccess(responseBody, false);
                    } else {
                        Log.e(TAG, "Login failed: " + responseBody);
                        showError("Login failed. Check your email and password.");
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Login network error: " + e.getMessage());
                    showError("Network error, try again.");
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
        }
    }

    private void signUp(String email, String password) {
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);

            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/auth/v1/signup")
                    .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                    .addHeader("apikey", ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        handleAuthSuccess(responseBody, true);
                    } else {
                        Log.e(TAG, "Signup failed: " + responseBody);
                        showError("Sign up failed. Email may already be in use.");
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Signup network error: " + e.getMessage());
                    showError("Network error, try again.");
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
        }
    }

    private void handleAuthSuccess(String responseBody, boolean isNewUser) {
        Log.d("Supabase", "Auth response: " + responseBody);
        try {
            JSONObject json = new JSONObject(responseBody);
            String accessToken = json.getString("access_token");
            String userId = json.getJSONObject("user").getString("id");

            // Save token and user ID to SharedPreferences
            SharedPreferences prefs = getSharedPreferences("forkit_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            clearActiveProfileKeys(editor);
            editor.putString("access_token", accessToken);
            String refreshToken = json.optString("refresh_token", "");
            if (!refreshToken.isEmpty()) {
                editor.putString(PrefsHelper.KEY_REFRESH_TOKEN, refreshToken);
            }
            editor.putString("user_id", userId);
            editor.putBoolean("is_new_user", isNewUser);
            editor.apply();

            if (!isNewUser) {
                restoreUserScopedCache(prefs, userId);
            }

            runOnUiThread(() -> {
                if (isNewUser) {
                    // New user -> go to profile setup
                    startActivity(new Intent(LoginActivity.this, ProfileSetupActivity.class));
                } else {
                    // Existing user -> go straight to main app
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                }
                finish();
            });

        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse auth response: " + e.getMessage());
            showError("Something went wrong, try again.");
        }
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            btnSubmit.setEnabled(true);
            btnSubmit.setText(isLoginMode ? "Log In" : "Sign Up");
            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void clearActiveProfileKeys(SharedPreferences.Editor editor) {
        for (String key : USER_SCOPED_KEYS) editor.remove(key);
        for (int i = 0; i < 7; i++) editor.remove("water_" + i);
    }

    private void restoreUserScopedCache(SharedPreferences prefs, String userId) {
        if (userId == null || userId.isEmpty()) return;
        String prefix = "usercache_" + userId + "_";
        if (!prefs.getBoolean(prefix + "__exists", false)) return;

        SharedPreferences.Editor editor = prefs.edit();
        for (String key : USER_SCOPED_KEYS) {
            copyKeyFromScopedCache(prefs, editor, prefix + key, key);
        }
        for (int i = 0; i < 7; i++) {
            copyKeyFromScopedCache(prefs, editor, prefix + "water_" + i, "water_" + i);
        }
        editor.apply();
    }

    private void copyKeyFromScopedCache(SharedPreferences prefs, SharedPreferences.Editor editor, String fromKey, String toKey) {
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