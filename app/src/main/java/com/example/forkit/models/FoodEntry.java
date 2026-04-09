package com.example.forkit.models;

import com.google.gson.annotations.SerializedName;

public class FoodEntry {

    private Integer id;
    @SerializedName("user_id")
    private String userId;
    @SerializedName("food_name")
    private String foodName;
    private int calories;
    private float protein;
    private float carbs;
    private float fat;
    @SerializedName("image_path")
    private String imagePath;
    private long timestamp;
    @SerializedName("meal_type")
    private String mealType;
    private String ingredients;
    @SerializedName("portion_g")
    private int portionG;

    @SerializedName("user_added")
    private boolean userAdded = true;

    public FoodEntry(String foodName, int calories, float protein, float carbs, float fat, String mealType) {
        this(foodName, calories, protein, carbs, fat, mealType, null, 0);
    }

    public FoodEntry(String foodName, int calories, float protein, float carbs, float fat, String mealType, String ingredients, int portionG) {
        this.foodName = foodName;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.mealType = mealType;
        this.ingredients = ingredients != null ? ingredients : "";
        this.portionG = portionG;
        this.timestamp = System.currentTimeMillis();
        this.userAdded = true;
    }

    public int getId() {
        return id != null ? id : 0;
    }

    public String getUserId() {
        return userId;
    }

    public String getFoodName() {
        return foodName;
    }

    public int getCalories() {
        return calories;
    }

    public float getProtein() {
        return protein;
    }

    public float getCarbs() {
        return carbs;
    }

    public float getFat() {
        return fat;
    }

    public String getImagePath() {
        return imagePath;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getMealType() {
        return mealType;
    }

    public String getIngredients() {
        return ingredients;
    }

    public int getPortionG() {
        return portionG;
    }

    public boolean isUserAdded() {
        return userAdded;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public void setPortionG(int portionG) {
        this.portionG = portionG;
    }

    public void setUserAdded(boolean userAdded) {
        this.userAdded = userAdded;
    }
}