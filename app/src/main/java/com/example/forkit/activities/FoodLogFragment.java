package com.example.forkit.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.forkit.R;
import com.example.forkit.adapters.FoodLogAdapter;
import com.example.forkit.utils.FoodSorter;

public class FoodLogFragment extends Fragment {

    private FoodLogAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_food_log, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_food_log);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new FoodLogAdapter(entry -> {
            HomeFragment.foodEntries.remove(entry);
            adapter.setEntries(HomeFragment.foodEntries);
        });
        recyclerView.setAdapter(adapter);
        adapter.setEntries(HomeFragment.foodEntries);

        // Sort spinner
        Spinner sortSpinner = view.findViewById(R.id.spinner_sort);
        String[] sortOptions = {
                "⏱ Newest First",
                "⏱ Oldest First",
                "🔥 Most Calories",
                "🥗 Fewest Calories",
                "🍽️ Meal Type",
                "🔤 A → Z",
                "🔤 Z → A"
        };
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, sortOptions);
        sortSpinner.setAdapter(spinnerAdapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                FoodSorter.SortBy sort;
                switch (position) {
                    case 1:  sort = FoodSorter.SortBy.TIME_OLDEST;   break;
                    case 2:  sort = FoodSorter.SortBy.CALORIES_HIGH; break;
                    case 3:  sort = FoodSorter.SortBy.CALORIES_LOW;  break;
                    case 4:  sort = FoodSorter.SortBy.MEAL_TYPE;     break;
                    case 5:  sort = FoodSorter.SortBy.NAME_AZ;       break;
                    case 6:  sort = FoodSorter.SortBy.NAME_ZA;       break;
                    default: sort = FoodSorter.SortBy.TIME_NEWEST;   break;
                }
                adapter.sortBy(sort);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) adapter.setEntries(HomeFragment.foodEntries);
    }
}