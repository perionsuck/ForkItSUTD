package com.example.forkit.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.forkit.MainActivity;
import com.example.forkit.R;
import com.example.forkit.adapters.SearchResultAdapter;
import com.example.forkit.models.FoodEntry;
import com.example.forkit.utils.ApiClient;
import com.example.forkit.utils.CaloriesNinjaApi;
import com.example.forkit.utils.PrefsHelper;
import com.example.forkit.utils.SupabaseApi;
import com.example.forkit.utils.SupabaseClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {

    private EditText etSearch;
    private RecyclerView rvResults;
    private SearchResultAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            etSearch = view.findViewById(R.id.et_search);
            rvResults = view.findViewById(R.id.rv_search_results);
            if (etSearch != null) etSearch.setHintTextColor(0xFF000000);
            if (rvResults == null) return;
            adapter = new SearchResultAdapter(new ArrayList<>(), item -> showMealTypePicker((mealType) -> {
                FoodEntry entry = new FoodEntry(
                        item.name, (int) item.calories,
                        (float) item.protein_g, (float) item.carbohydrates_total_g, (float) item.fat_total_g,
                        mealType);
                addEntryAndSync(entry);
                if (getContext() != null) new PrefsHelper(getContext()).onFoodLogged();
                if (getContext() != null) Toast.makeText(getContext(), item.name + " added", Toast.LENGTH_SHORT).show();
            }));
            rvResults.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvResults.setAdapter(adapter);

            View btnMenu = view.findViewById(R.id.btn_search_menu);
            if (btnMenu != null) btnMenu.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).openDrawer();
            });

            if (etSearch != null) etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 2) search(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

            if (etSearch != null) etSearch.setOnEditorActionListener((v, actionId, event) -> {
                String q = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
                if (!q.isEmpty()) {
                    search(q);
                    return true;
                }
                return false;
            });

            View btnCustom = view.findViewById(R.id.btn_add_custom);
            if (btnCustom != null) btnCustom.setOnClickListener(v -> showCustomFoodDialog());
        } catch (Exception e) {
            if (getContext() != null) Toast.makeText(getContext(), "Error loading search", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCustomFoodDialog() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(new android.view.ContextThemeWrapper(getContext(), R.style.AlertDialogLight)).inflate(R.layout.dialog_custom_food, null);
        EditText etName = dialogView.findViewById(R.id.et_custom_name);
        EditText etDesc = dialogView.findViewById(R.id.et_custom_description);
        EditText etCal = dialogView.findViewById(R.id.et_custom_calories);
        EditText etProtein = dialogView.findViewById(R.id.et_custom_protein);
        EditText etCarbs = dialogView.findViewById(R.id.et_custom_carbs);
        EditText etFat = dialogView.findViewById(R.id.et_custom_fat);
        if (etName == null || etCal == null) return;

        AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.AlertDialogLight)
                .setView(dialogView)
                .create();

        android.widget.Spinner spinnerMeal = dialogView.findViewById(R.id.spinner_meal_type);
        if (spinnerMeal != null) {
            String[] meals = {"Breakfast", "Lunch", "Dinner", "Tea", "Snack"};
            android.widget.ArrayAdapter<String> mealAdapter = new android.widget.ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, meals);
            mealAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerMeal.setAdapter(mealAdapter);
        }

        dialogView.findViewById(R.id.btn_save_custom).setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Enter food name", Toast.LENGTH_SHORT).show();
                return;
            }
            String desc = etDesc != null && etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
            if (!desc.isEmpty()) name = name + " - " + desc;
            int cal = parseInt(etCal != null ? etCal.getText() : null, 0);
            float protein = parseFloat(etProtein != null ? etProtein.getText() : null, 0f);
            float carbs = parseFloat(etCarbs != null ? etCarbs.getText() : null, 0f);
            float fat = parseFloat(etFat != null ? etFat.getText() : null, 0f);
            String mealType = "Lunch";
            if (spinnerMeal != null && spinnerMeal.getSelectedItem() != null) mealType = spinnerMeal.getSelectedItem().toString();
            addEntryAndSync(new FoodEntry(name, cal, protein, carbs, fat, mealType));
            new PrefsHelper(requireContext()).onFoodLogged();
            Toast.makeText(requireContext(), name + " added", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void addEntryAndSync(FoodEntry entry) {
        HomeFragment.foodEntries.add(entry);
        if (getContext() == null) return;

        PrefsHelper prefs = new PrefsHelper(getContext());
        String userId = prefs.getUserId();
        if (userId == null || userId.isEmpty()) return;

        entry.setUserId(userId);
        SupabaseApi api = SupabaseClient.getClient().create(SupabaseApi.class);
        api.insertFoodEntry(entry).enqueue(new Callback<List<FoodEntry>>() {
            @Override
            public void onResponse(Call<List<FoodEntry>> call, Response<List<FoodEntry>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    entry.setId(response.body().get(0).getId());
                }
            }

            @Override
            public void onFailure(Call<List<FoodEntry>> call, Throwable t) {}
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

    private int parseInt(Editable e, int def) {
        if (e == null || e.length() == 0) return def;
        try { return Integer.parseInt(e.toString()); } catch (NumberFormatException ex) { return def; }
    }
    private float parseFloat(Editable e, float def) {
        if (e == null || e.length() == 0) return def;
        try { return Float.parseFloat(e.toString()); } catch (NumberFormatException ex) { return def; }
    }

    private void search(String query) {
        if (TextUtils.isEmpty(query)) return;
        try {
            ApiClient.getApi().getNutrition(ApiClient.API_KEY, query).enqueue(new Callback<CaloriesNinjaApi.NutritionResponse>() {
                @Override
                public void onResponse(Call<CaloriesNinjaApi.NutritionResponse> c, Response<CaloriesNinjaApi.NutritionResponse> r) {
                    if (!isAdded()) return;
                    try {
                        if (r.isSuccessful() && r.body() != null && r.body().items != null) {
                            adapter.update(r.body().items);
                        } else {
                            adapter.update(new ArrayList<>());
                            if (getContext() != null)
                                Toast.makeText(getContext(), "No results found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        adapter.update(new ArrayList<>());
                        if (getContext() != null)
                            Toast.makeText(getContext(), "Search error", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<CaloriesNinjaApi.NutritionResponse> c, Throwable t) {
                    if (!isAdded()) return;
                    adapter.update(new ArrayList<>());
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Search failed - check connection", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            if (getContext() != null)
                Toast.makeText(getContext(), "Search error", Toast.LENGTH_SHORT).show();
            adapter.update(new ArrayList<>());
        }
    }
}
