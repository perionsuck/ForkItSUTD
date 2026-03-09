package com.example.forkit.models;

import com.google.gson.annotations.SerializedName;

public class UserGoals {

    private String id; // primary key for user_goals

    @SerializedName("user_name")
    private String userName;

    @SerializedName("daily_calorie_goal")
    private int dailyCalorieGoal;

    @SerializedName("daily_burn_goal")
    private int dailyBurnGoal;

    @SerializedName("protein_goal")
    private float proteinGoal;

    @SerializedName("carbs_goal")
    private float carbsGoal;

    @SerializedName("fat_goal")
    private float fatGoal;

    public UserGoals() {
        this.dailyCalorieGoal = 2000;
        this.dailyBurnGoal = 500;
        this.proteinGoal = 150;
        this.carbsGoal = 250;
        this.fatGoal = 65;
    }

    // Getters
    public String getId() { return id; }
    public String getUserName() { return userName; }
    public int getDailyCalorieGoal() { return dailyCalorieGoal; }
    public int getDailyBurnGoal() { return dailyBurnGoal; }
    public float getProteinGoal() { return proteinGoal; }
    public float getCarbsGoal() { return carbsGoal; }
    public float getFatGoal() { return fatGoal; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setDailyCalorieGoal(int dailyCalorieGoal) { this.dailyCalorieGoal = dailyCalorieGoal; }
    public void setDailyBurnGoal(int dailyBurnGoal) { this.dailyBurnGoal = dailyBurnGoal; }
    public void setProteinGoal(float proteinGoal) { this.proteinGoal = proteinGoal; }
    public void setCarbsGoal(float carbsGoal) { this.carbsGoal = carbsGoal; }
    public void setFatGoal(float fatGoal) { this.fatGoal = fatGoal; }
}