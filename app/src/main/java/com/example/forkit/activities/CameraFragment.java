package com.example.forkit.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.forkit.R;
import com.example.forkit.models.FoodEntry;
import com.example.forkit.utils.ApiClient;
import com.example.forkit.utils.CaloriesNinjaApi;
import com.example.forkit.utils.DateUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CameraFragment extends Fragment {

    private TextInputEditText etFoodQuery;
    private TextView tvResult, tvCaloriesFound, tvMacrosFound;
    private MaterialButton btnSearch, btnAddFood;
    private Spinner spinnerMealType;
    private ProgressBar progressBar;

    private CaloriesNinjaApi.NutritionItem foundItem = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etFoodQuery     = view.findViewById(R.id.et_food_query);
        tvResult        = view.findViewById(R.id.tv_result_label);
        tvCaloriesFound = view.findViewById(R.id.tv_calories_found);
        tvMacrosFound   = view.findViewById(R.id.tv_macros_found);
        btnSearch       = view.findViewById(R.id.btn_search_food);
        btnAddFood      = view.findViewById(R.id.btn_add_food);
        spinnerMealType = view.findViewById(R.id.spinner_meal_type);
        progressBar     = view.findViewById(R.id.progress_search);

        // Meal type spinner
        String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Snack"};
        ArrayAdapter<String> mealAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, mealTypes);
        spinnerMealType.setAdapter(mealAdapter);

        // Auto select meal based on time
        String suggested = DateUtils.getMealTypeForHour();
        for (int i = 0; i < mealTypes.length; i++) {
            if (mealTypes[i].equals(suggested)) spinnerMealType.setSelection(i);
        }

        btnSearch.setOnClickListener(v -> searchFood());
        btnAddFood.setOnClickListener(v -> addFoodToLog());
        btnAddFood.setVisibility(View.GONE);
    }

    private void searchFood() {
        String query = etFoodQuery.getText() != null ? etFoodQuery.getText().toString().trim() : "";
        if (TextUtils.isEmpty(query)) {
            etFoodQuery.setError("Enter a food name");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSearch.setEnabled(false);
        tvResult.setText("Searching...");
        btnAddFood.setVisibility(View.GONE);
        foundItem = null;

        ApiClient.getApi().getNutrition(ApiClient.API_KEY, query)
                .enqueue(new Callback<CaloriesNinjaApi.NutritionResponse>() {
                    @Override
                    public void onResponse(Call<CaloriesNinjaApi.NutritionResponse> call,
                                           Response<CaloriesNinjaApi.NutritionResponse> response) {
                        progressBar.setVisibility(View.GONE);
                        btnSearch.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null) {
                            List<CaloriesNinjaApi.NutritionItem> items = response.body().items;
                            if (items != null && !items.isEmpty()) {
                                foundItem = items.get(0);
                                tvResult.setText("Found: " + capitalize(foundItem.name));
                                tvCaloriesFound.setText((int) foundItem.calories + " kcal");
                                tvMacrosFound.setText(String.format(
                                        "Protein: %.1fg   Carbs: %.1fg   Fat: %.1fg",
                                        foundItem.protein_g,
                                        foundItem.carbohydrates_total_g,
                                        foundItem.fat_total_g));
                                btnAddFood.setVisibility(View.VISIBLE);
                            } else {
                                tvResult.setText("❌ No results found. Try a different name.");
                                tvCaloriesFound.setText("");
                                tvMacrosFound.setText("");
                            }
                        } else {
                            showDemoResult(query);
                        }
                    }

                    @Override
                    public void onFailure(Call<CaloriesNinjaApi.NutritionResponse> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnSearch.setEnabled(true);
                        showDemoResult(query);
                    }
                });
    }

    private void showDemoResult(String query) {
        foundItem = new CaloriesNinjaApi.NutritionItem();
        foundItem.name = query;
        foundItem.calories = 250;
        foundItem.protein_g = 15;
        foundItem.carbohydrates_total_g = 30;
        foundItem.fat_total_g = 8;

        tvResult.setText("📊 Demo: " + capitalize(query) + " (rough estimate)");
        tvCaloriesFound.setText("~250 kcal");
        tvMacrosFound.setText("Protein: 15g   Carbs: 30g   Fat: 8g");
        btnAddFood.setVisibility(View.VISIBLE);
    }

    private void addFoodToLog() {
        if (foundItem == null) return;

        String mealType = spinnerMealType.getSelectedItem().toString();
        FoodEntry entry = new FoodEntry(
                capitalize(foundItem.name),
                (int) foundItem.calories,
                (float) foundItem.protein_g,
                (float) foundItem.carbohydrates_total_g,
                (float) foundItem.fat_total_g,
                mealType
        );

        HomeFragment.foodEntries.add(entry);

        Snackbar.make(requireView(),
                "✅ " + capitalize(foundItem.name) + " added to " + mealType,
                Snackbar.LENGTH_SHORT).show();

        etFoodQuery.setText("");
        tvResult.setText("Search for a food item below");
        tvCaloriesFound.setText("");
        tvMacrosFound.setText("");
        btnAddFood.setVisibility(View.GONE);
        foundItem = null;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}