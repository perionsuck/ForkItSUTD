package com.example.forkit.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.forkit.MainActivity;
import com.example.forkit.R;
import com.example.forkit.models.SavedFood;
import com.example.forkit.models.UserGoals;
import com.example.forkit.utils.CalorieCalculator;
import com.example.forkit.utils.PrefsHelper;
import com.example.forkit.utils.SupabaseApi;
import com.example.forkit.utils.SupabaseClient;
import com.example.forkit.utils.FoodStore;
import com.example.forkit.utils.ReminderScheduler;
import com.example.forkit.utils.HealthConnectBridge;
import com.example.forkit.utils.SavedFoodsStore;

import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {

    private PrefsHelper p;
    private ImageView ivProfilePic;
    private boolean pendingEnableReminders;
    private AlertDialog deviceSyncDialog;
    private TextView deviceSyncStatus;
    private AlertDialog savedFoodsDialog;
    private AlertDialog privacyDialog;
    private AlertDialog historyDialog;

    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && p != null) {
                    p.setProfilePicUri(uri.toString());
                    if (ivProfilePic != null) ivProfilePic.setImageURI(uri);
                }
            });
    private final ActivityResultLauncher<String> requestNotificationsPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (pendingEnableReminders) {
                    pendingEnableReminders = false;
                    // Apply schedules now that permission flow completed
                    ReminderScheduler.apply(requireContext(), p.getRemindersEnabled(), p.getReminderTimes());
                    Toast.makeText(requireContext(), granted ? "Reminders enabled" : "Notifications permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Set<String>> requestHealthConnectPermissions =
            registerForActivityResult(
                    androidx.health.connect.client.PermissionController.createRequestPermissionResultContract(),
                    granted -> {
                        // After permissions, try syncing once.
                        syncFromHealthConnect();
                    }
            );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        p = new PrefsHelper(requireContext());
        ivProfilePic = view.findViewById(R.id.iv_profile_pic);
        refreshStats(view);
        loadProfilePic();

        view.findViewById(R.id.profile_pic_container).setOnClickListener(v -> pickImage.launch("image/*"));

        view.findViewById(R.id.btn_settings_menu).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).openDrawer();
        });

        view.findViewById(R.id.settings_personal).setOnClickListener(v -> showPersonalDetailsDialog());
        view.findViewById(R.id.settings_fitness).setOnClickListener(v -> showMacroGoalsDialog());
        view.findViewById(R.id.settings_dietary).setOnClickListener(v -> showDietaryDialog());
        view.findViewById(R.id.settings_saved).setOnClickListener(v -> {
            showSavedFoodsDialog();
        });
        view.findViewById(R.id.settings_progress).setOnClickListener(v -> Toast.makeText(requireContext(), "Progress - coming soon", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.settings_reminders).setOnClickListener(v -> showRemindersDialog());
        view.findViewById(R.id.settings_sync).setOnClickListener(v -> {
            showDeviceSyncDialog();
        });
        view.findViewById(R.id.settings_privacy).setOnClickListener(v -> {
            showPrivacyDialog();
        });
        view.findViewById(R.id.settings_history).setOnClickListener(v -> {
            showHistoryDialog();
        });
    }

    private void loadProfilePic() {
        String uri = p != null ? p.getProfilePicUri() : "";
        if (ivProfilePic != null && uri != null && !uri.isEmpty()) {
            try {
                ivProfilePic.setImageURI(Uri.parse(uri));
            } catch (Exception ignored) {}
        }
    }

    private void refreshStats(View view) {
        if (p == null) return;
        UserGoals g = HomeFragment.userGoals;
        g.setDailyBurnGoal(p.getCaloriesBurned());
        String name = p.getUserName().isEmpty() ? "User" : p.getUserName();
        String h = p.getUserHandle();
        String handle = (h == null || h.isEmpty()) ? "@user" : (h.startsWith("@") ? h : "@" + h);
        int totalCal = 0;
        for (com.example.forkit.models.FoodEntry e : FoodStore.getEntriesView()) totalCal += e.getCalories();
        int burned = g.getDailyBurnGoal();
        int remaining = g.getDailyCalorieGoal() - totalCal + burned;

        ((TextView) view.findViewById(R.id.tv_settings_name)).setText(name);
        ((TextView) view.findViewById(R.id.tv_settings_handle)).setText(handle);
        ((TextView) view.findViewById(R.id.tv_settings_goal)).setText("Goal: " + g.getDailyCalorieGoal() + " kcal/day");

        TextView tvAge = view.findViewById(R.id.tv_profile_age);
        if (tvAge != null) tvAge.setText(p.hasAge() ? ("Age " + p.getAge()) : "Age —");

        TextView tvHeight = view.findViewById(R.id.tv_profile_height);
        if (tvHeight != null) tvHeight.setText(p.hasHeightCm() ? ("Height " + String.format("%.0f cm", p.getHeightCm())) : "Height —");

        TextView tvWeight = view.findViewById(R.id.tv_profile_weight);
        if (tvWeight != null) tvWeight.setText(p.hasWeight() ? ("Weight " + String.format("%.1f kg", p.getWeight())) : "Weight —");
    }

    private void showPersonalDetailsDialog() {
        Context ctx = new ContextThemeWrapper(requireContext(), R.style.AlertDialogLight);
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_personal_details, null);
        ((EditText) v.findViewById(R.id.et_name)).setText(p.getUserName());
        ((EditText) v.findViewById(R.id.et_handle)).setText(p.getUserHandle());
        ((EditText) v.findViewById(R.id.et_age)).setText(p.hasAge() ? String.valueOf(p.getAge()) : "");
        ((EditText) v.findViewById(R.id.et_height)).setText(p.hasHeightCm() ? String.valueOf(p.getHeightCm()) : "");
        ((EditText) v.findViewById(R.id.et_weight)).setText(p.hasWeight() ? String.valueOf(p.getWeight()) : "");
        ((EditText) v.findViewById(R.id.et_goal_weight)).setText(p.hasGoalWeight() ? String.valueOf(p.getGoalWeight()) : "");

        if (p.hasGender()) {
            ((RadioButton) v.findViewById(p.isMale() ? R.id.rb_male : R.id.rb_female)).setChecked(true);
        } else {
            ((RadioGroup) v.findViewById(R.id.rg_gender)).clearCheck();
        }
        if (p.hasActivityLevel()) {
            int act = p.getActivityLevel();
            int actId = act == 0 ? R.id.rb_sedentary : act == 1 ? R.id.rb_light : act == 2 ? R.id.rb_moderate : act == 3 ? R.id.rb_active : R.id.rb_extra;
            ((RadioButton) v.findViewById(actId)).setChecked(true);
        } else {
            ((RadioGroup) v.findViewById(R.id.rg_activity)).clearCheck();
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.AlertDialogLight).setView(v).create();
        v.findViewById(R.id.btn_save_personal).setOnClickListener(x -> {
            p.setUserName(getText(v, R.id.et_name));
            String handleVal = getText(v, R.id.et_handle);
            if (!handleVal.isEmpty() && handleVal.startsWith("@")) handleVal = handleVal.substring(1).trim();
            p.setUserHandle(handleVal);
            try { p.setAge(Integer.parseInt(getText(v, R.id.et_age))); } catch (Exception e) {}
            try { p.setHeightCm(Float.parseFloat(getText(v, R.id.et_height))); } catch (Exception e) {}
            try { p.setWeight(Float.parseFloat(getText(v, R.id.et_weight))); } catch (Exception e) {}
            try { p.setGoalWeight(Float.parseFloat(getText(v, R.id.et_goal_weight))); } catch (Exception e) {}
            p.setIsMale(((RadioButton) v.findViewById(R.id.rb_male)).isChecked());
            int actLevel = 1;
            if (((RadioButton) v.findViewById(R.id.rb_sedentary)).isChecked()) actLevel = 0;
            else if (((RadioButton) v.findViewById(R.id.rb_light)).isChecked()) actLevel = 1;
            else if (((RadioButton) v.findViewById(R.id.rb_moderate)).isChecked()) actLevel = 2;
            else if (((RadioButton) v.findViewById(R.id.rb_active)).isChecked()) actLevel = 3;
            else if (((RadioButton) v.findViewById(R.id.rb_extra)).isChecked()) actLevel = 4;
            p.setActivityLevel(actLevel);

            int calGoal = CalorieCalculator.calculateDailyCalories(p.isMale(), p.getWeight(), p.getHeightCm(), p.getAge(), actLevel);
            p.setCalorieGoal(calGoal);
            HomeFragment.userGoals.setUserName(p.getUserName());
            HomeFragment.userGoals.setUserHandle(p.getUserHandle());
            HomeFragment.userGoals.setAge(p.getAge());
            HomeFragment.userGoals.setHeightCm(p.getHeightCm());
            HomeFragment.userGoals.setWeightKg(p.getWeight());
            HomeFragment.userGoals.setGoalWeightKg(p.getGoalWeight());
            HomeFragment.userGoals.setMale(p.isMale());
            HomeFragment.userGoals.setActivityLevel(actLevel);
            HomeFragment.userGoals.setDailyCalorieGoal(calGoal);

            String userId = p.getUserId();
            if (userId != null && !userId.isEmpty()) {
                HomeFragment.userGoals.setId(userId);
                SupabaseApi api = SupabaseClient.getClient().create(SupabaseApi.class);
                api.updateUserGoals("eq." + userId, HomeFragment.userGoals).enqueue(new Callback<List<UserGoals>>() {
                    @Override
                    public void onResponse(Call<List<UserGoals>> call, Response<List<UserGoals>> response) {}
                    @Override
                    public void onFailure(Call<List<UserGoals>> call, Throwable t) {}
                });
            }

            Toast.makeText(requireContext(), "Saved. Calorie goal: " + calGoal + " kcal/day", Toast.LENGTH_SHORT).show();
            View fv = getView();
            if (fv != null) refreshStats(fv);
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showMacroGoalsDialog() {
        Context ctx = new ContextThemeWrapper(requireContext(), R.style.AlertDialogLight);
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_macro_goals, null);
        ((EditText) v.findViewById(R.id.et_protein)).setText(String.valueOf((int) p.getProteinGoal()));
        ((EditText) v.findViewById(R.id.et_carbs)).setText(String.valueOf((int) p.getCarbsGoal()));
        ((EditText) v.findViewById(R.id.et_fat)).setText(String.valueOf((int) p.getFatGoal()));

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.AlertDialogLight).setView(v).create();
        v.findViewById(R.id.btn_save_macros).setOnClickListener(x -> {
            try {
                float protein = Float.parseFloat(getText(v, R.id.et_protein));
                float carbs = Float.parseFloat(getText(v, R.id.et_carbs));
                float fat = Float.parseFloat(getText(v, R.id.et_fat));
                p.setProteinGoal(protein);
                p.setCarbsGoal(carbs);
                p.setFatGoal(fat);
                HomeFragment.userGoals.setProteinGoal(protein);
                HomeFragment.userGoals.setCarbsGoal(carbs);
                HomeFragment.userGoals.setFatGoal(fat);
                Toast.makeText(requireContext(), "Macro goals saved", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Enter valid numbers", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showDietaryDialog() {
        Context ctx = new ContextThemeWrapper(requireContext(), R.style.AlertDialogLight);
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_dietary, null);
        String prefs = p.getDietaryPrefs();
        if (prefs != null && !prefs.isEmpty()) {
            String[] parts = prefs.split(",");
            StringBuilder extra = new StringBuilder();
            for (String s : parts) {
                String t = s.trim().toLowerCase();
                if (t.equals("vegetarian")) ((android.widget.CheckBox) v.findViewById(R.id.cb_vegetarian)).setChecked(true);
                else if (t.equals("vegan")) ((android.widget.CheckBox) v.findViewById(R.id.cb_vegan)).setChecked(true);
                else if (t.equals("gluten-free") || t.equals("glutenfree")) ((android.widget.CheckBox) v.findViewById(R.id.cb_gluten_free)).setChecked(true);
                else if (t.equals("low carb") || t.equals("lowcarb")) ((android.widget.CheckBox) v.findViewById(R.id.cb_low_carb)).setChecked(true);
                else if (t.equals("dairy-free") || t.equals("dairyfree")) ((android.widget.CheckBox) v.findViewById(R.id.cb_dairy_free)).setChecked(true);
                else if (!t.isEmpty()) { if (extra.length() > 0) extra.append(", "); extra.append(s.trim()); }
            }
            if (extra.length() > 0) ((EditText) v.findViewById(R.id.et_dietary_extra)).setText(extra.toString());
        }
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.AlertDialogLight).setView(v).create();
        v.findViewById(R.id.btn_save_dietary).setOnClickListener(x -> {
            StringBuilder sb = new StringBuilder();
            if (((android.widget.CheckBox) v.findViewById(R.id.cb_vegetarian)).isChecked()) sb.append("Vegetarian,");
            if (((android.widget.CheckBox) v.findViewById(R.id.cb_vegan)).isChecked()) sb.append("Vegan,");
            if (((android.widget.CheckBox) v.findViewById(R.id.cb_gluten_free)).isChecked()) sb.append("Gluten-free,");
            if (((android.widget.CheckBox) v.findViewById(R.id.cb_low_carb)).isChecked()) sb.append("Low carb,");
            if (((android.widget.CheckBox) v.findViewById(R.id.cb_dairy_free)).isChecked()) sb.append("Dairy-free,");
            String extra = getText(v, R.id.et_dietary_extra);
            if (!extra.isEmpty()) sb.append(extra);
            p.setDietaryPrefs(sb.length() > 0 ? sb.toString().replaceAll(",$", "") : "");
            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showRemindersDialog() {
        Context ctx = new ContextThemeWrapper(requireContext(), R.style.AlertDialogLight);
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_reminders, null);
        ((com.google.android.material.switchmaterial.SwitchMaterial) v.findViewById(R.id.switch_reminders)).setChecked(p.getRemindersEnabled());
        ((EditText) v.findViewById(R.id.et_reminder_times)).setText(p.getReminderTimes());
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.AlertDialogLight).setView(v).create();
        v.findViewById(R.id.btn_save_reminders).setOnClickListener(x -> {
            boolean enabled = ((com.google.android.material.switchmaterial.SwitchMaterial) v.findViewById(R.id.switch_reminders)).isChecked();
            p.setRemindersEnabled(enabled);
            String normalized = ReminderScheduler.normalizeTimesCsv(getText(v, R.id.et_reminder_times));
            p.setReminderTimes(normalized);

            if (enabled && Build.VERSION.SDK_INT >= 33) {
                pendingEnableReminders = true;
                requestNotificationsPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            } else {
                ReminderScheduler.apply(requireContext(), enabled, normalized);
            }
            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showDeviceSyncDialog() {
        Context ctx = new ContextThemeWrapper(requireContext(), R.style.AlertDialogLight);
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_device_sync, null);
        deviceSyncStatus = v.findViewById(R.id.tv_sync_status);
        refreshDeviceSyncStatus();

        deviceSyncDialog = new AlertDialog.Builder(requireContext(), R.style.AlertDialogLight)
                .setView(v)
                .create();
        final AlertDialog dialog = deviceSyncDialog;

        View btn = v.findViewById(R.id.btn_connect_sync);
        if (btn != null) {
            btn.setOnClickListener(x -> requestHealthConnect()); // permission result callback triggers sync
        }
        v.findViewById(R.id.btn_save_device_sync).setOnClickListener(x -> dialog.dismiss());

        deviceSyncDialog.show();
    }

    private void showSavedFoodsDialog() {
        Context ctx = new ContextThemeWrapper(requireContext(), R.style.AlertDialogLight);
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_saved_foods, null);

        TextView empty = v.findViewById(R.id.tv_saved_empty);
        RecyclerView rv = v.findViewById(R.id.rv_saved_foods);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        final SavedFoodsAdapter[] adapterRef = new SavedFoodsAdapter[1];
        SavedFoodsAdapter adapter = new SavedFoodsAdapter(new ArrayList<>(),
                f -> showMealTypePicker(mealType -> {
                    com.example.forkit.models.FoodEntry e = new com.example.forkit.models.FoodEntry(
                            f.name, f.calories, f.protein, f.carbs, f.fat, mealType
                    );
                    com.example.forkit.utils.CustomFoodHelper.addFoodEntryAndSync(SettingsFragment.this, e);
                    new PrefsHelper(requireContext()).onFoodLogged();
                }),
                f -> {
                    SavedFoodsStore.remove(requireContext(), f.name);
                    List<SavedFood> list = SavedFoodsStore.list(requireContext());
                    if (adapterRef[0] != null) adapterRef[0].update(list);
                    if (empty != null) empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                });
        adapterRef[0] = adapter;
        rv.setAdapter(adapter);

        List<SavedFood> list = SavedFoodsStore.list(requireContext());
        adapter.update(list);
        if (empty != null) empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);

        savedFoodsDialog = new AlertDialog.Builder(requireContext(), R.style.AlertDialogLight)
                .setView(v)
                .create();
        savedFoodsDialog.show();

        v.findViewById(R.id.btn_save_saved_foods).setOnClickListener(x -> {
            if (savedFoodsDialog != null) savedFoodsDialog.dismiss();
        });
    }

    private void showPrivacyDialog() {
        Context ctx = new ContextThemeWrapper(requireContext(), R.style.AlertDialogLight);
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_privacy, null);
        privacyDialog = new AlertDialog.Builder(requireContext(), R.style.AlertDialogLight)
                .setView(v)
                .create();
        privacyDialog.show();

        v.findViewById(R.id.btn_save_privacy).setOnClickListener(x -> {
            if (privacyDialog != null) privacyDialog.dismiss();
        });
    }

    private void showHistoryDialog() {
        Context ctx = new ContextThemeWrapper(requireContext(), R.style.AlertDialogLight);
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_history, null);
        TextView empty = v.findViewById(R.id.tv_history_empty);
        RecyclerView rv = v.findViewById(R.id.rv_history);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        HistoryAdapter adapter = new HistoryAdapter(new ArrayList<>(), day -> {
            // Reuse existing detail popup from HistoryFragment style: show meals list.
            if (getContext() == null || day == null) return;
            StringBuilder sb = new StringBuilder();
            java.util.ArrayList<com.example.forkit.models.FoodEntry> meals = new java.util.ArrayList<>(day.meals);
            meals.sort(java.util.Comparator.comparingLong(com.example.forkit.models.FoodEntry::getTimestamp).reversed());
            for (com.example.forkit.models.FoodEntry e : meals) {
                sb.append("• ").append(e.getFoodName()).append(" — ").append(e.getCalories()).append(" kcal\n");
            }
            if (sb.length() == 0) sb.append("No meals logged.");
            new AlertDialog.Builder(getContext(), R.style.AlertDialogLight)
                    .setTitle(day.label + " (" + day.totalCalories + " kcal)")
                    .setMessage(sb.toString().trim())
                    .setPositiveButton("OK", (x, y) -> {})
                    .show();
        });
        rv.setAdapter(adapter);

        // build summaries using existing HistoryFragment logic by instantiating it is overkill jsut do minimal grouping here.
        java.util.List<com.example.forkit.models.FoodEntry> entries = FoodStore.getEntriesView();
        java.util.Map<String, com.example.forkit.activities.HistoryFragment.DaySummary> map = new java.util.HashMap<>();
        java.util.TimeZone tz = java.util.TimeZone.getTimeZone("Asia/Singapore");
        java.util.Calendar c = java.util.Calendar.getInstance(tz);
        for (com.example.forkit.models.FoodEntry e : entries) {
            if (e == null) continue;
            c.setTimeInMillis(e.getTimestamp());
            String key = c.get(java.util.Calendar.YEAR) + "-" + c.get(java.util.Calendar.DAY_OF_YEAR);
            com.example.forkit.activities.HistoryFragment.DaySummary s = map.get(key);
            if (s == null) {
                s = new com.example.forkit.activities.HistoryFragment.DaySummary(key);
                map.put(key, s);
            }
            s.totalCalories += e.getCalories();
            s.meals.add(e);
        }
        java.util.ArrayList<com.example.forkit.activities.HistoryFragment.DaySummary> days = new java.util.ArrayList<>(map.values());
        days.sort((a, b) -> Long.compare(b.dayStartMs, a.dayStartMs));
        for (com.example.forkit.activities.HistoryFragment.DaySummary d : days) {
            d.waterMl = p != null ? p.getWaterForDateKey(d.key) : 0;
        }
        adapter.update(days);
        if (empty != null) empty.setVisibility(days.isEmpty() ? View.VISIBLE : View.GONE);

        historyDialog = new AlertDialog.Builder(requireContext(), R.style.AlertDialogLight)
                .setView(v)
                .create();
        historyDialog.show();

        v.findViewById(R.id.btn_save_history).setOnClickListener(x -> {
            if (historyDialog != null) historyDialog.dismiss();
        });
    }

    private void showMealTypePicker(java.util.function.Consumer<String> onSelected) {
        if (getContext() == null) return;
        View v = LayoutInflater.from(new android.view.ContextThemeWrapper(getContext(), R.style.AlertDialogLight)).inflate(R.layout.dialog_meal_type, null);
        AlertDialog d = new AlertDialog.Builder(getContext(), R.style.AlertDialogLight).setView(v).create();
        String[] meals = {"Breakfast", "Lunch", "Dinner", "Tea", "Snack"};
        for (String meal : meals) {
            int id = meal.equals("Breakfast") ? R.id.option_breakfast : meal.equals("Lunch") ? R.id.option_lunch : meal.equals("Dinner") ? R.id.option_dinner : meal.equals("Tea") ? R.id.option_tea : R.id.option_snack;
            v.findViewById(id).setOnClickListener(x -> { d.dismiss(); onSelected.accept(meal); });
        }
        d.show();
    }

    private void refreshDeviceSyncStatus() {
        if (deviceSyncStatus == null || p == null) return;
        deviceSyncStatus.setText(
                "Steps today: " + p.getStepsToday() + "\n" +
                        "Exercise today: " + p.getExerciseMins() + " min\n" +
                        "Active calories today: " + p.getCaloriesBurned() + " kcal"
        );
    }

    private boolean ensureHealthConnectAvailable() {
        try {
            int status = androidx.health.connect.client.HealthConnectClient.getSdkStatus(requireContext());
            if (status == androidx.health.connect.client.HealthConnectClient.SDK_UNAVAILABLE) {
                if (deviceSyncStatus != null) deviceSyncStatus.setText("Health Connect isn’t available on this device.");
                return false;
            }
            if (status == androidx.health.connect.client.HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
                if (deviceSyncStatus != null) deviceSyncStatus.setText("Download Health Connect to connect your devices.");
                openHealthConnectStore();
                return false;
            }
            return true;
        } catch (Exception e) {
            if (deviceSyncStatus != null) deviceSyncStatus.setText("Download Health Connect to connect your devices.");
            return false;
        }
    }

    private void requestHealthConnect() {
        if (!ensureHealthConnectAvailable()) return;
        requestHealthConnectPermissions.launch(HealthConnectBridge.permissions());
    }

    private void syncFromHealthConnect() {
        if (!ensureHealthConnectAvailable()) return;
        if (deviceSyncStatus != null) deviceSyncStatus.setText("Syncing…");

        HealthConnectBridge.readTodayMetrics(requireContext(), new HealthConnectBridge.Callback() {
            @Override
            public void onSuccess(HealthConnectBridge.Metrics m) {
                if (!isAdded() || p == null) return;
                p.setStepsToday(m.getStepsToday());
                p.setExerciseMins(m.getExerciseMinutesToday());
                p.setCaloriesBurned(m.getActiveCaloriesToday());
                refreshDeviceSyncStatus();
                Toast.makeText(requireContext(), "Connected", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                // User-friendly messages only.
                if (deviceSyncStatus != null) {
                    deviceSyncStatus.setText("To connect, tap “Connect (Health Connect)” and allow permissions.");
                }
            }
        });
    }
    //pretty sure this is screwed up
    private void openHealthConnectStore() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception ignored) {
            try {
                Intent i2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"));
                startActivity(i2);
            } catch (Exception ignored2) {}
        }
    }

    private String getText(View parent, int id) {
        EditText et = parent.findViewById(id);
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }
}
