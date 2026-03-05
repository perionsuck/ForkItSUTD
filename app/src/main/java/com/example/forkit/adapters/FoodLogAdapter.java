package com.example.forkit.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.forkit.R;
import com.example.forkit.models.FoodEntry;
import com.example.forkit.utils.FoodSorter;

import java.util.ArrayList;
import java.util.List;

public class FoodLogAdapter extends RecyclerView.Adapter<FoodLogAdapter.ViewHolder> {

    private List<FoodEntry> entries = new ArrayList<>();
    private FoodSorter.SortBy currentSort = FoodSorter.SortBy.TIME_NEWEST;
    private OnDeleteClickListener deleteListener;

    public interface OnDeleteClickListener {
        void onDelete(FoodEntry entry);
    }

    public FoodLogAdapter(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    public FoodSorter.SortBy currentSort() { return currentSort; }

    public void setEntries(List<FoodEntry> newEntries) {
        this.entries = FoodSorter.sort(newEntries, currentSort);
        notifyDataSetChanged();
    }

    public void sortBy(FoodSorter.SortBy sortBy) {
        this.currentSort = sortBy;
        this.entries = FoodSorter.sort(entries, sortBy);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodEntry entry = entries.get(position);
        holder.tvFoodName.setText(entry.getFoodName());
        holder.tvCalories.setText(entry.getCalories() + " kcal");
        holder.tvMealType.setText(entry.getMealType());
        holder.tvMacros.setText(String.format(
                "P: %.0fg  C: %.0fg  F: %.0fg",
                entry.getProtein(), entry.getCarbs(), entry.getFat()));

        int color;
        switch (entry.getMealType() != null ? entry.getMealType() : "Snack") {
            case "Breakfast": color = 0xFFFFA726; break;
            case "Lunch":     color = 0xFF66BB6A; break;
            case "Dinner":    color = 0xFF5C6BC0; break;
            default:          color = 0xFFEF5350; break;
        }
        holder.mealTag.setBackgroundColor(color);

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(entry);
        });
    }

    @Override
    public int getItemCount() { return entries.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFoodName, tvCalories, tvMealType, tvMacros;
        View mealTag;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFoodName = itemView.findViewById(R.id.tv_food_name);
            tvCalories = itemView.findViewById(R.id.tv_calories);
            tvMealType = itemView.findViewById(R.id.tv_meal_type);
            tvMacros   = itemView.findViewById(R.id.tv_macros);
            mealTag    = itemView.findViewById(R.id.meal_color_tag);
            btnDelete  = itemView.findViewById(R.id.btn_delete);
        }
    }
}