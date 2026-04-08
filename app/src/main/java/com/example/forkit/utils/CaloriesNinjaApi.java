package com.example.forkit.utils;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface CaloriesNinjaApi {

    @GET("v1/nutrition")
    Call<List<NutritionItem>> getNutrition(
            @Header("X-Api-Key") String apiKey,
            @Query("query") String query

    );

    class NutritionItem {
        public String name;
        public JsonElement calories;
        public JsonElement protein_g;
        @SerializedName(value = "carbohydrates_total_g", alternate = {"total_carbohydrates_g"})
        public JsonElement carbohydrates_total_g;
        @SerializedName(value = "fat_total_g", alternate = {"total_fat_g"})
        public JsonElement fat_total_g;
        public JsonElement serving_size_g;

        public double caloriesVal() { return asDouble(calories); }
        public double proteinVal() { return asDouble(protein_g); }
        public double carbsVal() { return asDouble(carbohydrates_total_g); }
        public double fatVal() { return asDouble(fat_total_g); }

        public boolean hasPremiumError() {
            return containsPremiumText(calories) || containsPremiumText(protein_g)
                    || containsPremiumText(carbohydrates_total_g) || containsPremiumText(fat_total_g);
        }

        private static boolean containsPremiumText(JsonElement e) {
            if (e == null || !e.isJsonPrimitive()) return false;
            try {
                if (!e.getAsJsonPrimitive().isString()) return false;
                String s = e.getAsString();
                return s != null && s.toLowerCase().contains("premium");
            } catch (Exception ex) {
                return false;
            }
        }

        private static double asDouble(JsonElement e) {
            if (e == null || !e.isJsonPrimitive()) return 0.0;
            try {
                if (e.getAsJsonPrimitive().isNumber()) return e.getAsDouble();
                if (e.getAsJsonPrimitive().isString()) {
                    String s = e.getAsString();
                    if (s == null) return 0.0;
                    return Double.parseDouble(s.trim());
                }
            } catch (Exception ignored) {}
            return 0.0;
        }
    }
}