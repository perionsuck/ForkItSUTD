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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.forkit.MainActivity;
import com.example.forkit.R;
import com.example.forkit.models.FoodEntry;
import com.example.forkit.models.SavedFood;
import com.example.forkit.utils.CustomFoodHelper;
import com.example.forkit.utils.PrefsHelper;
import com.example.forkit.utils.SavedFoodsStore;

import java.util.ArrayList;
import java.util.List;

public class SavedFoodsFragment extends Fragment {

    private SavedFoodsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved_foods, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_saved_back).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new SettingsFragment());
                ((MainActivity) getActivity()).selectNavItem(R.id.nav_profile);
            }
        });

        RecyclerView rv = view.findViewById(R.id.rv_saved_foods);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SavedFoodsAdapter(new ArrayList<>(),
                f -> showMealTypePicker(mealType -> {
                    FoodEntry e = new FoodEntry(f.name, f.calories, f.protein, f.carbs, f.fat, mealType);
                    CustomFoodHelper.addFoodEntryAndSync(SavedFoodsFragment.this, e);
                    new PrefsHelper(requireContext()).onFoodLogged();
                }),
                f -> {
                    SavedFoodsStore.remove(requireContext(), f.name);
                    refresh();
                });
        rv.setAdapter(adapter);
        refresh();
    }

    private void refresh() {
        List<SavedFood> list = SavedFoodsStore.list(requireContext());
        adapter.update(list);
        TextView empty = getView() != null ? getView().findViewById(R.id.tv_saved_empty) : null;
        if (empty != null) empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
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
}

