package com.example.forkit.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.forkit.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

public class GoalsFragment extends Fragment {

    private TextInputEditText etName, etCalGoal, etBurnGoal, etProtein, etCarbs, etFat;
    private MaterialButton btnSave;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_goals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etName     = view.findViewById(R.id.et_name);
        etCalGoal  = view.findViewById(R.id.et_cal_goal);
        etBurnGoal = view.findViewById(R.id.et_burn_goal);
        etProtein  = view.findViewById(R.id.et_protein);
        etCarbs    = view.findViewById(R.id.et_carbs);
        etFat      = view.findViewById(R.id.et_fat);
        btnSave    = view.findViewById(R.id.btn_save_goals);

        // Load existing goals
        etName.setText(HomeFragment.userGoals.getUserName());
        etCalGoal.setText(String.valueOf(HomeFragment.userGoals.getDailyCalorieGoal()));
        etBurnGoal.setText(String.valueOf(HomeFragment.userGoals.getDailyBurnGoal()));
        etProtein.setText(String.valueOf((int) HomeFragment.userGoals.getProteinGoal()));
        etCarbs.setText(String.valueOf((int) HomeFragment.userGoals.getCarbsGoal()));
        etFat.setText(String.valueOf((int) HomeFragment.userGoals.getFatGoal()));

        btnSave.setOnClickListener(v -> saveGoals());
    }

    private void saveGoals() {
        try {
            String name    = etName.getText() != null ? etName.getText().toString() : "";
            int calGoal    = Integer.parseInt(etCalGoal.getText().toString().trim());
            int burnGoal   = Integer.parseInt(etBurnGoal.getText().toString().trim());
            float protein  = Float.parseFloat(etProtein.getText().toString().trim());
            float carbs    = Float.parseFloat(etCarbs.getText().toString().trim());
            float fat      = Float.parseFloat(etFat.getText().toString().trim());

            HomeFragment.userGoals.setUserName(name);
            HomeFragment.userGoals.setDailyCalorieGoal(calGoal);
            HomeFragment.userGoals.setDailyBurnGoal(burnGoal);
            HomeFragment.userGoals.setProteinGoal(protein);
            HomeFragment.userGoals.setCarbsGoal(carbs);
            HomeFragment.userGoals.setFatGoal(fat);

            Snackbar.make(requireView(), "✅ Goals saved!", Snackbar.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(),
                    "Please fill in all fields with valid numbers",
                    Toast.LENGTH_SHORT).show();
        }
    }
}