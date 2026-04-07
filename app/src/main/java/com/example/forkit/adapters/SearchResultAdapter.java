package com.example.forkit.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.forkit.R;
import com.example.forkit.utils.CaloriesNinjaApi;

import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.VH> {

    private List<CaloriesNinjaApi.NutritionItem> items;
    private final OnAddListener addListener;
    private final OnSaveListener saveListener;

    public interface OnAddListener {
        void onAdd(CaloriesNinjaApi.NutritionItem item);
    }

    public interface OnSaveListener {
        void onSave(CaloriesNinjaApi.NutritionItem item);
    }

    public SearchResultAdapter(List<CaloriesNinjaApi.NutritionItem> items, OnAddListener addListener, OnSaveListener saveListener) {
        this.items = items;
        this.addListener = addListener;
        this.saveListener = saveListener;
    }

    public void update(List<CaloriesNinjaApi.NutritionItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        CaloriesNinjaApi.NutritionItem i = items.get(pos);
        h.name.setText(cap(i.name));
        h.macros.setText(String.format("P %.0fg C %.0fg F %.0fg", i.proteinVal(), i.carbsVal(), i.fatVal()));
        h.kcal.setText((int) i.caloriesVal() + " kcal");
        h.add.setOnClickListener(v -> addListener.onAdd(i));
        if (h.save != null) {
            h.save.setOnClickListener(v -> {
                if (saveListener != null) saveListener.onSave(i);
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, macros, kcal;
        ImageView add, save;

        VH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.tv_food_name);
            macros = v.findViewById(R.id.tv_food_macros);
            kcal = v.findViewById(R.id.tv_food_kcal);
            add = v.findViewById(R.id.btn_add_food);
            save = v.findViewById(R.id.btn_save_food);
        }
    }
}
