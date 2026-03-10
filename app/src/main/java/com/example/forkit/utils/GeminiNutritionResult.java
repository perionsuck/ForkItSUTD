package com.example.forkit.utils;

import com.google.gson.annotations.SerializedName;

public class GeminiNutritionResult {

    public String name;
    public int calories;
    @SerializedName("protein_g")
    public float proteinG;
    @SerializedName("carbs_g")
    public float carbsG;
    @SerializedName("fat_g")
    public float fatG;
}
