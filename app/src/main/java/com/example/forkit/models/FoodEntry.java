package com.example.forkit.models;

import com.google.gson.annotations.SerializedName;

public class FoodEntry {

    private int id;
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
    private int portionG;

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
        this.ingredients = ingredients;
        this.portionG = portionG;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public int getId() {
        return id;
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

    // Setters
    public void setId(int id) {
        this.id = id;
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
}