package com.example.forkit.models;

public class SavedFood {
    public String name;
    public int calories;
    public float protein;
    public float carbs;
    public float fat;

    public SavedFood() {}

    public SavedFood(String name, int calories, float protein, float carbs, float fat) {
        this.name = name;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
    }
}

