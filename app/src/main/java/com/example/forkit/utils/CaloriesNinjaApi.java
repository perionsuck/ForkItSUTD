package com.example.forkit.utils;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface CaloriesNinjaApi {

    @GET("v1/nutrition")
    Call<NutritionResponse> getNutrition(
            @Header("X-Api-Key") String apiKey,
            @Query("query") String query
    );

    class NutritionResponse {
        public List<NutritionItem> items;
    }

    class NutritionItem {
        public String name;
        public double calories;
        public double protein_g;
        @SerializedName(value = "carbohydrates_total_g", alternate = {"total_carbohydrates_g"})
        public double carbohydrates_total_g;
        @SerializedName(value = "fat_total_g", alternate = {"total_fat_g"})
        public double fat_total_g;
        public double serving_size_g;
    }
}