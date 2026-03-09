// utils/SupabaseApi.java
package com.example.forkit.utils;

import com.example.forkit.models.FoodEntry;
import com.example.forkit.models.UserGoals;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface SupabaseApi {

    // --- FoodEntry ---
    @GET("food_entries")
    Call<List<FoodEntry>> getFoodEntries(
            @Query("order") String order,         // e.g. "timestamp.desc"
            @Query("select") String select        // e.g. "*"
    );

    @POST("food_entries")
    Call<List<FoodEntry>> insertFoodEntry(@Body FoodEntry entry);

    @DELETE("food_entries")
    Call<Void> deleteFoodEntry(@Query("id") String idFilter); // e.g. "eq.5"

    // --- UserGoals ---
    @GET("user_goals")
    Call<List<UserGoals>> getUserGoals(@Query("select") String select);

    @POST("user_goals")
    Call<List<UserGoals>> insertUserGoals(@Body UserGoals goals);

    @PATCH("user_goals")
    Call<List<UserGoals>> updateUserGoals(
            @Query("id") String idFilter,
            @Body UserGoals goals
    );
}