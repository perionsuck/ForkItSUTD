package com.example.forkit.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.forkit.MainActivity;
import com.example.forkit.R;
import com.example.forkit.models.FoodEntry;
import com.example.forkit.utils.PrefsHelper;
import com.example.forkit.utils.DateUtils;
import com.example.forkit.utils.FoodStore;
import com.example.forkit.utils.SupabaseApi;
import com.example.forkit.utils.SupabaseClient;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecommendationsFragment extends Fragment {

    private static final class MealRec {
        String name, detail;
        int cal;
        float p, c, f;
        MealRec(String name, String detail, int cal, float p, float c, float f) {
            this.name = name; this.detail = detail; this.cal = cal; this.p = p; this.c = c; this.f = f;
        }
    }

    private static final MealRec[] ALL_MEALS = {
            new MealRec("Greek yoghurt bowl", "1 bowl (450g)  P 26g C 35g F 13g", 320, 26, 35, 13),
            new MealRec("Chicken salad", "1 bowl (450g)  P 13g C 20g F 4g", 450, 13, 29, 4),
            new MealRec("Salmon with broccoli & quinoa", "1 bowl (500g)  P 32g C 40g F 15g", 520, 32, 40, 18),
            new MealRec("Tuna poke bowl", "1 bowl (400g)  P 35g C 25g F 12g", 380, 35, 25, 12),
            new MealRec("Grilled chicken & greens", "1 plate (350g)  P 42g C 5g F 18g", 350, 42, 5, 18),
            new MealRec("Egg white omelette", "1 portion (200g)  P 24g C 4g F 8g", 180, 24, 4, 8),
            new MealRec("Steak & roasted veggies", "1 plate (400g)  P 40g C 15g F 28g", 480, 40, 15, 28),
            new MealRec("Cottage cheese bowl", "1 bowl (300g)  P 28g C 15g F 6g", 220, 28, 15, 6),
            new MealRec("Turkey & avocado wrap", "1 wrap (350g)  P 30g C 45g F 12g", 420, 30, 45, 12),
            new MealRec("Shrimp stir-fry", "1 bowl (400g)  P 38g C 22g F 10g", 360, 38, 22, 10),
    };

    private View rootView;
    private PrefsHelper prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recommendations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            rootView = view;
            prefs = getContext() != null ? new PrefsHelper(getContext()) : null;

            View backBtn = view.findViewById(R.id.btn_back_recommendations);
            if (backBtn != null) backBtn.setOnClickListener(v -> goHome());
            View regenBtn = view.findViewById(R.id.btn_regenerate);
            if (regenBtn != null) regenBtn.setOnClickListener(v -> regenerateMeals());

            Chip chipMoreProtein = view.findViewById(R.id.chip_more_protein);
            Chip chipLessFat = view.findViewById(R.id.chip_less_fat);
            Chip chipNoCarbs = view.findViewById(R.id.chip_no_carbs);
            Chip chipLessCarbs = view.findViewById(R.id.chip_less_carbs);
            if (chipMoreProtein != null) chipMoreProtein.setOnClickListener(v -> regenerateMeals());
            if (chipLessFat != null) chipLessFat.setOnClickListener(v -> regenerateMeals());
            if (chipNoCarbs != null) chipNoCarbs.setOnClickListener(v -> regenerateMeals());
            if (chipLessCarbs != null) chipLessCarbs.setOnClickListener(v -> regenerateMeals());

            updateGreeting();
            regenerateMeals();
        } catch (Exception e) {
            if (getContext() != null) android.widget.Toast.makeText(getContext(), "Error loading recommendations", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void updateGreeting() {
        String greeting = "Good day!";
        if (prefs != null) {
            String name = prefs.getUserName();
            String dg = DateUtils.getGreeting();
            greeting = (dg != null ? dg : "Good day") + (name != null && !name.isEmpty() ? ", " + name + "!" : "!");
        }
        TextView tv = rootView != null ? rootView.findViewById(R.id.tv_recommendations_greeting) : null;
        if (tv != null) tv.setText(greeting);
    }

    private void regenerateMeals() {
        if (rootView == null) return;
        try {
        Chip c1 = rootView.findViewById(R.id.chip_more_protein);
        Chip c2 = rootView.findViewById(R.id.chip_less_fat);
        Chip c3 = rootView.findViewById(R.id.chip_no_carbs);
        Chip c4 = rootView.findViewById(R.id.chip_less_carbs);
        boolean moreProtein = c1 != null && c1.isChecked();
        boolean lessFat = c2 != null && c2.isChecked();
        boolean noCarbs = c3 != null && c3.isChecked();
        boolean lessCarbs = c4 != null && c4.isChecked();

        List<MealRec> pool = new ArrayList<>();
        for (MealRec m : ALL_MEALS) {
            if (moreProtein && m.p < 25) continue;
            if (lessFat && m.f > 15) continue;
            if (noCarbs && m.c > 10) continue;
            if (lessCarbs && m.c > 25) continue;
            pool.add(m);
        }
        if (pool.isEmpty()) pool = new ArrayList<>(java.util.Arrays.asList(ALL_MEALS));
        Collections.shuffle(pool, new Random());

        String dietary = prefs != null ? prefs.getDietaryPrefs() : "";
        if (dietary != null) dietary = dietary.toLowerCase();
        boolean vegan = dietary != null && dietary.contains("vegan");
        boolean vegetarian = dietary != null && dietary.contains("vegetarian");

        List<MealRec> filtered = new ArrayList<>();
        for (MealRec m : pool) {
            if (vegan && (m.name.toLowerCase().contains("chicken") || m.name.toLowerCase().contains("salmon") || m.name.toLowerCase().contains("tuna") || m.name.toLowerCase().contains("turkey") || m.name.toLowerCase().contains("steak") || m.name.toLowerCase().contains("shrimp") || m.name.toLowerCase().contains("egg"))) continue;
            if (vegetarian && (m.name.toLowerCase().contains("chicken") || m.name.toLowerCase().contains("salmon") || m.name.toLowerCase().contains("tuna") || m.name.toLowerCase().contains("turkey") || m.name.toLowerCase().contains("steak") || m.name.toLowerCase().contains("shrimp"))) continue;
            filtered.add(m);
        }
        if (filtered.isEmpty()) filtered = pool;

        TextView noMatch = rootView.findViewById(R.id.tv_no_dietary_match);
        View m1 = rootView.findViewById(R.id.meal_rec_1);
        View m2 = rootView.findViewById(R.id.meal_rec_2);
        View m3 = rootView.findViewById(R.id.meal_rec_3);

        if (filtered.isEmpty() || (vegan && filtered.size() < 3)) {
            if (noMatch != null) noMatch.setVisibility(View.VISIBLE);
            if (m1 != null) m1.setVisibility(View.GONE);
            if (m2 != null) m2.setVisibility(View.GONE);
            if (m3 != null) m3.setVisibility(View.GONE);
        } else {
            if (noMatch != null) noMatch.setVisibility(View.GONE);
            bindMeal(m1, filtered.size() > 0 ? filtered.get(0) : null);
            bindMeal(m2, filtered.size() > 1 ? filtered.get(1) : null);
            bindMeal(m3, filtered.size() > 2 ? filtered.get(2) : null);
        }
        } catch (Exception ignored) {}
    }

    private void bindMeal(View card, MealRec m) {
        if (card == null) return;
        if (m == null) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        int nameId = card.getId() == R.id.meal_rec_1 ? R.id.tv_meal_1_name : card.getId() == R.id.meal_rec_2 ? R.id.tv_meal_2_name : R.id.tv_meal_3_name;
        int detailId = card.getId() == R.id.meal_rec_1 ? R.id.tv_meal_1_detail : card.getId() == R.id.meal_rec_2 ? R.id.tv_meal_2_detail : R.id.tv_meal_3_detail;
        int kcalId = card.getId() == R.id.meal_rec_1 ? R.id.tv_meal_1_kcal : card.getId() == R.id.meal_rec_2 ? R.id.tv_meal_2_kcal : R.id.tv_meal_3_kcal;
        ((TextView) rootView.findViewById(nameId)).setText(m.name);
        ((TextView) rootView.findViewById(detailId)).setText(m.detail);
        ((TextView) rootView.findViewById(kcalId)).setText(m.cal + " kcal");
        card.setOnClickListener(v -> showMealTypePickerAndAdd(m.name, m.cal, m.p, m.c, m.f, m.detail, 0));
    }

    private void goHome() {
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadFragment(new HomeFragment());
            ((MainActivity) getActivity()).selectNavItem(R.id.nav_home);
        }
    }

    private void showMealTypePickerAndAdd(String name, int cal, float p, float c, float f) {
        showMealTypePickerAndAdd(name, cal, p, c, f, null, 0);
    }

    private void showMealTypePickerAndAdd(String name, int cal, float p, float c, float f, String detail, int portion) {
        if (getContext() == null) return;
        View v = LayoutInflater.from(new android.view.ContextThemeWrapper(getContext(), R.style.AlertDialogLight)).inflate(R.layout.dialog_meal_type, null);
        AlertDialog d = new AlertDialog.Builder(getContext(), R.style.AlertDialogLight).setView(v).create();
        String[] meals = {"Breakfast", "Lunch", "Dinner", "Tea", "Snack"};
        for (String meal : meals) {
            int id = meal.equals("Breakfast") ? R.id.option_breakfast : meal.equals("Lunch") ? R.id.option_lunch : meal.equals("Dinner") ? R.id.option_dinner : meal.equals("Tea") ? R.id.option_tea : R.id.option_snack;
            v.findViewById(id).setOnClickListener(x -> {
                d.dismiss();
                FoodEntry entry = (detail != null || portion > 0)
                        ? new FoodEntry(name, cal, p, c, f, meal, detail != null ? detail : "", portion)
                        : new FoodEntry(name, cal, p, c, f, meal);
                addEntryAndSync(entry);
                if (getContext() != null) new PrefsHelper(getContext()).onFoodLogged();
                goHome();
            });
        }
        d.show();
    }

    private void addEntryAndSync(FoodEntry entry) {
        FoodStore.add(entry);
        if (getContext() == null) return;

        PrefsHelper prefs = new PrefsHelper(getContext());
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
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).refreshFoodEntriesFromCloud();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<FoodEntry>> call, Throwable t) {}
        });
    }
}
