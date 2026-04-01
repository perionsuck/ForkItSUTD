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
import com.example.forkit.utils.CustomFoodHelper;
import com.example.forkit.utils.PrefsHelper;

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
                CustomFoodHelper.addFoodEntryAndSync(SearchFragment.this, entry);
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
            if (btnCustom != null) btnCustom.setOnClickListener(v -> CustomFoodHelper.show(SearchFragment.this));
        } catch (Exception e) {
            if (getContext() != null) Toast.makeText(getContext(), "Error loading search", Toast.LENGTH_SHORT).show();
        }
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
