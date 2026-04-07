package com.example.forkit.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.forkit.R;
import com.example.forkit.models.SavedFood;

import java.util.ArrayList;
import java.util.List;

public class SavedFoodsAdapter extends RecyclerView.Adapter<SavedFoodsAdapter.VH> {

    public interface OnAddListener { void onAdd(SavedFood f); }
    public interface OnDeleteListener { void onDelete(SavedFood f); }

    private List<SavedFood> items = new ArrayList<>();
    private final OnAddListener addListener;
    private final OnDeleteListener deleteListener;

    public SavedFoodsAdapter(List<SavedFood> items, OnAddListener addListener, OnDeleteListener deleteListener) {
        if (items != null) this.items = items;
        this.addListener = addListener;
        this.deleteListener = deleteListener;
    }

    public void update(List<SavedFood> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_food, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        SavedFood f = items.get(pos);
        h.name.setText(f.name != null ? f.name : "—");
        h.macros.setText(String.format("P %.0fg C %.0fg F %.0fg", f.protein, f.carbs, f.fat));
        h.kcal.setText(f.calories + " kcal");
        h.add.setOnClickListener(v -> { if (addListener != null) addListener.onAdd(f); });
        h.del.setOnClickListener(v -> { if (deleteListener != null) deleteListener.onDelete(f); });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, macros, kcal;
        ImageView add, del;
        VH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.tv_food_name);
            macros = v.findViewById(R.id.tv_food_macros);
            kcal = v.findViewById(R.id.tv_food_kcal);
            add = v.findViewById(R.id.btn_add_food);
            del = v.findViewById(R.id.btn_delete_food);
        }
    }
}

