package com.example.forkit.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.forkit.models.SavedFood;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class SavedFoodsStore {

    private SavedFoodsStore() {}

    private static final String PREFS = "forkit_prefs";
    private static final String KEY = "saved_foods_json";
    private static final Gson gson = new Gson();
    private static final Type LIST_TYPE = new TypeToken<ArrayList<SavedFood>>() {}.getType();

    public static List<SavedFood> list(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = p.getString(KEY, "[]");
        try {
            List<SavedFood> items = gson.fromJson(json, LIST_TYPE);
            return items != null ? items : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void save(Context ctx, SavedFood f) {
        if (f == null || f.name == null || f.name.trim().isEmpty()) return;
        ArrayList<SavedFood> items = new ArrayList<>(list(ctx));
        // de-dup by name (case-insensitive)
        for (SavedFood s : items) {
            if (s != null && s.name != null && s.name.equalsIgnoreCase(f.name)) {
                s.calories = f.calories;
                s.protein = f.protein;
                s.carbs = f.carbs;
                s.fat = f.fat;
                persist(ctx, items);
                return;
            }
        }
        items.add(0, f);
        persist(ctx, items);
    }

    public static void remove(Context ctx, String name) {
        if (name == null || name.trim().isEmpty()) return;
        ArrayList<SavedFood> items = new ArrayList<>(list(ctx));
        for (Iterator<SavedFood> it = items.iterator(); it.hasNext();) {
            SavedFood s = it.next();
            if (s != null && s.name != null && s.name.equalsIgnoreCase(name)) it.remove();
        }
        persist(ctx, items);
    }

    private static void persist(Context ctx, List<SavedFood> items) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putString(KEY, gson.toJson(items != null ? items : new ArrayList<>())).apply();
    }
}

