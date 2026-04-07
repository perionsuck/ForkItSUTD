package com.example.forkit.utils;

import com.google.gson.annotations.SerializedName;

public class GeminiNutritionResult {

    public String name;
    public String ingredients;

    @SerializedName(value = "calories", alternate = {"kcal", "calories_kcal", "energy_kcal"})
    public int calories;

    @SerializedName(value = "protein_g", alternate = {"protein", "proteinG", "protein_grams", "protein_g_total"})
    public float proteinG;

    @SerializedName(value = "carbs_g", alternate = {"carbs", "carbohydrates_g", "carbohydrates", "carbsG", "carbs_grams", "carbs_g_total"})
    public float carbsG;

    @SerializedName(value = "fat_g", alternate = {"fat", "fatG", "fat_grams", "fat_g_total", "lipids_g"})
    public float fatG;

    @SerializedName(value = "portion_g", alternate = {"portion", "portionGrams", "portion_grams", "serving_g", "serving_grams"})
    public int portionG;

    @SerializedName(value = "confidence", alternate = {"confidence_pct", "confidencePercent", "confidence_score"})
    public int confidence;
}
