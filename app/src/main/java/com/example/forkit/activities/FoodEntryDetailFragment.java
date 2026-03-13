package com.example.forkit.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.forkit.MainActivity;
import com.example.forkit.R;
import com.example.forkit.models.FoodEntry;

public class FoodEntryDetailFragment extends Fragment {

    private static final String ARG_ENTRY = "entry";

    public static FoodEntryDetailFragment newInstance(FoodEntry entry) {
        FoodEntryDetailFragment f = new FoodEntryDetailFragment();
        Bundle b = new Bundle();
        b.putString(ARG_ENTRY + "_name", entry.getFoodName());
        b.putInt(ARG_ENTRY + "_cal", entry.getCalories());
        b.putFloat(ARG_ENTRY + "_p", entry.getProtein());
        b.putFloat(ARG_ENTRY + "_c", entry.getCarbs());
        b.putFloat(ARG_ENTRY + "_f", entry.getFat());
        b.putString(ARG_ENTRY + "_meal", entry.getMealType());
        b.putString(ARG_ENTRY + "_ing", entry.getIngredients());
        b.putInt(ARG_ENTRY + "_portion", entry.getPortionG());
        b.putString(ARG_ENTRY + "_img", entry.getImagePath());
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_food_entry_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if (args == null) return;

        String name = args.getString(ARG_ENTRY + "_name", "");
        int cal = args.getInt(ARG_ENTRY + "_cal", 0);
        float p = args.getFloat(ARG_ENTRY + "_p", 0);
        float c = args.getFloat(ARG_ENTRY + "_c", 0);
        float f = args.getFloat(ARG_ENTRY + "_f", 0);
        String meal = args.getString(ARG_ENTRY + "_meal", "");
        String ing = args.getString(ARG_ENTRY + "_ing", "");
        int portion = args.getInt(ARG_ENTRY + "_portion", 0);
        String imgPath = args.getString(ARG_ENTRY + "_img", "");

        ((TextView) view.findViewById(R.id.tv_meal_name)).setText(name);
        ((TextView) view.findViewById(R.id.tv_meal_type)).setText(meal);
        ((TextView) view.findViewById(R.id.tv_meal_ingredients)).setText(ing != null && !ing.isEmpty() ? ing : "—");
        ((TextView) view.findViewById(R.id.tv_meal_kcal)).setText(cal + " kcal");
        ((TextView) view.findViewById(R.id.tv_meal_macros)).setText(String.format("Protein: %.0fg  Carbs: %.0fg  Fat: %.0fg", p, c, f));
        TextView tvPortion = view.findViewById(R.id.tv_portion);
        tvPortion.setText(portion > 0 ? "Portion: " + portion + "g" : "");
        tvPortion.setVisibility(portion > 0 ? View.VISIBLE : View.GONE);

        ImageView iv = view.findViewById(R.id.iv_meal_result);
        if (imgPath != null && !imgPath.isEmpty()) {
            try {
                iv.setImageURI(Uri.parse(imgPath));
            } catch (Exception ignored) {
                iv.setImageResource(R.drawable.natural_food);
            }
        } else {
            iv.setImageResource(R.drawable.natural_food);
        }

        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new FoodLogFragment());
                ((MainActivity) getActivity()).selectNavItem(R.id.nav_log);
            }
        });
    }
}
