package com.example.forkit.models;

public class FoodEntry {

    private int id;
    private String foodName;
    private int calories;
    private float protein;
    private float carbs;
    private float fat;
    private String imagePath;
    private long timestamp;
    private String mealType;

    public FoodEntry(String foodName, int calories, float protein, float carbs, float fat, String mealType) {
        this.foodName = foodName;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.mealType = mealType;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public int getId() { return id; }
    public String getFoodName() { return foodName; }
    public int getCalories() { return calories; }
    public float getProtein() { return protein; }
    public float getCarbs() { return carbs; }
    public float getFat() { return fat; }
    public String getImagePath() { return imagePath; }
    public long getTimestamp() { return timestamp; }
    public String getMealType() { return mealType; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}