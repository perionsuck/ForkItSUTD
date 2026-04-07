package com.example.forkit.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.view.ContextThemeWrapper;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.forkit.MainActivity;
import com.example.forkit.R;
import com.example.forkit.models.FoodEntry;
import com.example.forkit.models.UserGoals;
import com.example.forkit.utils.FoodAnalytics;
import com.example.forkit.utils.FoodStore;
import com.example.forkit.utils.PrefsHelper;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvCalLeft, tvCalConsumed, tvCalBurned;
    private LinearProgressIndicator progressProtein, progressCarbs, progressFat;
    private TextView tvProtein, tvCarbs, tvFat;
    private TextView tvStreak, tvHeartRate;
    private TextView tvPeakDay, tvPeakKcal, tvTopMeal, tvTopMealKcal;
    private View statsCollapsible;
    private ImageView ivStatsArrow;
    private View[] waterBars;
    private PrefsHelper prefs;

    // Logged foods are stored in FoodStore; keep static list only for backward compatibility.
    public static List<FoodEntry> foodEntries = new ArrayList<>();
    public static UserGoals userGoals = new UserGoals();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new PrefsHelper(requireContext());
        syncUserGoalsFromPrefs();

        tvCalLeft = view.findViewById(R.id.tv_cal_left);
        tvCalConsumed = view.findViewById(R.id.tv_cal_consumed);
        tvCalBurned = view.findViewById(R.id.tv_cal_burned);
        progressProtein = view.findViewById(R.id.progress_protein);
        progressCarbs = view.findViewById(R.id.progress_carbs);
        progressFat = view.findViewById(R.id.progress_fat);
        tvProtein = view.findViewById(R.id.tv_protein_val);
        tvCarbs = view.findViewById(R.id.tv_carbs_val);
        tvFat = view.findViewById(R.id.tv_fat_val);
        tvStreak = view.findViewById(R.id.tv_streak);
        tvHeartRate = view.findViewById(R.id.tv_heart_rate);
        tvPeakDay = view.findViewById(R.id.tv_peak_day);
        tvPeakKcal = view.findViewById(R.id.tv_peak_kcal);
        tvTopMeal = view.findViewById(R.id.tv_top_meal);
        tvTopMealKcal = view.findViewById(R.id.tv_top_meal_kcal);
        statsCollapsible = view.findViewById(R.id.stats_collapsible);
        ivStatsArrow = view.findViewById(R.id.iv_stats_arrow);

        waterBars = new View[]{
                view.findViewById(R.id.bar_mon), view.findViewById(R.id.bar_tue),
                view.findViewById(R.id.bar_wed), view.findViewById(R.id.bar_thu),
                view.findViewById(R.id.bar_fri), view.findViewById(R.id.bar_sat),
                view.findViewById(R.id.bar_sun)
        };

        view.findViewById(R.id.btn_see_stats).setOnClickListener(v -> toggleStats());
        view.findViewById(R.id.btn_menu).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openDrawer();
            }
        });

        view.findViewById(R.id.recommendations_card).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new RecommendationsFragment());
                ((MainActivity) getActivity()).selectNavItem(R.id.nav_home);
            }
        });

        view.findViewById(R.id.btn_add_water).setOnClickListener(v -> showWaterDialog());
        view.findViewById(R.id.water_card).setOnClickListener(v -> showWaterDialog());

        updateDashboard();
    }

    private void syncUserGoalsFromPrefs() {
        if (prefs == null) return;
        userGoals.setDailyCalorieGoal(prefs.getCalorieGoal());
        userGoals.setDailyBurnGoal(prefs.getCaloriesBurned());
        userGoals.setProteinGoal(prefs.getProteinGoal());
        userGoals.setCarbsGoal(prefs.getCarbsGoal());
        userGoals.setFatGoal(prefs.getFatGoal());
    }

    private void showWaterDialog() {
        View d = LayoutInflater.from(new ContextThemeWrapper(requireContext(), R.style.AlertDialogLight)).inflate(R.layout.dialog_water_input, null);
        EditText et = d.findViewById(R.id.et_water_ml);
        TextView goalTv = d.findViewById(R.id.tv_water_goal);
        goalTv.setText("goal: " + prefs.getWaterGoalMl() + " ml/day (tap to change)");
        goalTv.setOnClickListener(v -> showWaterGoalDialog());
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.AlertDialogLight).setView(d).create();
        d.findViewById(R.id.btn_water_confirm).setOnClickListener(x -> {
            try {
                String s = et.getText() != null ? et.getText().toString().trim() : "";
                if (!TextUtils.isEmpty(s)) {
                    int ml = Integer.parseInt(s);
                    if (ml > 0) {
                        prefs.addWaterForDay(getDayIndex(), ml);
                        prefs.addWaterForToday(ml);
                        updateWaterBars();
                    }
                }
            } catch (Exception ignored) {}
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showWaterGoalDialog() {
        View v = LayoutInflater.from(new ContextThemeWrapper(requireContext(), R.style.AlertDialogLight)).inflate(R.layout.dialog_water_goal, null);
        EditText et = v.findViewById(R.id.et_water_goal);
        et.setText(String.valueOf(prefs.getWaterGoalMl()));
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.AlertDialogLight).setView(v).create();
        v.findViewById(R.id.btn_save_water_goal).setOnClickListener(x -> {
            try {
                int ml = Integer.parseInt(et.getText() != null ? et.getText().toString().trim() : "2000");
                if (ml > 0) prefs.setWaterGoalMl(ml);
            } catch (Exception ignored) {}
            updateWaterBars();
            dialog.dismiss();
            Toast.makeText(requireContext(), "Goal updated", Toast.LENGTH_SHORT).show();
        });
        dialog.show();
    }

    private void toggleStats() {
        boolean expanded = statsCollapsible.getVisibility() == View.VISIBLE;
        if (expanded) {
            statsCollapsible.animate().alpha(0).setDuration(200).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator a) {
                    statsCollapsible.setVisibility(View.GONE);
                    ivStatsArrow.setImageResource(R.drawable.ic_see_stats_up);
                }
            }).start();
        } else {
            statsCollapsible.setAlpha(0);
            statsCollapsible.setVisibility(View.VISIBLE);
            statsCollapsible.animate().alpha(1).setDuration(200).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator a) {
                    ivStatsArrow.setImageResource(R.drawable.ic_see_stats);
                }
            }).start();
        }
    }

    private int getDayIndex() {
        // Bars: 0=Mon, 1=Tue, 2=Wed, 3=Thu, 4=Fri, 5=Sat, 6=Sun. Use Singapore timezone.
        java.util.Calendar cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Singapore"));
        int dow = cal.get(Calendar.DAY_OF_WEEK); // 1=Sun, 2=Mon, ..., 7=Sat
        return (dow + 5) % 7;
    }

    private void updateWaterBars() {
        int goal = prefs.getWaterGoalMl();
        if (goal <= 0) goal = 2000;
        int maxH = 48;
        for (int i = 0; i < 7; i++) {
            int ml = prefs.getWaterForDay(i);
            int h = goal > 0 ? Math.min(maxH, Math.max(4, (ml * maxH) / goal)) : 4;
            waterBars[i].getLayoutParams().height = h;
            waterBars[i].requestLayout();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDashboard();
    }

    public void updateDashboard() {
        int totalCalIn = 0;
        float totalProtein = 0, totalCarbs = 0, totalFat = 0;

        List<FoodEntry> list = FoodStore.getEntriesView();
        for (FoodEntry e : list) {
            totalCalIn += e.getCalories();
            totalProtein += e.getProtein();
            totalCarbs += e.getCarbs();
            totalFat += e.getFat();
        }

        int calBurned = userGoals.getDailyBurnGoal();
        int calGoal = userGoals.getDailyCalorieGoal() > 0 ? userGoals.getDailyCalorieGoal() : 2000;
        int calLeft = calGoal - totalCalIn + calBurned;
        int pGoal = (int) (userGoals.getProteinGoal() > 0 ? userGoals.getProteinGoal() : 150);
        int cGoal = (int) (userGoals.getCarbsGoal() > 0 ? userGoals.getCarbsGoal() : 250);
        int fGoal = (int) (userGoals.getFatGoal() > 0 ? userGoals.getFatGoal() : 65);
        int pLeft = Math.max(0, pGoal - (int) totalProtein);
        int cLeft = Math.max(0, cGoal - (int) totalCarbs);
        int fLeft = Math.max(0, fGoal - (int) totalFat);
        int pPct = pGoal > 0 ? Math.min((int) (totalProtein / pGoal * 100), 100) : 0;
        int cPct = cGoal > 0 ? Math.min((int) (totalCarbs / cGoal * 100), 100) : 0;
        int fPct = fGoal > 0 ? Math.min((int) (totalFat / fGoal * 100), 100) : 0;

        tvCalLeft.setText(String.valueOf(calLeft>=0 ? calLeft : 0));
        tvCalConsumed.setText(String.valueOf(totalCalIn));
        tvCalBurned.setText(String.valueOf(calBurned));
        progressProtein.setProgress(pPct);
        progressCarbs.setProgress(cPct);
        progressFat.setProgress(fPct);
        tvProtein.setText(String.format(Locale.getDefault(), "%dg left", pLeft));
        tvCarbs.setText(String.format(Locale.getDefault(), "%dg left", cLeft));
        tvFat.setText(String.format(Locale.getDefault(), "%dg left", fLeft));
        tvStreak.setText(String.valueOf(prefs.getStreak()));
        tvHeartRate.setText(prefs.getExerciseMins() > 0 ? prefs.getExerciseMins() + " min" : "0");
        updateWaterBars();

        // Weekly analysis: peak calorie day + highest calorie meal
        FoodAnalytics.DayTotal peak = FoodStore.peekPeakDay();
        if (tvPeakDay != null) {
            tvPeakDay.setText("Peak: " + (peak != null ? FoodAnalytics.dayLabelMon0(peak.dayIndexMon0) : "—"));
        }
        if (tvPeakKcal != null) {
            tvPeakKcal.setText((peak != null ? peak.calories : 0) + " kcal");
        }
        FoodEntry topMeal = FoodStore.peekTopMeal();
        if (tvTopMeal != null) {
            tvTopMeal.setText("Top meal: " + (topMeal != null ? topMeal.getFoodName() : "—"));
        }
        if (tvTopMealKcal != null) {
            tvTopMealKcal.setText((topMeal != null ? topMeal.getCalories() : 0) + " kcal");
        }
    }
}
