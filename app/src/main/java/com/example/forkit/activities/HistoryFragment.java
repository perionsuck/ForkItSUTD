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
import com.example.forkit.utils.FoodStore;
import com.example.forkit.utils.PrefsHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class HistoryFragment extends Fragment {

    private HistoryAdapter adapter;
    private PrefsHelper prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new PrefsHelper(requireContext());

        view.findViewById(R.id.btn_history_back).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new SettingsFragment());
                ((MainActivity) getActivity()).selectNavItem(R.id.nav_profile);
            }
        });

        RecyclerView rv = view.findViewById(R.id.rv_history);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoryAdapter(new ArrayList<>(), this::showDayDetails);
        rv.setAdapter(adapter);
        refresh();
    }

    private void refresh() {
        List<FoodEntry> list = FoodStore.getEntriesView();
        Map<String, DaySummary> map = new HashMap<>();

        for (FoodEntry e : list) {
            if (e == null) continue;
            String key = dayKeySG(e.getTimestamp());
            DaySummary s = map.get(key);
            if (s == null) {
                s = new DaySummary(key);
                map.put(key, s);
            }
            s.totalCalories += e.getCalories();
            s.meals.add(e);
        }

        ArrayList<DaySummary> days = new ArrayList<>(map.values());
        Collections.sort(days, (a, b) -> Long.compare(b.dayStartMs, a.dayStartMs));

        // Fill in water history values
        for (DaySummary d : days) {
            d.waterMl = prefs.getWaterForDateKey(d.key);
        }

        adapter.update(days);

        TextView empty = getView() != null ? getView().findViewById(R.id.tv_history_empty) : null;
        if (empty != null) empty.setVisibility(days.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showDayDetails(DaySummary d) {
        if (getContext() == null || d == null) return;
        StringBuilder sb = new StringBuilder();
        ArrayList<FoodEntry> meals = new ArrayList<>(d.meals);
        meals.sort(Comparator.comparingLong(FoodEntry::getTimestamp).reversed());
        for (FoodEntry e : meals) {
            sb.append("• ").append(e.getFoodName()).append(" — ").append(e.getCalories()).append(" kcal\n");
        }
        if (sb.length() == 0) sb.append("No meals logged.");

        new AlertDialog.Builder(getContext(), R.style.AlertDialogLight)
                .setTitle(d.label + " (" + d.totalCalories + " kcal)")
                .setMessage(sb.toString().trim())
                .setPositiveButton("OK", (x, y) -> {})
                .show();
    }

    private static String dayKeySG(long ts) {
        TimeZone tz = TimeZone.getTimeZone("Asia/Singapore");
        Calendar c = Calendar.getInstance(tz);
        c.setTimeInMillis(ts);
        return c.get(Calendar.YEAR) + "-" + c.get(Calendar.DAY_OF_YEAR);
    }

    static final class DaySummary {
        final String key;
        final long dayStartMs;
        final String label;
        int totalCalories = 0;
        int waterMl = 0;
        final ArrayList<FoodEntry> meals = new ArrayList<>();

        DaySummary(String key) {
            this.key = key;
            String[] parts = key.split("-");
            int y = 1970;
            int doy = 1;
            try {
                y = Integer.parseInt(parts[0]);
                doy = Integer.parseInt(parts[1]);
            } catch (Exception ignored) {}
            TimeZone tz = TimeZone.getTimeZone("Asia/Singapore");
            Calendar c = Calendar.getInstance(tz);
            c.set(Calendar.YEAR, y);
            c.set(Calendar.DAY_OF_YEAR, doy);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            dayStartMs = c.getTimeInMillis();
            label = String.format(Locale.getDefault(), "%1$tb %1$td", c.getTime());
        }
    }
}

