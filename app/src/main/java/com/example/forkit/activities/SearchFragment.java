package com.example.forkit.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
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
import com.example.forkit.models.SavedFood;
import com.example.forkit.utils.ApiClient;
import com.example.forkit.utils.CaloriesNinjaApi;
import com.example.forkit.utils.CustomFoodHelper;
import com.example.forkit.utils.PrefsHelper;
import com.example.forkit.utils.SavedFoodsStore;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {

    private static final String TAG = "SearchFragment";
    private static final int SEARCH_DEBOUNCE_MS = 350;

    private EditText etSearch;
    private RecyclerView rvResults;
    private SearchResultAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private Call<List<CaloriesNinjaApi.NutritionItem>> inFlight;

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
            adapter = new SearchResultAdapter(
                    new ArrayList<>(),
                    item -> showMealTypePicker((mealType) -> {
                FoodEntry entry = new FoodEntry(
                        item.name, (int) item.caloriesVal(),
                        (float) item.proteinVal(), (float) item.carbsVal(), (float) item.fatVal(),
                        mealType);
                CustomFoodHelper.addFoodEntryAndSync(SearchFragment.this, entry);
                if (getContext() != null) new PrefsHelper(getContext()).onFoodLogged();
                if (getContext() != null) Toast.makeText(getContext(), item.name + " added", Toast.LENGTH_SHORT).show();
                    }),
                    item -> {
                        if (getContext() == null) return;
                        SavedFoodsStore.save(getContext(), new SavedFood(
                                item.name,
                                (int) item.caloriesVal(),
                                (float) item.proteinVal(),
                                (float) item.carbsVal(),
                                (float) item.fatVal()
                        ));
                        Toast.makeText(getContext(), "Saved: " + item.name, Toast.LENGTH_SHORT).show();
                    }
            );
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
                if (s.length() >= 2) scheduleSearch(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

            if (etSearch != null) etSearch.setOnEditorActionListener((v, actionId, event) -> {
                String q = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
                if (!q.isEmpty()) {
                    scheduleSearch(q);
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

    private void scheduleSearch(String query) {
        if (TextUtils.isEmpty(query)) return;
        if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
        pendingSearch = () -> search(query);
        handler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
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
            String q = normalizeQuery(query);
            if (inFlight != null) inFlight.cancel();
            inFlight = ApiClient.getApi().getNutrition(ApiClient.API_KEY, q);
            inFlight.enqueue(new Callback<List<CaloriesNinjaApi.NutritionItem>>() {
                @Override
                public void onResponse(Call<List<CaloriesNinjaApi.NutritionItem>> c, Response<List<CaloriesNinjaApi.NutritionItem>> r) {
                    if (!isAdded()) return;
                    try {
                        if (r.isSuccessful() && r.body() != null) {
                            List<CaloriesNinjaApi.NutritionItem> items = r.body();
                            if (!items.isEmpty() && items.get(0) != null && items.get(0).hasPremiumError()) {
                                adapter.update(new ArrayList<>());
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Search API says premium-only. Replace the API key or switch API.", Toast.LENGTH_LONG).show();
                                }
                                return;
                            }
                            adapter.update(items);
                            if (items.isEmpty() && getContext() != null) {
                                Toast.makeText(getContext(), "Try: 1 " + query + " / 100g " + query, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            adapter.update(new ArrayList<>());
                            if (getContext() != null)
                                Toast.makeText(getContext(), "No results found. Try: 1 " + query + " / 100g " + query, Toast.LENGTH_SHORT).show();
                            logHttpError(r);
                        }
                    } catch (Exception e) {
                        adapter.update(new ArrayList<>());
                        if (getContext() != null)
                            Toast.makeText(getContext(), "Search error", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Search parse error", e);
                    }
                }
                @Override
                public void onFailure(Call<List<CaloriesNinjaApi.NutritionItem>> c, Throwable t) {
                    if (!isAdded()) return;
                    if (c.isCanceled()) return;
                    adapter.update(new ArrayList<>());
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Search failed: " + (t.getMessage() != null ? t.getMessage() : "check connection"), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Search failed", t);
                }
            });
        } catch (Exception e) {
            if (getContext() != null)
                Toast.makeText(getContext(), "Search error", Toast.LENGTH_SHORT).show();
            adapter.update(new ArrayList<>());
            Log.e(TAG, "Search exception", e);
        }
    }

    private static String normalizeQuery(String raw) {
        String q = raw != null ? raw.trim() : "";
        if (q.isEmpty()) return q;
        // API Ninjas nutrition often needs quantity. If user typed no digits, assume 1 serving.
        boolean hasDigit = false;
        for (int i = 0; i < q.length(); i++) {
            if (Character.isDigit(q.charAt(i))) { hasDigit = true; break; }
        }
        if (!hasDigit) return "1 " + q;
        return q;
    }

    private static void logHttpError(Response<?> r) {
        try {
            String body = r.errorBody() != null ? r.errorBody().string() : "";
            Log.w(TAG, "HTTP " + r.code() + " " + body);
        } catch (Exception e) {
            Log.w(TAG, "HTTP " + r.code());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
        pendingSearch = null;
        if (inFlight != null) inFlight.cancel();
        inFlight = null;
    }
}
