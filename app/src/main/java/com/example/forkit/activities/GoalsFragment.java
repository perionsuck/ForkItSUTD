package com.example.forkit.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.forkit.MainActivity;
import com.example.forkit.R;
import com.example.forkit.models.UserGoals;
import com.example.forkit.utils.SupabaseApi;
import com.example.forkit.utils.SupabaseClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GoalsFragment extends Fragment {

    private TextInputEditText etName, etCalGoal, etBurnGoal, etProtein, etCarbs, etFat;
    private MaterialButton btnSave;
    private SupabaseApi supabaseApi;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_goals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        supabaseApi = SupabaseClient.getClient().create(SupabaseApi.class);

        etName     = view.findViewById(R.id.et_name);
        etCalGoal  = view.findViewById(R.id.et_cal_goal);
        etBurnGoal = view.findViewById(R.id.et_burn_goal);
        etProtein  = view.findViewById(R.id.et_protein);
        etCarbs    = view.findViewById(R.id.et_carbs);
        etFat      = view.findViewById(R.id.et_fat);
        btnSave    = view.findViewById(R.id.btn_save_goals);

        view.findViewById(R.id.btn_goals_menu).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).openDrawer();
        });

        // this is to load existing goals into fields
        etName.setText(HomeFragment.userGoals.getUserName());
        etCalGoal.setText(String.valueOf(HomeFragment.userGoals.getDailyCalorieGoal()));
        etBurnGoal.setText(String.valueOf(HomeFragment.userGoals.getDailyBurnGoal()));
        etProtein.setText(String.valueOf((int) HomeFragment.userGoals.getProteinGoal()));
        etCarbs.setText(String.valueOf((int) HomeFragment.userGoals.getCarbsGoal()));
        etFat.setText(String.valueOf((int) HomeFragment.userGoals.getFatGoal()));

        btnSave.setOnClickListener(v -> saveGoals());
    }

    private void saveGoals() {
        String name;
        int calGoal, burnGoal;
        float protein, carbs, fat;

        try {
            name     = etName.getText() != null ? etName.getText().toString().trim() : "";
            calGoal  = Integer.parseInt(etCalGoal.getText().toString().trim());
            burnGoal = Integer.parseInt(etBurnGoal.getText().toString().trim());
            protein  = Float.parseFloat(etProtein.getText().toString().trim());
            carbs    = Float.parseFloat(etCarbs.getText().toString().trim());
            fat      = Float.parseFloat(etFat.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(),
                    "Please fill in all fields with valid numbers",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // must update local in memory object
        HomeFragment.userGoals.setUserName(name);
        HomeFragment.userGoals.setDailyCalorieGoal(calGoal);
        HomeFragment.userGoals.setDailyBurnGoal(burnGoal);
        HomeFragment.userGoals.setProteinGoal(protein);
        HomeFragment.userGoals.setCarbsGoal(carbs);
        HomeFragment.userGoals.setFatGoal(fat);

        // this is to disable button while saving
        btnSave.setEnabled(false);

        // check for goal existence if any
        String userIdFilter = "eq." + HomeFragment.userGoals.getId();

        supabaseApi.getUserGoals("*", userIdFilter).enqueue(new Callback<List<UserGoals>>() {
            @Override
            public void onResponse(@NonNull Call<List<UserGoals>> call, @NonNull Response<List<UserGoals>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    //if row exists then we update it
                    String existingId = "eq." + response.body().get(0).getId();
                    supabaseApi.updateUserGoals(existingId, HomeFragment.userGoals)
                            .enqueue(saveCallback);
                } else {
                    // this is just to update
                    supabaseApi.insertUserGoals(HomeFragment.userGoals)
                            .enqueue(saveCallback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<UserGoals>> call, @NonNull Throwable t) {
                Log.e("Supabase", "Failed to check existing goals: " + t.getClass().getName() + " - " + t.getMessage());
                if (t.getCause() != null) Log.e("Supabase", "Cause: " + t.getCause().getMessage());
                onSaveError();
            }
        });
    }

    private final Callback<List<UserGoals>> saveCallback = new Callback<List<UserGoals>>() {
        @Override
        public void onResponse(@NonNull Call<List<UserGoals>> call, @NonNull Response<List<UserGoals>> response) {
            btnSave.setEnabled(true);
            if (response.isSuccessful()) {
                Snackbar.make(requireView(), "✅ Goals saved!", Snackbar.LENGTH_SHORT).show();
            } else {
                Log.e("Supabase", "Save failed: " + response.code() + " " + response.message());
                Snackbar.make(requireView(), "❌ Save failed (code " + response.code() + ")", Snackbar.LENGTH_LONG).show();
            }
        }

        @Override
        public void onFailure(@NonNull Call<List<UserGoals>> call, @NonNull Throwable t) {
            Log.e("Supabase", "Network error saving goals", t);
            onSaveError();
        }
    };

    private void onSaveError() {
        if (getView() != null) {
            btnSave.setEnabled(true);
            Snackbar.make(requireView(), "❌ Network error, try again", Snackbar.LENGTH_LONG).show();
        }
    }
}