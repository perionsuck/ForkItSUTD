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

    @SerializedName("user_handle")
    private String userHandle;

    private int age;

    @SerializedName("height_cm")
    private float heightCm;

    @SerializedName("weight_kg")
    private float weightKg;

    @SerializedName("goal_weight_kg")
    private float goalWeightKg;

    @SerializedName("activity_level")
    private int activityLevel;

    @SerializedName("is_male")
    private boolean isMale;

    public UserGoals() {
        this.dailyCalorieGoal = 2000;
        this.dailyBurnGoal = 0;
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
    public String getUserHandle() { return userHandle; }
    public int getAge() { return age; }
    public float getHeightCm() { return heightCm; }
    public float getWeightKg() { return weightKg; }
    public float getGoalWeightKg() { return goalWeightKg; }
    public int getActivityLevel() { return activityLevel; }
    public boolean isMale() { return isMale; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setDailyCalorieGoal(int dailyCalorieGoal) { this.dailyCalorieGoal = dailyCalorieGoal; }
    public void setDailyBurnGoal(int dailyBurnGoal) { this.dailyBurnGoal = dailyBurnGoal; }
    public void setProteinGoal(float proteinGoal) { this.proteinGoal = proteinGoal; }
    public void setCarbsGoal(float carbsGoal) { this.carbsGoal = carbsGoal; }
    public void setFatGoal(float fatGoal) { this.fatGoal = fatGoal; }
    public void setUserHandle(String userHandle) { this.userHandle = userHandle; }
    public void setAge(int age) { this.age = age; }
    public void setHeightCm(float heightCm) { this.heightCm = heightCm; }
    public void setWeightKg(float weightKg) { this.weightKg = weightKg; }
    public void setGoalWeightKg(float goalWeightKg) { this.goalWeightKg = goalWeightKg; }
    public void setActivityLevel(int activityLevel) { this.activityLevel = activityLevel; }
    public void setMale(boolean male) { isMale = male; }
}