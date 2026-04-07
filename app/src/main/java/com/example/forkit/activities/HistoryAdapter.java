package com.example.forkit.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.forkit.R;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    public interface OnDayClick { void onClick(HistoryFragment.DaySummary day); }

    private List<HistoryFragment.DaySummary> days = new ArrayList<>();
    private final OnDayClick listener;

    public HistoryAdapter(List<HistoryFragment.DaySummary> days, OnDayClick listener) {
        if (days != null) this.days = days;
        this.listener = listener;
    }

    public void update(List<HistoryFragment.DaySummary> days) {
        this.days = days != null ? days : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_day, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        HistoryFragment.DaySummary d = days.get(pos);
        h.day.setText(d.label);
        h.kcal.setText(d.totalCalories + " kcal");
        h.water.setText(d.waterMl + " ml");
        h.count.setText(d.meals.size() + " meals");
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onClick(d); });
    }

    @Override
    public int getItemCount() { return days.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView day, kcal, water, count;
        VH(@NonNull View v) {
            super(v);
            day = v.findViewById(R.id.tv_day);
            kcal = v.findViewById(R.id.tv_day_kcal);
            water = v.findViewById(R.id.tv_day_water);
            count = v.findViewById(R.id.tv_day_count);
        }
    }
}

