package com.example.forkit.activities;

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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.forkit.R;
import com.example.forkit.MainActivity;
import com.example.forkit.adapters.SearchResultAdapter;
import com.example.forkit.utils.ApiClient;
import com.example.forkit.utils.CaloriesNinjaApi;
import com.example.forkit.utils.GeminiApi;
import com.example.forkit.utils.CustomFoodHelper;
import com.example.forkit.utils.PrefsHelper;
import com.example.forkit.models.FoodEntry;
import com.example.forkit.models.SavedFood;
import com.example.forkit.utils.SavedFoodsStore;
import com.google.gson.JsonPrimitive;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {

    private static final String TAG = "SearchFragment";
    private static final int SEARCH_DEBOUNCE_MS = 800;

    private EditText etSearch;
    private RecyclerView rvResults;
    private SearchResultAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private Call<List<CaloriesNinjaApi.NutritionItem>> inFlight;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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

                    item -> showMealTypePicker(mealType -> {
                        FoodEntry entry = new FoodEntry(
                                item.name, (int) item.caloriesVal(),
                                (float) item.proteinVal(), (float) item.carbsVal(),
                                (float) item.fatVal(), mealType);
                        CustomFoodHelper.addFoodEntryAndSync(SearchFragment.this, entry);
                        if (getContext() != null)
                            new PrefsHelper(getContext()).onFoodLogged();
                        if (getContext() != null)
                            Toast.makeText(getContext(),
                                    item.name + " added", Toast.LENGTH_SHORT).show();
                    }),

                    item -> {
                        if (getContext() == null) return;
                        SavedFoodsStore.save(getContext(), new SavedFood(
                                item.name,
                                (int) item.caloriesVal(),
                                (float) item.proteinVal(),
                                (float) item.carbsVal(),
                                (float) item.fatVal()));
                        Toast.makeText(getContext(),
                                "Saved: " + item.name, Toast.LENGTH_SHORT).show();
                    }
            );

            rvResults.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvResults.setAdapter(adapter);

            View btnMenu = view.findViewById(R.id.btn_search_menu);
            if (btnMenu != null) btnMenu.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity)
                    ((MainActivity) getActivity()).openDrawer();
            });

            if (etSearch != null) etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() >= 4) scheduleSearch(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            if (etSearch != null) etSearch.setOnEditorActionListener((v, actionId, event) -> {
                String q = etSearch.getText() != null
                        ? etSearch.getText().toString().trim() : "";
                if (!q.isEmpty()) {
                    scheduleSearch(q);
                    return true;
                }
                return false;
            });

            View btnCustom = view.findViewById(R.id.btn_add_custom);
            if (btnCustom != null)
                btnCustom.setOnClickListener(v ->
                        CustomFoodHelper.show(SearchFragment.this));

        } catch (Exception e) {
            if (getContext() != null)
                Toast.makeText(getContext(),
                        "Error loading search", Toast.LENGTH_SHORT).show();
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


    private void scheduleSearch(String query) {
        if (TextUtils.isEmpty(query)) return;
        if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
        pendingSearch = () -> search(query);
        handler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
    }


    private void showMealTypePicker(java.util.function.Consumer<String> onSelected) {
        if (getContext() == null) return;
        View v = LayoutInflater.from(
                        new android.view.ContextThemeWrapper(getContext(), R.style.AlertDialogLight))
                .inflate(R.layout.dialog_meal_type, null);
        AlertDialog d = new AlertDialog.Builder(getContext(), R.style.AlertDialogLight)
                .setView(v).create();
        String[] meals = {"Breakfast", "Lunch", "Dinner", "Tea", "Snack"};
        for (String meal : meals) {
            int id = meal.equals("Breakfast") ? R.id.option_breakfast
                    : meal.equals("Lunch") ? R.id.option_lunch
                    : meal.equals("Dinner") ? R.id.option_dinner
                    : meal.equals("Tea") ? R.id.option_tea
                    : R.id.option_snack;
            v.findViewById(id).setOnClickListener(x -> {
                d.dismiss();
                onSelected.accept(meal);
            });
        }
        d.show();
    }


    // primary search using CaloriesNinjaApi first then use Gemini as a fallback if empty or error


    private void search(String query) {
        if (TextUtils.isEmpty(query)) return;

        try {
            String normalizedQuery = normalizeQuery(query);
            Log.w(TAG, "searching for:" + normalizedQuery);

            if (inFlight != null) inFlight.cancel();

            inFlight = ApiClient.getApi().getNutrition(ApiClient.API_KEY, normalizedQuery);
            inFlight.enqueue(new Callback<List<CaloriesNinjaApi.NutritionItem>>() {

                @Override
                public void onResponse(Call<List<CaloriesNinjaApi.NutritionItem>> c,
                                       Response<List<CaloriesNinjaApi.NutritionItem>> r) {
                    if (!isAdded()) return;

                    try {
                        if (r.isSuccessful() && r.body() != null) {
                            List<CaloriesNinjaApi.NutritionItem> items = r.body();

                            // Premium-only gate → fall back to Gemini
                            if (!items.isEmpty() && items.get(0) != null
                                    && items.get(0).hasPremiumError()) {
                                adapter.update(new ArrayList<>());
                                Log.w(TAG, "CaloriesNinja premium wall – falling back to Gemini");
                                searchWithGemini(normalizedQuery);
                                return;
                            }

                            // Good results
                            if (!items.isEmpty()) {
                                adapter.update(items);
                                return;
                            }

                            // Empty → Gemini fallback
                            Log.i(TAG, "CaloriesNinja returned 0 results – trying Gemini");
                            searchWithGemini(normalizedQuery);

                        } else {
                            Log.w(TAG, "CaloriesNinja HTTP error – trying Gemini");
                            logHttpError(r);
                            searchWithGemini(normalizedQuery);
                        }
                    } catch (Exception e) {
                        adapter.update(new ArrayList<>());
                        if (getContext() != null)
                            Toast.makeText(getContext(),
                                    "Search error", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Search parse error", e);
                    }
                }

                @Override
                public void onFailure(Call<List<CaloriesNinjaApi.NutritionItem>> c,
                                      Throwable t) {
                    if (!isAdded()) return;
                    if (c.isCanceled()) return;
                    Log.w(TAG, "CaloriesNinja network failure – trying Gemini", t);
                    searchWithGemini(normalizedQuery);
                }
            });

        } catch (Exception e) {
            if (getContext() != null)
                Toast.makeText(getContext(),
                        "Search error", Toast.LENGTH_SHORT).show();
            adapter.update(new ArrayList<>());
            Log.e(TAG, "Search exception", e);
        }
    }

    // Gemini fallback – uses existing GeminiApi + ApiClient directly

    private void searchWithGemini(String query) {
        if (!isAdded()) return;

        if (getContext() != null) {
            Toast.makeText(getContext(),
                    "Searching with AI for \"" + query + "\"…",
                    Toast.LENGTH_SHORT).show();
        }

        String prompt = "You are a nutrition database. "
                + "For the food item of 1 \"" + query + "\", return a JSON array of matching foods. "
                + "Each object in the array must have exactly these fields:\n"
                + "- \"name\": string (food name with serving size, e.g. \"1 cup rice (200g)\")\n"
                + "- \"calories\": number (kcal)\n"
                + "- \"protein_g\": number (grams)\n"
                + "- \"carbohydrates_total_g\": number (grams)\n"
                + "- \"fat_total_g\": number (grams)\n"
                + "- \"serving_size_g\": number (grams)\n\n"
                + "Return between 1 and 5 common variations/serving sizes. "
                + "If the food includes a local or regional dish, still estimate values. "
                + "Return ONLY the JSON array, no markdown, no explanation.";
        Log.w(TAG, "the prompt is:" + prompt);

        // Build request using existing GeminiApi model classes
        GeminiApi.Part part = new GeminiApi.Part(prompt);
        GeminiApi.Content content = new GeminiApi.Content();
        content.parts = new GeminiApi.Part[]{part};

        GeminiApi.GenerationConfig config = new GeminiApi.GenerationConfig();

        GeminiApi.GeminiRequest request = new GeminiApi.GeminiRequest(
                new GeminiApi.Content[]{content}, config);

        Call<GeminiApi.GeminiResponse> call =
                ApiClient.getGeminiApi().generateContent(ApiClient.GEMINI_API_KEY, request);

        call.enqueue(new Callback<GeminiApi.GeminiResponse>() {
            @Override
            public void onResponse(Call<GeminiApi.GeminiResponse> c,
                                   Response<GeminiApi.GeminiResponse> r) {
                if (!isAdded()) return;

                try {
                    if (r.isSuccessful() && r.body() != null) {
                        List<CaloriesNinjaApi.NutritionItem> items =
                                parseGeminiResponse(r.body());
                        if (!items.isEmpty()) {
                            adapter.update(items);
                        } else {
                            adapter.update(new ArrayList<>());
                            showNoResultsForGemini(query);
                        }
                    } else {
                        adapter.update(new ArrayList<>());
                        showNoResultsForGemini(query);
                        String err = r.errorBody() != null ? r.errorBody().string() : "";
                        Log.e(TAG, "Gemini HTTP error: " + r.code() + " " + err);
                    }
                } catch (Exception e) {
                    adapter.update(new ArrayList<>());
                    showNoResultsForGemini(query);
                    Log.e(TAG, "Gemini parse error", e);
                }
            }

            @Override
            public void onFailure(Call<GeminiApi.GeminiResponse> c, Throwable t) {
                if (!isAdded()) return;
                adapter.update(new ArrayList<>());
                showNoResultsForGemini(query);
                Log.e(TAG, "Gemini network failure", t);
            }
        });
    }


    // parse Gemini response into NutritionItem list


    private List<CaloriesNinjaApi.NutritionItem> parseGeminiResponse(
            GeminiApi.GeminiResponse response) {

        List<CaloriesNinjaApi.NutritionItem> results = new ArrayList<>();

        try {
            if (response.candidates == null || response.candidates.length == 0)
                return results;

            GeminiApi.Candidate candidate = response.candidates[0];
            if (candidate.content == null || candidate.content.parts == null
                    || candidate.content.parts.length == 0)
                return results;

            String text = candidate.content.parts[0].text;
            if (text == null || text.isEmpty()) return results;

            // Strip markdown fences if present
            text = text.trim();
            if (text.startsWith("```json")) text = text.substring(7);
            else if (text.startsWith("```")) text = text.substring(3);
            if (text.endsWith("```")) text = text.substring(0, text.length() - 3);
            text = text.trim();

            JSONArray foodArray = new JSONArray(text);

            for (int i = 0; i < foodArray.length(); i++) {
                JSONObject food = foodArray.getJSONObject(i);

                CaloriesNinjaApi.NutritionItem item = new CaloriesNinjaApi.NutritionItem();

                String foodName = food.optString("name", "Unknown food");
                item.name = foodName + " (AI est.)";

                // NutritionItem fields are JsonElement → wrap with JsonPrimitive
                item.calories = new JsonPrimitive(food.optDouble("calories", 0));
                item.protein_g = new JsonPrimitive(food.optDouble("protein_g", 0));
                item.carbohydrates_total_g = new JsonPrimitive(food.optDouble("carbohydrates_total_g", 0));
                item.fat_total_g = new JsonPrimitive(food.optDouble("fat_total_g", 0));
                item.serving_size_g = new JsonPrimitive(food.optDouble("serving_size_g", 100));

                results.add(item);
            }

            Log.i(TAG, "Gemini returned " + results.size() + " food items");

        } catch (Exception e) {
            Log.e(TAG, "Gemini JSON parse error", e);
        }

        return results;
    }

    // Helpers


    private void showNoResultsForGemini(String query) {
        if (getContext() != null)
            Toast.makeText(getContext(),
                    "No results found for:" + query,
                    Toast.LENGTH_SHORT).show();
    }

    private static String normalizeQuery(String raw) {
        String query = raw != null ? raw.trim() : "";
        if (query.isEmpty()) return query;
        boolean hasDigit = false;
        for (int i = 0; i < query.length(); i++) {
            if (Character.isDigit(query.charAt(i))) {
                hasDigit = true;
                break;
            }
        }
        return query;
    }

    private static void logHttpError(Response<?> r) {
        try {
            String body = r.errorBody() != null ? r.errorBody().string() : "";
            Log.w(TAG, "HTTP " + r.code() + " " + body);
        } catch (Exception e) {
            Log.w(TAG, "HTTP " + r.code());
        }
    }
}