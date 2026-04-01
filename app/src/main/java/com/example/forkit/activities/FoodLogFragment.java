package com.example.forkit.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.forkit.MainActivity;
import com.example.forkit.R;
import com.example.forkit.adapters.FoodLogAdapter;
import com.example.forkit.models.FoodEntry;
import com.example.forkit.utils.FoodSorter;
import com.example.forkit.utils.SupabaseApi;
import com.example.forkit.utils.SupabaseClient;

public class FoodLogFragment extends Fragment {

    private FoodLogAdapter adapter;
    private TextView tvEmptyLog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_food_log, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvEmptyLog = view.findViewById(R.id.tv_empty_log);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_food_log);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        view.findViewById(R.id.btn_log_menu).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).openDrawer();
        });

        adapter = new FoodLogAdapter(entry -> {
            HomeFragment.foodEntries.remove(entry);
            deleteFromCloud(entry);
            updateEntries();
        });
        adapter.setOnItemClickListener(entry -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(FoodEntryDetailFragment.newInstance(entry));
                ((MainActivity) getActivity()).selectNavItem(R.id.nav_log);
            }
        });
        recyclerView.setAdapter(adapter);
        updateEntries();

        Spinner sortSpinner = view.findViewById(R.id.spinner_sort);
        String[] sortOptions = {
                "Newest First",
                "Oldest First",
                "Most Calories",
                "Fewest Calories",
                "Meal Type",
                "A to Z",
                "Z to A"
        };
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.item_spinner_dropdown, sortOptions);
        spinnerAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        sortSpinner.setAdapter(spinnerAdapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
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

    private void updateEntries() {
        adapter.setEntries(HomeFragment.foodEntries);
        if (tvEmptyLog != null) {
            tvEmptyLog.setVisibility(HomeFragment.foodEntries.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void deleteFromCloud(FoodEntry entry) {
        if (entry == null || entry.getId() <= 0) return;
        SupabaseApi api = SupabaseClient.getClient().create(SupabaseApi.class);
        api.deleteFoodEntry("eq." + entry.getId()).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {}
            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) updateEntries();
    }
}