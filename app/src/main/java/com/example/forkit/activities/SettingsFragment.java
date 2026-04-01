package com.example.forkit.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
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

import com.example.forkit.MainActivity;
import com.example.forkit.R;
import com.example.forkit.models.UserGoals;
import com.example.forkit.utils.CalorieCalculator;
import com.example.forkit.utils.PrefsHelper;
import com.example.forkit.utils.SupabaseApi;
import com.example.forkit.utils.SupabaseClient;

import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {

    private PrefsHelper p;
    private ImageView ivProfilePic;
    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && p != null) {
                    p.setProfilePicUri(uri.toString());
                    if (ivProfilePic != null) ivProfilePic.setImageURI(uri);
                }
            });

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
        view.findViewById(R.id.settings_saved).setOnClickListener(v -> Toast.makeText(requireContext(), "Saved foods - coming soon", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.settings_progress).setOnClickListener(v -> Toast.makeText(requireContext(), "Progress - coming soon", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.settings_reminders).setOnClickListener(v -> showRemindersDialog());
        view.findViewById(R.id.settings_sync).setOnClickListener(v -> Toast.makeText(requireContext(), "Sync - coming soon", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.settings_privacy).setOnClickListener(v -> Toast.makeText(requireContext(), "Privacy - coming soon", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.settings_history).setOnClickListener(v -> Toast.makeText(requireContext(), "History - coming soon", Toast.LENGTH_SHORT).show());
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
        for (com.example.forkit.models.FoodEntry e : HomeFragment.foodEntries) totalCal += e.getCalories();
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
            p.setRemindersEnabled(((com.google.android.material.switchmaterial.SwitchMaterial) v.findViewById(R.id.switch_reminders)).isChecked());
            p.setReminderTimes(getText(v, R.id.et_reminder_times));
            if (p.getReminderTimes().isEmpty()) p.setReminderTimes("8:00,12:00,18:00");
            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        dialog.show();
    }

    private String getText(View parent, int id) {
        EditText et = parent.findViewById(id);
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }
}
