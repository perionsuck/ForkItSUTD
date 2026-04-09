package com.example.forkit.utils;

import android.app.AlertDialog;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.forkit.MainActivity;
import com.example.forkit.R;
import com.example.forkit.activities.HomeFragment;
import com.example.forkit.models.FoodEntry;
import com.example.forkit.utils.FoodStore;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class CustomFoodHelper {

    private CustomFoodHelper() {
    }

    public static void show(Fragment fragment) {
        if (fragment.getContext() == null) return;
        View dialogView = LayoutInflater.from(new android.view.ContextThemeWrapper(fragment.getContext(), R.style.AlertDialogLight))
                .inflate(R.layout.dialog_custom_food, null);
        EditText etName = dialogView.findViewById(R.id.et_custom_name);
        EditText etDesc = dialogView.findViewById(R.id.et_custom_description);
        EditText etCal = dialogView.findViewById(R.id.et_custom_calories);
        EditText etProtein = dialogView.findViewById(R.id.et_custom_protein);
        EditText etCarbs = dialogView.findViewById(R.id.et_custom_carbs);
        EditText etFat = dialogView.findViewById(R.id.et_custom_fat);
        if (etName == null || etCal == null) return;

        AlertDialog dialog = new AlertDialog.Builder(fragment.getContext(), R.style.AlertDialogLight)
                .setView(dialogView)
                .create();

        android.widget.Spinner spinnerMeal = dialogView.findViewById(R.id.spinner_meal_type);
        if (spinnerMeal != null) {
            String[] meals = {"Breakfast", "Lunch", "Dinner", "Tea", "Snack"};
            android.widget.ArrayAdapter<String> mealAdapter = new android.widget.ArrayAdapter<>(fragment.getContext(), android.R.layout.simple_spinner_item, meals);
            mealAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerMeal.setAdapter(mealAdapter);
        }

        dialogView.findViewById(R.id.btn_save_custom).setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                Toast.makeText(fragment.requireContext(), "Enter food name", Toast.LENGTH_SHORT).show();
                return;
            }
            String desc = etDesc != null && etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
            if (!desc.isEmpty()) name = name + " - " + desc;
            int cal = parseInt(etCal != null ? etCal.getText() : null, 0);
            float protein = parseFloat(etProtein != null ? etProtein.getText() : null, 0f);
            float carbs = parseFloat(etCarbs != null ? etCarbs.getText() : null, 0f);
            float fat = parseFloat(etFat != null ? etFat.getText() : null, 0f);
            String mealType = "Lunch";
            if (spinnerMeal != null && spinnerMeal.getSelectedItem() != null)
                mealType = spinnerMeal.getSelectedItem().toString();
            addFoodEntryAndSync(fragment, new FoodEntry(name, cal, protein, carbs, fat, mealType));
            new PrefsHelper(fragment.requireContext()).onFoodLogged();
            Toast.makeText(fragment.requireContext(), name + " added", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        dialog.show();
    }

    public static void addFoodEntryAndSync(Fragment fragment, FoodEntry entry) {
        FoodStore.add(entry);
        if (fragment.getContext() == null) return;

        PrefsHelper prefs = new PrefsHelper(fragment.getContext());
        String userId = prefs.getUserId();
        if (userId == null || userId.isEmpty()) return;

        entry.setUserId(userId);
        entry.setUserAdded(true);
        String token = prefs.getAccessToken();
        if (token != null && !token.isEmpty()) {
            SupabaseClient.setAccessToken(token);
        }
        SupabaseApi api = SupabaseClient.getClient().create(SupabaseApi.class);
        api.insertFoodEntry(entry).enqueue(new Callback<List<FoodEntry>>() {
            @Override
            public void onResponse(Call<List<FoodEntry>> call, Response<List<FoodEntry>> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null && !response.body().isEmpty()) {
                        entry.setId(response.body().get(0).getId());
                    }
                    if (fragment.getActivity() instanceof MainActivity) {
                        ((MainActivity) fragment.getActivity()).refreshFoodEntriesFromCloud();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<FoodEntry>> call, Throwable t) {
            }
        });
    }

    private static int parseInt(Editable e, int def) {
        if (e == null || e.length() == 0) return def;
        try {
            return Integer.parseInt(e.toString());
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    private static float parseFloat(Editable e, float def) {
        if (e == null || e.length() == 0) return def;
        try {
            return Float.parseFloat(e.toString());
        } catch (NumberFormatException ex) {
            return def;
        }
    }
}
