package com.example.forkit.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.forkit.R;
import com.example.forkit.models.FoodEntry;
import com.example.forkit.models.UserGoals;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvGreeting, tvDate, tvCalLeft, tvCalConsumed, tvCalBurned;
    private TextView tvCalGoal, tvNetCal, tvStatusMsg;
    private CircularProgressIndicator circularProgress;
    private LinearProgressIndicator progressProtein, progressCarbs, progressFat;
    private TextView tvProtein, tvCarbs, tvFat;

    // Temporary in-memory storage until Supabase is connected
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

        tvGreeting       = view.findViewById(R.id.tv_greeting);
        tvDate           = view.findViewById(R.id.tv_date);
        tvCalLeft        = view.findViewById(R.id.tv_cal_left);
        tvCalConsumed    = view.findViewById(R.id.tv_cal_consumed);
        tvCalBurned      = view.findViewById(R.id.tv_cal_burned);
        tvCalGoal        = view.findViewById(R.id.tv_cal_goal);
        tvNetCal         = view.findViewById(R.id.tv_net_cal);
        circularProgress = view.findViewById(R.id.circular_progress);
        progressProtein  = view.findViewById(R.id.progress_protein);
        progressCarbs    = view.findViewById(R.id.progress_carbs);
        progressFat      = view.findViewById(R.id.progress_fat);
        tvProtein        = view.findViewById(R.id.tv_protein_val);
        tvCarbs          = view.findViewById(R.id.tv_carbs_val);
        tvFat            = view.findViewById(R.id.tv_fat_val);
        tvStatusMsg      = view.findViewById(R.id.tv_status_msg);

        // Date
        String date = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(new Date());
        tvDate.setText(date);

        // Greeting based on time of day
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String greeting = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
        String name = userGoals.getUserName() != null && !userGoals.getUserName().isEmpty()
                ? ", " + userGoals.getUserName() : "";
        tvGreeting.setText(greeting + name + " 👋");

        updateDashboard();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDashboard();
    }

    public void updateDashboard() {
        int totalCalIn = 0;
        float totalProtein = 0, totalCarbs = 0, totalFat = 0;

        for (FoodEntry e : foodEntries) {
            totalCalIn   += e.getCalories();
            totalProtein += e.getProtein();
            totalCarbs   += e.getCarbs();
            totalFat     += e.getFat();
        }

        int calBurned = userGoals.getDailyBurnGoal();
        int calLeft   = userGoals.getDailyCalorieGoal() - totalCalIn + calBurned;
        int netCal    = totalCalIn - calBurned;
        int percent   = userGoals.getDailyCalorieGoal() > 0
                ? Math.min((int)((totalCalIn / (float) userGoals.getDailyCalorieGoal()) * 100), 100)
                : 0;

        tvCalLeft.setText(String.valueOf(calLeft));
        tvCalConsumed.setText(totalCalIn + " kcal eaten");
        tvCalBurned.setText(calBurned + " kcal burned");
        tvCalGoal.setText("Goal: " + userGoals.getDailyCalorieGoal());
        tvNetCal.setText("Net: " + netCal + " kcal");
        circularProgress.setProgress(percent);

        // Macros
        int pPct = userGoals.getProteinGoal() > 0 ? Math.min((int)(totalProtein / userGoals.getProteinGoal() * 100), 100) : 0;
        int cPct = userGoals.getCarbsGoal()   > 0 ? Math.min((int)(totalCarbs   / userGoals.getCarbsGoal()   * 100), 100) : 0;
        int fPct = userGoals.getFatGoal()     > 0 ? Math.min((int)(totalFat     / userGoals.getFatGoal()     * 100), 100) : 0;

        progressProtein.setProgress(pPct);
        progressCarbs.setProgress(cPct);
        progressFat.setProgress(fPct);
        tvProtein.setText(String.format(Locale.getDefault(), "%.0fg / %.0fg", totalProtein, userGoals.getProteinGoal()));
        tvCarbs.setText(String.format(Locale.getDefault(),   "%.0fg / %.0fg", totalCarbs,   userGoals.getCarbsGoal()));
        tvFat.setText(String.format(Locale.getDefault(),     "%.0fg / %.0fg", totalFat,     userGoals.getFatGoal()));

        // Status message
        if (totalCalIn > userGoals.getDailyCalorieGoal()) {
            tvStatusMsg.setText("⚠️ Over your daily goal!");
            tvStatusMsg.setTextColor(Color.parseColor("#EF5350"));
        } else if (percent > 80) {
            tvStatusMsg.setText("🟡 Getting close to your limit!");
            tvStatusMsg.setTextColor(Color.parseColor("#FFA726"));
        } else {
            tvStatusMsg.setText("✅ You're on track today!");
            tvStatusMsg.setTextColor(Color.parseColor("#30D158"));
        }
    }
}